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
package org.codehaus.groovy.grails.plugins.log4j.appenders;

import grails.build.logging.GrailsConsole;
import groovy.util.ConfigObject;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.codehaus.groovy.grails.cli.logging.GrailsConsolePrintStream;
import org.codehaus.groovy.grails.exceptions.*;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A Log4j appender that appends to the GrailsConsole instance.
 * Not for use in production/WAR deployed scenarios!
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class GrailsConsoleAppender extends AppenderSkeleton {

    public static final String MESSAGE_PREFIX = "Message: ";
    GrailsConsole console = GrailsConsole.getInstance();
    private StackTracePrinter stackTracePrinter;
    private StackTraceFilterer stackTraceFilterer;

    public GrailsConsoleAppender(ConfigObject config) {
        createStackTracePrinter(config);
        createStackTraceFilterer(config);
    }

    @Override
    protected void append(LoggingEvent event) {
        Level level = event.getLevel();
        String message = buildMessage(event);
        if (System.out instanceof GrailsConsolePrintStream) {
            if (level.equals(Level.ERROR) || level.equals(Level.FATAL)) {
                console.error(message);
            }
            else {
                console.log(message);
            }
        }
        else {
            if (level.equals(Level.ERROR) || level.equals(Level.FATAL)) {
                System.err.println(message);
            }
            else {
                System.out.println(message);
            }
        }
    }

    private String buildMessage(LoggingEvent event) {
        StringBuilder b = new StringBuilder(layout.format(event));

        if (console.isVerbose()) {
            String[] throwableStrRep = event.getThrowableStrRep();
            if (throwableStrRep != null) {
                b.append(Layout.LINE_SEP);
                for (String line : throwableStrRep) {
                    b.append(line).append(Layout.LINE_SEP);
                }
            }
        }
        else {
            ThrowableInformation throwableInformation = event.getThrowableInformation();
            if (throwableInformation != null) {
                Throwable throwable = throwableInformation.getThrowable();
                if (throwable != null) {
                    b.append(MESSAGE_PREFIX).append(throwable.getMessage()).append(Layout.LINE_SEP);
                    stackTraceFilterer.filter(throwable, true);
                    b.append(stackTracePrinter.prettyPrint(throwable));
                }
            }
        }

        return b.toString();
    }

    public void close() {
        // do nothing
    }

    public boolean requiresLayout() {
        return true;
    }

    protected void createStackTracePrinter(ConfigObject config) {
       try {
           stackTracePrinter = (StackTracePrinter) GrailsResourceUtils.instantiateFromConfig(
                   config, "grails.logging.stackTracePrinterClass", DefaultStackTracePrinter.class.getName());
       }
       catch (Throwable t) {
           LogLog.warn("Problem instantiating StackTracePrinter class, using default: " + t.getMessage());
           try {
               stackTracePrinter = new DefaultStackTracePrinter();
           } catch (Throwable t1) {
               stackTracePrinter = new StackTracePrinter() {
                  /**
                    * Pretty print the given stack trace and return the result
                    *
                    * @param throwable The throwable
                    * @return The result
                   */
                   public String prettyPrint(Throwable throwable) {
                       StringWriter sw = new StringWriter();
                       PrintWriter pw = new PrintWriter(sw);
                       throwable.printStackTrace(pw);
                       return sw.toString();
                   }
               };
           }
       }
    }

    protected void createStackTraceFilterer(ConfigObject config) {
        try {
            stackTraceFilterer = (StackTraceFilterer)GrailsResourceUtils.instantiateFromConfig(
                    config, "grails.logging.stackTraceFiltererClass", DefaultStackTraceFilterer.class.getName());
        }
        catch (Throwable t) {
            LogLog.error("Problem instantiating StackTraceFilter class, using default: " + t.getMessage());
            stackTraceFilterer = new DefaultStackTraceFilterer();
        }
    }
}
