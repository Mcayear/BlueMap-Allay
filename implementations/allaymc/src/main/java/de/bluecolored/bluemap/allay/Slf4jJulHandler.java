package de.bluecolored.bluemap.allay;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class Slf4jJulHandler extends Handler {

    private final org.slf4j.Logger slf4jLogger;

    public Slf4jJulHandler(org.slf4j.Logger slf4jLogger) {
        this.slf4jLogger = slf4jLogger;
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }

        String message = record.getMessage();
        Throwable throwable = record.getThrown();
        Level level = record.getLevel();

        if (level.intValue() >= Level.SEVERE.intValue()) {
            if (throwable != null) {
                slf4jLogger.error(message, throwable);
            } else {
                slf4jLogger.error(message);
            }
        } else if (level.intValue() >= Level.WARNING.intValue()) {
            if (throwable != null) {
                slf4jLogger.warn(message, throwable);
            } else {
                slf4jLogger.warn(message);
            }
        } else if (level.intValue() >= Level.INFO.intValue()) { // Includes INFO and CONFIG
            // SLF4J doesn't have a CONFIG level, mapping to INFO
            if (throwable != null) {
                slf4jLogger.info(message, throwable);
            } else {
                slf4jLogger.info(message);
            }
        } else if (level.intValue() >= Level.FINE.intValue()) { // Includes FINE
            if (throwable != null) {
                slf4jLogger.debug(message, throwable);
            } else {
                slf4jLogger.debug(message);
            }
        } else { // Includes FINER, FINEST - mapping to TRACE or DEBUG
            // If your SLF4J backend doesn't handle TRACE well or you prefer DEBUG:
            if (throwable != null) {
                slf4jLogger.trace(message, throwable); // or slf4jLogger.debug(...)
            } else {
                slf4jLogger.trace(message); // or slf4jLogger.debug(...)
            }
        }
    }

    @Override
    public void flush() {
        // SLF4J typically doesn't require manual flushing for console/file appenders
    }

    @Override
    public void close() throws SecurityException {
        // SLF4J loggers are managed by the logging framework
    }
}