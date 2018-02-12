package de.gunis.roger.jobService;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.JCommander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ClearingHouse {
    private static final Logger logger = LoggerFactory.getLogger("ClearingHouse.class");

    private static final Map<Level, LoggingFunction> map;
    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    static {
        map = new HashMap<>();
        map.put(Level.TRACE, logger::trace);
        map.put(Level.DEBUG, logger::debug);
        map.put(Level.INFO, logger::info);
        map.put(Level.WARN, logger::warn);
        map.put(Level.ERROR, logger::error);
    }

    static void log(String s) {
        ch.qos.logback.classic.Logger rootLogger = getRootLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        Level effectiveLevel = rootLogger.getEffectiveLevel();
        try {
            map.get(effectiveLevel).log(s);

        } catch (Exception e) {
            logger.error(s);
        }
    }

    static void setLoggingLevel(String level) {
        ch.qos.logback.classic.Logger root = getRootLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.toLevel(level, Level.DEBUG));
        ClearingHouse.log("Setting loglevel to " + root.getEffectiveLevel());

        // we suppress calendar TRACE level, because not needed for me
        ch.qos.logback.classic.Logger cal = getRootLogger("net.fortuna.ical4j.data.FoldingWriter");
        cal.setLevel(Level.DEBUG);
    }

    private static ch.qos.logback.classic.Logger getRootLogger(String rootLoggerName) {
        return (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
                rootLoggerName
        );
    }

    public static boolean askedForInstructions(JCommander jCommander, boolean help) {
        if (help) {
            jCommander.usage();
            logger.info("-------------\nAll elements marked with * are required\n-------------\n");
            return true;
        } else {
            return false;
        }
    }

    @FunctionalInterface
    private interface LoggingFunction {
        public void log(String arg);
    }
}
