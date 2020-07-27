import cfg.ApplicationProperties;
import service.watcher.FileAdapter;
import service.watcher.FileWatcher;

import java.sql.DriverManager;

class Application {

    void start() throws Exception {
        DriverManager.registerDriver(new oracle.jdbc.OracleDriver());

        var fileWatcher = new FileWatcher(
                ApplicationProperties.getProp("config-path").asText(),
                ApplicationProperties.getProp("interval-millis").asLong(),
                new FileAdapter()
        );
        fileWatcher.watch();
    }

}
