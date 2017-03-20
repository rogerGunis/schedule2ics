package de.gunis.roger;

import ch.qos.logback.classic.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ClearingHouse {
    private static final Logger logger = LoggerFactory.getLogger("ClearingHouse.class");

    private static final Map<Level, LoggingFunction> map;

    static {
        map = new HashMap<>();
        map.put(Level.TRACE, (o) -> logger.trace(o));
        map.put(Level.DEBUG, (o) -> logger.debug(o));
        map.put(Level.INFO, (o) -> logger.info(o));
        map.put(Level.WARN, (o) -> logger.warn(o));
        map.put(Level.ERROR, (o) -> logger.error(o));
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

    @FunctionalInterface
    private interface LoggingFunction {
        public void log(String arg);
    }
}
