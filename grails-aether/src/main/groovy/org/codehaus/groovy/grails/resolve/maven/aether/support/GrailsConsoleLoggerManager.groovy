/*
 * Copyright 2013 the original author or authors.
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
package org.codehaus.groovy.grails.resolve.maven.aether.support

import grails.build.logging.GrailsConsole
import groovy.transform.CompileStatic

import org.codehaus.plexus.logging.AbstractLogger
import org.codehaus.plexus.logging.AbstractLoggerManager
import org.codehaus.plexus.logging.Logger

/**
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class GrailsConsoleLoggerManager extends AbstractLoggerManager {

    Logger logger = new GrailsConsoleLogger(Logger.LEVEL_INFO)

    void setThresholds(int threshold) {
        logger.setThreshold(threshold)
    }

    int getActiveLoggerCount() {
        return 1
    }
    Logger getLoggerForComponent(String role, String hint) {
        return logger
    }

    void returnComponentLogger(String role, String hint) {
        // noop
    }

    int getThreshold() {
        logger.threshold
    }

    void setThreshold(int threshold) {
        logger.threshold = threshold
    }
}

@CompileStatic
class GrailsConsoleLogger extends AbstractLogger {

    GrailsConsole grailsConsole = GrailsConsole.getInstance()

    GrailsConsoleLogger(int threshold) {
        super(threshold, "grailsConsole")
    }

    void debug(String message, Throwable throwable) {
        if (isDebugEnabled()) {
           if (throwable) {
               grailsConsole.error(message, throwable)
           }
           else {
               grailsConsole.log(message)
           }
        }
    }

    void info(String message, Throwable throwable) {
        if (isInfoEnabled()) {
            if (throwable) {
                grailsConsole.error(message, throwable)
            }
            else {
                grailsConsole.log(message)
            }
        }
    }

    void warn(String message, Throwable throwable) {
        if (isWarnEnabled()) {
            if (throwable) {
                grailsConsole.error(message, throwable)
            }
            else {
                grailsConsole.warn(message)
            }
        }
    }

    void error(String message, Throwable throwable) {
        if (isErrorEnabled()) {
            if (throwable) {
                grailsConsole.error(message, throwable)
            }
            else {
                grailsConsole.error(message)
            }
        }
    }

    void fatalError(String message, Throwable throwable) {
        if (isFatalErrorEnabled()) {
            if (throwable) {
                grailsConsole.error(message, throwable)
            }
            else {
                grailsConsole.error(message)
            }
        }
    }

    Logger getChildLogger(String name) {
        return this
    }
}
