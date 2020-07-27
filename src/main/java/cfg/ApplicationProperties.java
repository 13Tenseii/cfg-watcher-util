package cfg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class ApplicationProperties {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationProperties.class);

    private static Map<String, Object> fieldToValue;
    private static ApplicationProperties properties = new ApplicationProperties();
    ;

    public static Property getProp(String propName) {
        if (fieldToValue == null)
            properties.readYaml();

        return new Property(fieldToValue.get(propName));
    }

    private void readYaml() {
        if (fieldToValue == null) {
            // for prod(props outside jar)
            var jarPath = getJarPath();

            try (var inputStream = new FileInputStream(Path.of(
                    Path.of(jarPath).toFile().getParent(),
                    "application.yaml").toString())) {
                fieldToValue = new Yaml().load(inputStream);
                return;
            } catch (IOException exc) {}

            // for develop(props inside jar)
            var inputStream = this.getClass().getClassLoader().getResourceAsStream("application.yaml");
            fieldToValue = new Yaml().load(inputStream);
        }
    }

    private String getJarPath() {
        return ApplicationProperties.class
                .getProtectionDomain().getCodeSource().getLocation().getPath();
    }
}
