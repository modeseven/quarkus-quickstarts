package com.example.queryapi.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.provider.Arguments;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Scans src/test/resources/fixtures/ for JSON fixture files.
 * Returns Stream<Arguments> for JUnit @MethodSource.
 */
public class FixtureLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Stream<Arguments> loadFixtures() throws IOException, URISyntaxException {
        Path fixturesDir = Paths.get(
                FixtureLoader.class.getClassLoader().getResource("fixtures").toURI());
        return Files.list(fixturesDir)
                .filter(p -> p.toString().endsWith(".json"))
                .sorted()
                .map(FixtureLoader::readFixture)
                .map(f -> Arguments.of(f.name, f));
    }

    private static Fixture readFixture(Path path) {
        try {
            return MAPPER.readValue(path.toFile(), Fixture.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read fixture: " + path, e);
        }
    }
}
