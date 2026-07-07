package io.d4y.adapter.docker;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DockerStreamDemuxTest {

    private static byte[] frame(int type, String payload) {
        byte[] p = payload.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(type);
        out.write(0);
        out.write(0);
        out.write(0);
        out.write((p.length >>> 24) & 0xff);
        out.write((p.length >>> 16) & 0xff);
        out.write((p.length >>> 8) & 0xff);
        out.write(p.length & 0xff);
        out.write(p, 0, p.length);
        return out.toByteArray();
    }

    @Test
    void demuxesStdoutAndStderrFrames() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] a = frame(1, "hallo\n");
        byte[] b = frame(2, "fehler");
        buf.write(a, 0, a.length);
        buf.write(b, 0, b.length);

        assertThat(DockerStreamDemux.demux(buf.toByteArray())).isEqualTo("hallo\nfehler");
    }

    @Test
    void treatsNonFramedAsRaw() {
        byte[] raw = "plain text".getBytes(StandardCharsets.UTF_8);
        assertThat(DockerStreamDemux.demux(raw)).isEqualTo("plain text");
    }

    @Test
    void emptyInput() {
        assertThat(DockerStreamDemux.demux(new byte[0])).isEmpty();
    }
}
