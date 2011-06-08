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

import grails.util.GrailsUtil;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

import static org.apache.log4j.Level.*;

/**
 * A Log4j adapter that produces cleaner, more informative stack traces
 *
 * @author Graeme Rocher
 * @since 1.4
 */
public class GrailsLog4jLoggerAdapter extends MarkerIgnoringBase implements org.slf4j.Logger {

    final static String FQCN = GrailsLog4jLoggerAdapter.class.getName();

    private Logger log4jLogger;
    private String name;

    public GrailsLog4jLoggerAdapter(org.apache.log4j.Logger logger) {
        this.log4jLogger = logger;
        this.name = logger.getName();
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean isTraceEnabled() {
        return log4jLogger.isTraceEnabled();
    }

    public void trace(String msg) {
        logMessage(TRACE, msg, null);
    }

    public void trace(String format, Object arg) {
        logMessageFormat(TRACE, getMessageFormat(format, arg));
    }

    public void trace(String format, Object arg1, Object arg2) {
        logMessageFormat(TRACE, getMessageFormat(format, arg1, arg2));
    }

    public void trace(String format, Object[] argArray) {
        logMessageFormat(TRACE, getMessageFormat(format, argArray));
    }

    public void trace(String msg, Throwable t) {
        logMessage(TRACE, msg, t);
    }

    public boolean isDebugEnabled() {
        return log4jLogger.isDebugEnabled();
    }

    public void debug(String msg) {
        logMessage(DEBUG, msg);
    }

    public void debug(String format, Object arg) {
        logMessageFormat(DEBUG, getMessageFormat(format, arg));
    }

    public void debug(String format, Object arg1, Object arg2) {
        logMessageFormat(DEBUG, getMessageFormat(format, arg1, arg2));
    }

    public void debug(String format, Object[] argArray) {
        logMessageFormat(DEBUG, getMessageFormat(format, argArray));
    }

    public void debug(String msg, Throwable t) {
        logMessage(DEBUG, msg, t);
    }


    public boolean isInfoEnabled() {
        return log4jLogger.isInfoEnabled();
    }

    public void info(String msg) {
        logMessage(INFO, msg);
    }

    public void info(String format, Object arg) {
        logMessageFormat(INFO, getMessageFormat(format, arg));
    }

    public void info(String format, Object arg1, Object arg2) {
        logMessageFormat(INFO, getMessageFormat(format, arg1, arg2));
    }

    public void info(String format, Object[] argArray) {
        logMessageFormat(INFO, getMessageFormat(format, argArray));
    }

    public void info(String msg, Throwable t) {
        logMessage(INFO, msg, t);
    }


    public boolean isWarnEnabled() {
        return log4jLogger.isEnabledFor(WARN);
    }

    public void warn(String msg) {
        logMessage(WARN, msg);
    }

    public void warn(String format, Object arg) {
        logMessageFormat(WARN, getMessageFormat(format, arg));
    }

    public void warn(String format, Object[] argArray) {
        logMessageFormat(WARN, getMessageFormat(format, argArray));
    }

    public void warn(String format, Object arg1, Object arg2) {
        logMessageFormat(WARN, getMessageFormat(format, arg1, arg2));
    }

    public void warn(String msg, Throwable t) {
        logMessage(WARN, msg,t);
    }

    public boolean isErrorEnabled() {
        return log4jLogger.isEnabledFor(ERROR);
    }

    public void error(String msg) {
        logMessage(ERROR, msg);
    }

    public void error(String format, Object arg) {
        logMessageFormat(ERROR, getMessageFormat(format, arg));
    }

    public void error(String format, Object arg1, Object arg2) {
        logMessageFormat(ERROR, getMessageFormat(format, arg1, arg2));
    }

    public void error(String format, Object[] argArray) {
        logMessageFormat(ERROR, getMessageFormat(format, argArray));
    }

    public void error(String msg, Throwable t) {
        logMessage(ERROR, msg, t);
    }


    private FormattingTuple getMessageFormat(String format, Object... args) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, args);
        cleanIfException(ft.getThrowable());
        return ft;
    }

    private Throwable cleanIfException(Throwable t) {
        if (t != null) {
            GrailsUtil.deepSanitize(t);
        }
        return t;
    }

    private void logMessageFormat(Level level, FormattingTuple ft) {
        log4jLogger.log(FQCN, level, ft.getMessage(), ft.getThrowable());
    }

    private void logMessage(Level level, String msg) {
        logMessage(level, msg, null);
    }

    private void logMessage(Level level, String msg, Throwable t) {
        Throwable filteredTrace= !msg.startsWith(GrailsUtil.SANITIZING_STACKTRACE) ? cleanIfException(t) : t;

        log4jLogger.log(FQCN, level, msg, filteredTrace);
    }




}
