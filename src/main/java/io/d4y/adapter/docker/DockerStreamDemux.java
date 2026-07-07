package io.d4y.adapter.docker;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * De-Multiplexer für den Docker-Stream (Logs/exec ohne TTY).
 *
 * <p>Format je Frame: 1 Byte Stream-Typ (0=stdin, 1=stdout, 2=stderr), 3 Null-Bytes,
 * 4 Byte Länge (Big-Endian), danach die Nutzdaten. stdout und stderr werden zusammengeführt.
 */
public final class DockerStreamDemux {

    private DockerStreamDemux() {
    }

    public static String demux(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        // Heuristik: sieht das erste Byte nicht nach einem Frame-Header aus (TTY-Stream),
        // wird der Inhalt roh als UTF-8 zurückgegeben.
        int firstType = data[0] & 0xff;
        if (firstType > 2) {
            return new String(data, StandardCharsets.UTF_8);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
        int i = 0;
        while (i + 8 <= data.length) {
            int type = data[i] & 0xff;
            if (type > 2) {
                // Kein gültiger Header mehr — Rest roh anhängen.
                out.write(data, i, data.length - i);
                break;
            }
            long len = ((long) (data[i + 4] & 0xff) << 24)
                    | ((data[i + 5] & 0xff) << 16)
                    | ((data[i + 6] & 0xff) << 8)
                    | (data[i + 7] & 0xff);
            int start = i + 8;
            int available = data.length - start;
            int take = (int) Math.min(len, available);
            if (take > 0) {
                out.write(data, start, take);
            }
            i = start + take;
        }
        return out.toString(StandardCharsets.UTF_8);
    }
}
