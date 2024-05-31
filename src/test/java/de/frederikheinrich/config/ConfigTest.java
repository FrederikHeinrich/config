package de.frederikheinrich.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigTest {

    private static final Path path = Path.of("ConfigTest.json");
    private ByteArrayInputStream testInput;

    @BeforeEach
    public void setUp() {
        // Simulate console input for the test
        testInput = new ByteArrayInputStream("testValue1\n123\n132\n".getBytes());
        System.setIn(testInput);
    }

    @AfterEach
    public void tearDown() {
        System.setIn(System.in);  // Restore original System.in
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testConfigWithUserInput() throws IOException, IllegalAccessException {
        Files.deleteIfExists(path);

        TestObject obj = new TestObject();
        new Config(obj, path);

        assertEquals("testValue1", obj.field1);
        assertEquals(Integer.valueOf(123), obj.field2);

        // Check if values were saved correctly
        String configContent = Files.readString(path);
        System.out.println("Config: " + configContent);
        assert configContent.contains("\"key1\": \"testValue1\"");
        assert configContent.contains("\"key2\": \"123\"");
    }

    @Test
    public void testConfigWithExistingFile() throws IOException, IllegalAccessException {
        // Create a pre-existing configuration file
        String existingConfig = "{\n" +
                "  \"key1\": \"existingValue1\",\n" +
                "  \"key2\": \"456\"\n" +
                "}";
        Files.writeString(path, existingConfig);

        TestObject obj = new TestObject();
        new Config(obj, path);

        // Verify that the values from the existing config file are applied
        assertEquals("existingValue1", obj.field1);
        assertEquals(Integer.valueOf(456), obj.field2);

        // Check if values were preserved correctly
        String configContent = Files.readString(path);
        System.out.println("Config: " + configContent);
        assert configContent.contains("\"key1\": \"existingValue1\"");
        assert configContent.contains("\"key2\": \"456\"");
    }

    private static class TestObject {
        @Config.Key(key = "key1", description = "desc1", prompt = "prompt1", env = "env1", regex = ".*")
        public String field1;

        @Config.Key(key = "key2", description = "desc2", prompt = "prompt2", env = "env2", regex = "\\d+")
        public Integer field2;
        
        @Config.Key(key = "key-default", description = "descD", prompt = "promptD", env = "envD", regex = ".*")
        public String fieldD = "default";
    }
}
