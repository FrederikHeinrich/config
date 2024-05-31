package de.frederikheinrich.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The Config class provides a way to load and save configurations for an object.
 * Configuration values can be read from a local JSON file or from environment variables.
 * If a configuration value is not found, the user will be prompted to input the value.
 */
public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);
    private final Path path;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Scanner scanner;
    private final Object object;
    private JsonObject localConfig;

    /**
     * Initializes a Config object with the given object and default configuration file path.
     * If the configuration file does not exist, a new empty JsonObject is created and saved to the file.
     * If the configuration file exists, its contents are read and parsed into a JsonObject.
     * Fields annotated with @Key are processed based on their configuration in the JsonObject or user input if necessary.
     *
     * @param object the object for which the configuration is being loaded
     */
    public Config(Object object) {
        this(object, Path.of(object.getClass().getSimpleName() + ".json"));
    }

    /**
     * Initializes a Config object with the given object and configuration file path.
     * If the configuration file does not exist, a new empty JsonObject is created and saved to the file.
     * If the configuration file exists, its contents are read and parsed into a JsonObject.
     * Fields annotated with @Key are processed based on their configuration in the JsonObject or user input if necessary.
     *
     * @param object the object for which the configuration is being loaded
     * @param path   the configuration file path
     */
    public Config(Object object, Path path) {
        this.object = object;
        this.path = path;
        if (!Files.exists(path)) {
            log.debug("Config file does not exist: {}", path.toAbsolutePath());
            localConfig = new JsonObject();
        }
        try {
            String config = Files.readString(path, StandardCharsets.UTF_8);
            localConfig = gson.fromJson(config, JsonObject.class);
        } catch (IOException e) {
            log.warn("Failed to load config", e);
            localConfig = new JsonObject();
        }

        log.debug("Successfully loaded config: {}", localConfig);
        scanner = new Scanner(System.in);
        Arrays.stream(getAllFields()).forEach(field -> {
            try {
                processLocalField(object, field);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        scanner.close();
    }

    private Field[] getAllFields() {
        List<Field> fields = new ArrayList<>();
        Class<?> clazz = object.getClass();
        while (clazz != null) {
            fields.addAll(Arrays.stream(clazz.getDeclaredFields()).filter(field -> field.isAnnotationPresent(Key.class)).collect(Collectors.toList()));
            clazz = clazz.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }

    private void processLocalField(Object object, Field field) throws IllegalAccessException {
        Key lc = field.getAnnotation(Key.class);
        String envValue = System.getenv(lc.env());
        log.debug("Processing field {} with value {}", field.getName(), envValue);
        if (envValue != null && !envValue.isEmpty()) {
            setFieldValue(field, object, envValue);
            localConfig.addProperty(lc.key(), envValue);
        } else if (localConfig.has(lc.key())) {
            log.debug("Setting Field {} with value {}", field.getName(), localConfig.get(lc.key()));
            setFieldValue(field, object, localConfig.get(lc.key()).getAsString());
        } else {
            log.debug("Field {} not found", field.getName());
            System.out.println(lc.prompt());
            Pattern pattern = Pattern.compile(lc.regex());

            field.setAccessible(true);
            while (field.get(object) == null) {
                try {
                    String input = scanner.nextLine();
                    Matcher matcher = pattern.matcher(input);
                    if (matcher.matches()) {
                        setFieldValue(field, object, input);
                        localConfig.addProperty(lc.key(), input);
                    } else {
                        System.out.println("Your input is not valid");
                    }
                } catch (NoSuchElementException e) {
                    System.out.println("Your input is not valid");
                }
            }
            save();
        }
    }

    private void setFieldValue(Field field, Object object, String value) throws IllegalAccessException {
        field.setAccessible(true);
        if (field.getType().equals(String.class)) {
            field.set(object, value);
        } else {
            field.set(object, gson.fromJson(value, field.getType()));
        }
    }

    private void save() {
        for (Field field : getAllFields()) {
            Key key = field.getAnnotation(Key.class);
            try {
                if (field.get(object) != null)
                    if (field.getType().equals(String.class))
                        localConfig.addProperty(key.key(), field.get(object).toString());
                    else
                        localConfig.addProperty(key.key(), gson.toJson(field.get(object)));
            } catch (IllegalAccessException e) {
                log.error("Unable to read field {}", field.getName(), e);
            }
        }
        try {
            Files.writeString(path, gson.toJson(localConfig), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error saving config", e);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Key {
        String key();

        String description();

        String prompt();

        String env();

        String regex();
    }
}
