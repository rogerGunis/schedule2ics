package de.gunis.roger.workersAvailable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IllegalJobException extends Throwable {
    private static final Logger logger = LoggerFactory.getLogger("IllegalJobException.class");

    public IllegalJobException(String s) {
        logger.error(s);
    }
}
