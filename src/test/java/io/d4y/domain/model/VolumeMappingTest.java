package io.d4y.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VolumeMappingTest {

    @Test
    void rejectsRelativePath() {
        assertThatThrownBy(() -> new VolumeMapping("data", "relative/path"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidName() {
        assertThatThrownBy(() -> new VolumeMapping("bad name", "/data"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void encodeIsDeterministicAndSortedByName() {
        String encoded = VolumeMapping.encode(List.of(
                new VolumeMapping("html", "/usr/share/nginx/html"),
                new VolumeMapping("cache", "/var/cache/nginx")));

        assertThat(encoded).isEqualTo("cache=/var/cache/nginx;html=/usr/share/nginx/html");
    }

    @Test
    void encodeDecodeRoundTrip() {
        List<VolumeMapping> volumes = List.of(
                new VolumeMapping("cache", "/var/cache/nginx"),
                new VolumeMapping("html", "/usr/share/nginx/html"));

        assertThat(VolumeMapping.decode(VolumeMapping.encode(volumes)))
                .containsExactlyInAnyOrderElementsOf(volumes);
    }

    @Test
    void decodeOfBlankYieldsEmptyList() {
        assertThat(VolumeMapping.decode("")).isEmpty();
        assertThat(VolumeMapping.decode(null)).isEmpty();
    }
}
