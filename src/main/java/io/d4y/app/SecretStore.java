package io.d4y.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.d4y.config.D4yProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * In-Memory-Store gelieferter Image/Container-Secrets (ADR-0024), optional AES-GCM-verschlüsselt auf
 * Platte persistiert. Secrets werden per authentifiziertem Push geliefert (ADR-0023); der
 * Verschlüsselungs-Schlüssel ist ein host/d4y-Credential aus der Instanz-Konfiguration.
 *
 * <p>Der verschlüsselte Datei-Cache überlebt Neustarts (Self-Healing braucht die Secrets jederzeit)
 * und ist aus GitHub per Push re-derivierbar — keine autoritative Wahrheit. Secret-Werte werden
 * <b>niemals</b> geloggt ([privacy-rules PR-8]).
 */
@Component
public class SecretStore {

    private static final Logger log = LoggerFactory.getLogger(SecretStore.class);
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{secret:([^}]+)}");
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final ObjectMapper json = new ObjectMapper();
    private final SecureRandom random = new SecureRandom();
    private final Map<String, String> secrets = new ConcurrentHashMap<>();
    private final Path file;
    private final SecretKeySpec key; // null ⇒ keine Persistenz (nur RAM)

    public SecretStore(D4yProperties properties) {
        D4yProperties.Secrets cfg = properties.secrets();
        this.file = Path.of(cfg.file());
        this.key = cfg.persistent() ? deriveKey(cfg.encryptionKey()) : null;
    }

    /** Lädt den verschlüsselten Store beim Start, sofern vorhanden und ein Schlüssel gesetzt ist. */
    @EventListener(ApplicationReadyEvent.class)
    public synchronized void load() {
        if (key == null || !Files.isRegularFile(file)) {
            return;
        }
        try {
            byte[] plain = decrypt(Files.readAllBytes(file));
            Map<String, String> loaded = json.readValue(plain, new TypeReference<Map<String, String>>() {
            });
            secrets.clear();
            secrets.putAll(loaded);
            log.info("Secret-Store geladen ({} Einträge)", secrets.size());
        } catch (Exception e) {
            // Kein Secret-Wert im Log — nur die Ursache.
            log.warn("Secret-Store konnte nicht geladen werden: {}", e.getMessage());
        }
    }

    /** Ersetzt den gesamten Secret-Satz (idempotenter Replace) und persistiert verschlüsselt. */
    public synchronized void replaceAll(Map<String, String> delivered) {
        secrets.clear();
        if (delivered != null) {
            delivered.forEach((k, v) -> {
                if (k != null && v != null) {
                    secrets.put(k, v);
                }
            });
        }
        persist();
        log.info("Secret-Store aktualisiert ({} Einträge)", secrets.size());
    }

    public Optional<String> get(String name) {
        return Optional.ofNullable(secrets.get(name));
    }

    public int size() {
        return secrets.size();
    }

    /**
     * Ersetzt {@code ${secret:NAME}}-Platzhalter im Wert durch die gelieferten Secrets.
     * Ein unbekannter Name führt zu {@link UnresolvedSecretException}.
     */
    public String resolve(String value) {
        if (value == null || !value.contains("${secret:")) {
            return value;
        }
        Matcher m = PLACEHOLDER.matcher(value);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            String replacement = secrets.get(name);
            if (replacement == null) {
                throw new UnresolvedSecretException(name);
            }
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    private void persist() {
        if (key == null) {
            return; // nur RAM
        }
        try {
            byte[] plain = json.writeValueAsBytes(new LinkedHashMap<>(secrets));
            byte[] enc = encrypt(plain);
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(tmp, enc);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Secret-Store konnte nicht geschrieben werden", e);
        }
    }

    private byte[] encrypt(byte[] plain) {
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            random.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = c.doFinal(plain);
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Verschlüsselung fehlgeschlagen", e);
        }
    }

    private byte[] decrypt(byte[] data) {
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            System.arraycopy(data, 0, iv, 0, GCM_IV_BYTES);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return c.doFinal(data, GCM_IV_BYTES, data.length - GCM_IV_BYTES);
        } catch (Exception e) {
            throw new IllegalStateException("Entschlüsselung fehlgeschlagen", e);
        }
    }

    private static SecretKeySpec deriveKey(String secret) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(hash, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Schlüssel-Ableitung fehlgeschlagen", e);
        }
    }
}
