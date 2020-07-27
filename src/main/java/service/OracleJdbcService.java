package service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public class OracleJdbcService {
    private final static Logger logger = LoggerFactory.getLogger(OracleJdbcService.class);

    private final String url;
    private final String login;
    private final String password;

    public OracleJdbcService(String url, String login, String password) {
        this.url = url;
        this.login = login;
        this.password = password;
    }

    public Long readMaxSms(Long directionId, Connection connection) throws RuntimeException{
        logger.info("Reading maxSms from database");
        var query = "select NMAXIMUM from TM_DIRECTION_COUNTERS " + getDirectionFilter(directionId);

        try {
            var resultSet = connection.prepareStatement(query).executeQuery();

            if (!resultSet.next())
                return null;
            else
                return resultSet.getLong("NMAXIMUM");
        } catch (SQLException exc) {
            logger.error("Error reading maxSms", exc);
            throw new RuntimeException(exc);
        }
    }

    public void insertMaxSms(Long directionId, Long maxSms, Connection connection) throws SQLException{
        logger.info("inserting maxSms to database: {dirId:" + directionId
                + ",maxSms:" + maxSms + "}");
        var query = buildInsertStatement(directionId, maxSms);

        try {
            connection.prepareStatement(query).executeQuery();
            connection.commit();
        } catch (SQLException exc) {
            logger.error("Error inserting maxSms", exc);
            throw exc;
        }
    }

    public void insertBatchMaxSms(Map<Long, Long> idToMaxSmsMap, Connection connection) throws SQLException{
        if (idToMaxSmsMap.isEmpty())
            return; //throw new IllegalArgumentException("Inserting batch maxSms.. idToMaxSmsMap is empty");

        logger.info(String.format("Inserting maxSms in database by batch: {ids: %s}, maxSms: {%s}",
                idToMaxSmsMap.keySet(), idToMaxSmsMap.values()));
        try {
            var statement = connection.createStatement();
            for (var entry : idToMaxSmsMap.entrySet())
                statement.addBatch(buildInsertStatement(entry.getKey(), entry.getValue()));

            statement.executeBatch();
            connection.commit();
        } catch (SQLException exc) {
            logger.error("Error inserting maxSms", exc);
            throw exc;
        }
    }

    public void updateMaxSms(Long directionId, Long maxSms, Connection connection) throws SQLException{
        logger.info("Updating maxSms in database: {dirId:" + directionId
                + ",maxSms:" + maxSms + "}");
        var query = buildUpdateStatement(directionId, maxSms);

        try {
            connection.prepareStatement(query).executeUpdate();
            connection.commit();
        } catch (SQLException exc) {
            logger.error("Error updating maxSms", exc);
            throw exc;
        }
    }

    public void updateBatchMaxSms(Map<Long, Long> idToMaxSmsMap, Connection connection) throws SQLException{
        if (idToMaxSmsMap.isEmpty())
            return; //throw new IllegalArgumentException("Updating batch maxSms.. idToMaxSmsMap is empty");
        logger.info(String.format("Updating maxSms in database by batch: {ids: %s}, maxSms: {%s}",
                idToMaxSmsMap.keySet(), idToMaxSmsMap.values()));
        try {
            var statement = connection.createStatement();
            for (var entry : idToMaxSmsMap.entrySet())
                statement.addBatch(buildUpdateStatement(entry.getKey(), entry.getValue()));

            statement.executeBatch();
            connection.commit();
        } catch (SQLException exc) {
            logger.error("Error updating maxSms", exc);
            throw exc;
        }
    }

    public void clearUselessMaxSms(Set<Long> directionIdSet, Connection connection) throws SQLException {
        if (directionIdSet.isEmpty())
            return;
        logger.info(String.format("Clearing maxSms in database: {%s}", directionIdSet));
        var query = new StringJoiner(
                ",",
                "Update TM_DIRECTION_COUNTERS set NMAXIMUM = 0 where nDirectionId in (",
                ")");

        directionIdSet.forEach(it -> query.add(String.valueOf(it)));
        var finalQuery = query.toString();

        try {
            connection.prepareStatement(finalQuery).executeUpdate();
            connection.commit();
        } catch (SQLException exc) {
            logger.error("Error clearing useless maxSms", exc);
            throw exc;
//            rollback();
        }
    }

    private String buildUpdateStatement(Long directionId, Long maxSms) {
        return "update TM_DIRECTION_COUNTERS set NMAXIMUM = "
                + maxSms + getDirectionFilter(directionId);
    }

    private String buildInsertStatement(Long directionId, Long maxSms) {
        return "insert into TM_DIRECTION_COUNTERS (nDirectionID, nCounter, nMaximum) " +
                "values (" + directionId + ", 0, " + maxSms + ")";
    }

//    private void rollback() {
//        try {
//            connection.rollback();
//        } catch (SQLException exc) {
//            logger.error("Error rollback", exc);
//        }
//    }

    private String getDirectionFilter(Long directionId) {
        return " where nDirectionId = " + directionId;
    }

    public Connection getConnection() throws SQLException{
        var connection = DriverManager.getConnection(url, login, password);
        connection.setAutoCommit(false);
        return connection;
    }
}
