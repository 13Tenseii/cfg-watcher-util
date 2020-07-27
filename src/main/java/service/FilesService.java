package service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.model.DirectionMaxSms;
import service.parser.BracketSearcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class FilesService {
    private final static Logger logger = LoggerFactory.getLogger(FilesService.class);

    private static final Pattern ID_PROPERTY = Pattern.compile("(?<!\\w)id\\s*=\\s*(\\d+)", Pattern.MULTILINE);
    private static final Pattern ACTIVE_BILLING_PROPERTY = Pattern.compile("(?<!\\w)billing\\s*=\\s*1", Pattern.MULTILINE);
    private static final Pattern MAX_SMS_PROPERTY = Pattern.compile("(?<!\\w)max_sms\\s*=\\s*(\\d+)", Pattern.MULTILINE);
    private static final Pattern COMMENTS = Pattern.compile("#.*$", Pattern.MULTILINE);
    private final String configPath;

    public FilesService(String configPath) {
        this.configPath = configPath;
    }

    public Map<Long, Optional<DirectionMaxSms>> getIdToMaxSmsMap() throws IOException {
        String config = removeComments(readConfig());
        BracketSearcher searcher = new BracketSearcher();

        var directionIdToMaxSms = new HashMap<Long, Optional<DirectionMaxSms>>();
        var configLength = config.length();
        for(int from = config.indexOf("{"); from < configLength;) {
            int till = searcher.findCurvyClosingBracketPos(config, from);
            if (till == -1)
                break;
            var direction = config.substring(from, till);

            var optional = parseDirection(direction);
            if(optional.isPresent()) {
                var entry = optional.get();
                directionIdToMaxSms.put(entry.getKey(), entry.getValue());
            }
            from = till + 1;
        }
        return directionIdToMaxSms;
    }

    private String readConfig() throws IOException {
        Path filePath = Paths.get(configPath);
        return Files.readString(filePath);
    }

    private String removeComments(String config) {
        return COMMENTS.matcher(config).replaceAll("");
    }

    /**
     * @return entry of direction Id to max sms
     */
    private Optional<Map.Entry<Long, Optional<DirectionMaxSms>>> parseDirection(String direction){
        var idMatcher = ID_PROPERTY.matcher(direction);
        if(idMatcher.find()) {
            var directionId = Long.parseLong(idMatcher.group(1));
            logger.debug("Parse direction id: "+ directionId);

            var maxSmsMatcher = MAX_SMS_PROPERTY.matcher(direction);
            var isBillingActive = ACTIVE_BILLING_PROPERTY.matcher(direction).find();

            if (maxSmsMatcher.find()) {
                var maxSms = Long.parseLong(maxSmsMatcher.group(1));
                logger.debug("Direction max_sms: "+ maxSms);
                if (isBillingActive)
                    return Optional.of(Map.entry(directionId,
                            Optional.of(new DirectionMaxSms(directionId, maxSms, true))));
                else
                    return Optional.of(Map.entry(directionId,
                            Optional.of(new DirectionMaxSms(directionId, maxSms, false))));
            } else {
                if (isBillingActive)
                    throw new IllegalArgumentException("Direction max_sms property is not found while billing = 1");
                else
                    return Optional.of(Map.entry(directionId,
                            Optional.of(new DirectionMaxSms(directionId, null, false))));
            }
        }else {
            logger.warn("Direction id is not found!");
        }
        return Optional.empty();
    }
}
