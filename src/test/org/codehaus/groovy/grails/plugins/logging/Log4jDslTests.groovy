/*
 * Copyright 2004-2005 the original author or authors.
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

package org.codehaus.groovy.grails.plugins.logging

import org.apache.log4j.Logger
import org.apache.log4j.LogManager
import org.apache.log4j.Level
import grails.util.Environment
import org.apache.log4j.net.SMTPAppender
import org.apache.log4j.WriterAppender
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.Appender

class Log4jDslTests extends GroovyTestCase {

    protected void setUp() {
        super.setUp();
        System.setProperty(Environment.KEY, "")
    }

    protected void tearDown() {
        System.setProperty(Environment.KEY, "")
    }


    void testSingleDebugStatement() {
        LogManager.resetConfiguration()

        Log4jConfig config = new Log4jConfig()
        config.configure {
                debug 'org.hibernate.SQL'
        }
        def hibernateLogger = Logger.getLogger("org.hibernate.SQL")

        assertEquals hibernateLogger.level, Level.DEBUG
    	
    }

    void testEnvironmentSpecificLogging() {
        System.setProperty(Environment.KEY, "production")


        LogManager.resetConfiguration()

        Log4jConfig config = new Log4jConfig()
        config.configure {
            environments {
                development {
                    trace 'org.hibernate.SQL'
                }
                production {
                    error 'org.hibernate.SQL'
                }
            }

        }
        def hibernateLogger = Logger.getLogger("org.hibernate.SQL")

        assertEquals hibernateLogger.level, Level.ERROR

    }

    void testTraceLevel() {
        LogManager.resetConfiguration()

        Log4jConfig config = new Log4jConfig()
        config.configure {
            trace 'org.hibernate.SQL'
        }

        def hibernateLogger = Logger.getLogger("org.hibernate.SQL")

        assertEquals hibernateLogger.level, Level.TRACE
    }

    void testConfigureRootLogger() {
        LogManager.resetConfiguration()

        Log4jConfig config = new Log4jConfig()

        config.configure {
            root {
                debug()
                additivity = true
            }
        }

        def r = Logger.getRootLogger()

        assertEquals Level.DEBUG, r.level
        assertTrue r.additivity
        Appender a = r.allAppenders.nextElement()
        assertEquals "stdout",a.name

        LogManager.resetConfiguration()

        config.configure {
            appenders {
                appender new WriterAppender(name:'writerAppender')
            }
            root {
                trace 'writerAppender'
                additivity = true
            }
        }

        r = Logger.getRootLogger()

        assertEquals Level.TRACE, r.level
        assertTrue r.additivity
        a = r.allAppenders.nextElement()
        assertEquals "writerAppender",a.name

        LogManager.resetConfiguration()

        config.configure {
            appenders {
                appender new WriterAppender(name:'writerAppender')
            }
            root {
                warn 'writerAppender', 'stdout'
                additivity = true
            }
        }

        r = Logger.getRootLogger()

        assertEquals Level.WARN, r.level
        assertTrue r.additivity
        Enumeration appenders = r.allAppenders
        a = appenders.nextElement()
        assertEquals "writerAppender",a.name
        a = appenders.nextElement()
        assertEquals "stdout",a.name

    }

    void testSensibleDefaults() {
        LogManager.resetConfiguration()

        Log4jConfig config = new Log4jConfig()

        config.configure {
            debug 'org.codehaus.groovy.grails.web.servlet',
                   'org.codehaus.groovy.grails.web.pages'

            error 'org.codehaus.groovy.grails.web.sitemesh'
        }


        def root = Logger.getRootLogger()

        assertEquals Level.ERROR, root.level

        def appenders = root.getAllAppenders()
        assertTrue appenders.hasMoreElements()

        def consoleAppender = appenders.nextElement()
        assertEquals "stdout", consoleAppender.name

        def stackLogger = Logger.getLogger("StackTrace")
        assertFalse stackLogger.additivity

        assertTrue stackLogger.allAppenders.hasMoreElements()
        def fileAppender = stackLogger.allAppenders.nextElement()

        assertEquals "stacktrace.log", fileAppender.file

        def logger = Logger.getLogger('org.codehaus.groovy.grails.web.servlet')

        assertEquals Level.DEBUG, logger.level

    }

    void testCustomAppender() {

        LogManager.resetConfiguration()

        Log4jConfig config = new Log4jConfig()

        def consoleAppender
        config.configure {
            appenders {
                console follow:true,name:'customAppender', layout:pattern(conversionPattern: '%c{2} %m%n')
                appender name:'writerAppender', new WriterAppender()
            }
            debug customAppender: ['org.codehaus.groovy.grails.web.servlet',
                        'org.codehaus.groovy.grails.web.pages']

            error customAppender:'org.codehaus.groovy.grails.web.sitemesh',
                   writerAppender:'org.codehaus.groovy.grails.web.sitemesh'
        }


        def logger = Logger.getLogger('org.codehaus.groovy.grails.web.servlet')

        assertEquals Level.DEBUG, logger.level
        def appender = logger.getAppender("customAppender")

        assert appender
        assertTrue appender.follow
        assertEquals '%c{2} %m%n', appender.layout.conversionPattern

        logger = Logger.getLogger('org.codehaus.groovy.grails.web.sitemesh')

        assertEquals Level.ERROR, logger.level

        appender = logger.getAppender("customAppender")
        assert appender
        assertEquals '%c{2} %m%n', appender.layout.conversionPattern        
    }

    void testCustomAppenderWithInstance() {
        LogManager.resetConfiguration()
        Log4jConfig config = new Log4jConfig()

        config.configure {
            appenders {
                def consoleAppender = new ConsoleAppender(follow:true, target: "System.out", layout:pattern(conversionPattern: '%c{2} %m%n'))
                appender name:'customAppender', consoleAppender
            }

            error customAppender:'org.codehaus.groovy.grails.web.sitemesh'
        }

        def logger = Logger.getLogger('org.codehaus.groovy.grails.web.sitemesh')

        assertEquals Level.ERROR, logger.level
        def appender = logger.getAppender("customAppender")

        assert appender
        assertTrue appender.follow
        assertEquals "System.out", appender.target
        assertEquals '%c{2} %m%n', appender.layout.conversionPattern
    }
    
    /**
     * Tests that you can configure the root loader via the argument
     * passed into the Log4J closure.
     */
    void testRootLoggerModification() {

        LogManager.resetConfiguration()

        Log4jConfig config = new Log4jConfig()

        config.configure { root ->
            root.level = org.apache.log4j.Level.DEBUG
        }
        def rootLogger = Logger.rootLogger
        assertEquals Level.DEBUG, rootLogger.level
    }
}
