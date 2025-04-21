package com.tmkfolha.util;

import org.apache.logging.log4j.Logger;

public class LoggerUtil {
    public static void debug(Logger logger, String message, Object... params) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format(message.replace("{}", "%s"), params));
        }
    }

    public static void info(Logger logger, String message, Object param) {
        if (logger.isInfoEnabled()) {
            logger.info(message + " " + param.toString());
        }
    }

    public static void warn(Logger logger, String message, Object param) {
        if (logger.isWarnEnabled()) {
            logger.warn(message + " " + param);
        }
    }

}
