/*
 * Copyright 2011 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.slf4j.impl;

import static org.apache.log4j.Level.DEBUG;
import static org.apache.log4j.Level.ERROR;
import static org.apache.log4j.Level.INFO;
import static org.apache.log4j.Level.TRACE;
import static org.apache.log4j.Level.WARN;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.exceptions.DefaultStackTraceFilterer;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

/**
 * A Log4j adapter that produces cleaner, more informative stack traces,
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class GrailsLog4jLoggerAdapter extends MarkerIgnoringBase {

    private static final long serialVersionUID = 1;

    static final String FQCN = GrailsLog4jLoggerAdapter.class.getName();

    private final Logger log4jLogger;

    public GrailsLog4jLoggerAdapter(org.apache.log4j.Logger logger) {
        log4jLogger = logger;
        name = logger.getName();
    }

    public boolean isTraceEnabled() {
        return log4jLogger.isTraceEnabled();
    }

    public void trace(String msg) {
        logMessage(TRACE, msg, null);
    }

    public void trace(String format, Object arg) {
        logMessageFormat(TRACE, format, arg);
    }

    public void trace(String format, Object arg1, Object arg2) {
        logMessageFormat(TRACE, format, arg1, arg2);
    }

    public void trace(String format, Object... argArray) {
        logMessageFormat(TRACE, format, argArray);
    }

    public void trace(String msg, Throwable t) {
        logMessage(TRACE, msg, t);
    }

    public boolean isDebugEnabled() {
        return log4jLogger.isDebugEnabled();
    }

    public void debug(String msg) {
        logMessage(DEBUG, msg, null);
    }

    public void debug(String format, Object arg) {
        logMessageFormat(DEBUG, format, arg);
    }

    public void debug(String format, Object arg1, Object arg2) {
        logMessageFormat(DEBUG, format, arg1, arg2);
    }

    public void debug(String format, Object... argArray) {
        logMessageFormat(DEBUG, format, argArray);
    }

    public void debug(String msg, Throwable t) {
        logMessage(DEBUG, msg, t);
    }

    public boolean isInfoEnabled() {
        return log4jLogger.isInfoEnabled();
    }

    public void info(String msg) {
        logMessage(INFO, msg, null);
    }

    public void info(String format, Object arg) {
        logMessageFormat(INFO, format, arg);
    }

    public void info(String format, Object arg1, Object arg2) {
        logMessageFormat(INFO, format, arg1, arg2);
    }

    public void info(String format, Object... argArray) {
        logMessageFormat(INFO, format, argArray);
    }

    public void info(String msg, Throwable t) {
        logMessage(INFO, msg, t);
    }

    public boolean isWarnEnabled() {
        return log4jLogger.isEnabledFor(WARN);
    }

    public void warn(String msg) {
        logMessage(WARN, msg, null);
    }

    public void warn(String format, Object arg) {
        logMessageFormat(WARN, format, arg);
    }

    public void warn(String format, Object... argArray) {
        logMessageFormat(WARN, format, argArray);
    }

    public void warn(String format, Object arg1, Object arg2) {
        logMessageFormat(WARN, format, arg1, arg2);
    }

    public void warn(String msg, Throwable t) {
        logMessage(WARN, msg,t);
    }

    public boolean isErrorEnabled() {
        return log4jLogger.isEnabledFor(ERROR);
    }

    public void error(String msg) {
        logMessage(ERROR, msg, null);
    }

    public void error(String format, Object arg) {
        logMessageFormat(ERROR, format, arg);
    }

    public void error(String format, Object arg1, Object arg2) {
        logMessageFormat(ERROR, format, arg1, arg2);
    }

    public void error(String format, Object... argArray) {
        logMessageFormat(ERROR, format, argArray);
    }

    public void error(String msg, Throwable t) {
        logMessage(ERROR, msg, t);
    }

    private final FormattingTuple getMessageFormat(final String format, final Object... args) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
        cleanIfException(ft.getThrowable());
        return ft;
    }

    private final Throwable cleanIfException(final Throwable t) {
        if (t != null) {
            new DefaultStackTraceFilterer().filter(t, true);
        }
        return t;
    }

    private final void logMessageFormat(final Level level, final String format, final Object... args) {
        if (log4jLogger.isEnabledFor(level)) {
            FormattingTuple ft = getMessageFormat(format, args);
            log4jLogger.log(FQCN, level, ft.getMessage(), ft.getThrowable());
        }
    }

    private final void logMessage(final Level level, final String msg, final Throwable t) {
        Throwable filteredTrace = (t != null && log4jLogger.isEnabledFor(level) && !DefaultStackTraceFilterer.STACK_LOG_NAME.equals(name)) ? cleanIfException(t) : t;
        log4jLogger.log(FQCN, level, msg, filteredTrace);
    }
}
