package io.protop.core.logs;

import org.slf4j.LoggerFactory;

public class Logger implements ILogger {

    private final org.slf4j.Logger logger;

    private Logger(org.slf4j.Logger logger) {
        this.logger = logger;
    }

    public static final Logger getLogger(Class<?> cls) {
        return new Logger(LoggerFactory.getLogger(cls));
    }

    @Override
    public void always(String message) {
        System.out.println(message);
    }

    @Override
    public void debug(String message) {
        if (Logs.areEnabled()) {
            logger.debug(message);
        }
    }

    @Override
    public void debug(String format, Object... args) {
        if (Logs.areEnabled()) {
            logger.debug(format, args);
        }
    }

    @Override
    public void info(String message) {
        if (Logs.areEnabled()) {
            logger.info(message);
        }
    }

    @Override
    public void info(String format, Object... args) {
        if (Logs.areEnabled()) {
            logger.info(format, args);
        }
    }

    @Override
    public void warn(String message) {
        if (Logs.areEnabled()) {
            logger.warn(message);
        }
    }

    @Override
    public void warn(String format, Object... args) {
        if (Logs.areEnabled()) {
            logger.warn(format, args);
        }
    }

    @Override
    public void error(String message) {
        if (Logs.areEnabled()) {
            logger.error(message);
        }
    }

    @Override
    public void error(String format, Object... args) {
        if (Logs.areEnabled()) {
            logger.error(format, args);
        }
    }

    @Override
    public void error(String message, Throwable cause) {
        if (Logs.areEnabled()) {
            logger.error(message, cause);
        }
    }
}
