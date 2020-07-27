package service.watcher;

import cfg.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.FilesService;
import service.OracleJdbcService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;

public class FileAdapter implements FileListener {
    private final static Logger logger = LoggerFactory.getLogger(FileAdapter.class);

    private OracleJdbcService oracleJdbcService;
    private FilesService filesService;

    public FileAdapter() throws Exception {
        initialize();
    }

    @Override
    public void onChange(FileEvent fileEvent) {
        logger.info("File was changed...processing");
        try {
            var idToMaxSmsMap = filesService.getIdToMaxSmsMap();
            logger.info(String.format("Processing with directionId: {%s}", idToMaxSmsMap.keySet()));

            var directionsForUpdateMap = new HashMap<Long, Long>();
            var directionForInsertMap = new HashMap<Long, Long>();
            var directionForClearSet = new HashSet<Long>();

            try (Connection connection = oracleJdbcService.getConnection()){

                idToMaxSmsMap.forEach((directionId, directionMaxSms) -> {
                    if(directionMaxSms.isEmpty())
                        return;
                    var info = directionMaxSms.get();
                    var isDbMaxSmsExist = oracleJdbcService.readMaxSms(directionId, connection) != null;
                    if (info.isBillingActive()) {
                        if (isDbMaxSmsExist)
                            directionsForUpdateMap.put(directionId, info.getMaxSms());
                        else
                            directionForInsertMap.put(directionId, info.getMaxSms());
                    } else {
                        if (isDbMaxSmsExist && info.getMaxSms() == null)
                            directionForClearSet.add(directionId);

                    }
                });

                oracleJdbcService.updateBatchMaxSms(directionsForUpdateMap, connection);
                oracleJdbcService.insertBatchMaxSms(directionForInsertMap, connection);
                oracleJdbcService.clearUselessMaxSms(directionForClearSet, connection);

                connection.commit();
            } catch (SQLException exc) {
                throw new RuntimeException(exc);
            }

        } catch (Exception exc) {
            logger.error("Error updating maxSms", exc);
        }
    }

    private void initialize() throws Exception {
        try {
            oracleJdbcService = new OracleJdbcService(
                    ApplicationProperties.getProp("jdbc.url").asText(),
                    ApplicationProperties.getProp("jdbc.login").asText(),
                    ApplicationProperties.getProp("jdbc.password").asText()
            );
            filesService = new FilesService(
                    ApplicationProperties.getProp("config-path").asText()
            );
        } catch (Exception exc) {
            logger.error("Error initializing services", exc);
            throw new Exception(exc.getMessage());
        }
    }
}
