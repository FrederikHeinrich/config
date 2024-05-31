# config

[![](https://jitpack.io/v/FrederikHeinrich/config.svg)](https://jitpack.io/#FrederikHeinrich/config)

The Config Project is a Java library for managing configuration settings from environment variables, a local JSON file, and user input.

## Usage

### Installation

Maven:

```xml

<dependency>
    <groupId>com.github.FrederikHeinrich</groupId>
    <artifactId>config</artifactId>
    <version>Tag</version>
</dependency>
```

Gradle:

```groovy
implementation 'com.github.FrederikHeinrich:config:Tag'
```

Replace `Tag` with the version tag of the release you want to use.

### Examples

Create a configuration class:

```java
public class AppConfig {
    @Config.Key(key = "apiUrl", description = "API URL", prompt = "Enter API URL:", env = "API_URL", regex = "https?://.*")
    public String apiUrl;

    @Config.Key(key = "timeout", description = "Timeout in seconds", prompt = "Enter timeout:", env = "TIMEOUT", regex = "\\d+")
    public Integer timeout;
}
```

Initialize and use the Config class:

```java
public class Main {
    public static void main(String[] args) {
        AppConfig config = new AppConfig();
        new Config(config);
        System.out.println("API URL: " + config.apiUrl);
        System.out.println("Timeout: " + config.timeout);
    }
}
```

## Contributing

If you find a bug or have an enhancement suggestion, please create an issue or pull request on GitHub.
