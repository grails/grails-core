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

import grails.util.Environment
import grails.util.Metadata
import org.apache.log4j.Appender
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.FileAppender
import org.apache.log4j.HTMLLayout
import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.apache.log4j.SimpleLayout
import org.apache.log4j.WriterAppender
import org.apache.log4j.xml.XMLLayout
import org.codehaus.groovy.grails.plugins.log4j.Log4jConfig

class   Log4jDslTests extends GroovyTestCase {

    private Log4jConfig log4jConfig = new Log4jConfig(new ConfigObject())

    protected void setUp() {
        super.setUp()
        setEnv ''
        LogManager.resetConfiguration()
    }

    protected void tearDown() {
        super.tearDown()
        setEnv ''
        Metadata.reset()
    }

    void testSingleDebugStatement() {
        log4jConfig.configure {
            debug 'org.hibernate.SQL'
        }

        assertEquals Logger.getLogger("org.hibernate.SQL").level, Level.DEBUG
    }

    void testEnvironmentSpecificLogging() {

        setEnv 'production'

        log4jConfig.configure {
            environments {
                development {
                    trace 'org.hibernate.SQL'
                }
                production {
                    error 'org.hibernate.SQL'
                }
                firstCustomEnv {
                    warn 'org.hibernate.SQL'
                }
                secondCustomEnv {
                    debug 'org.hibernate.SQL'
                }
            }
        }

        assertEquals Logger.getLogger("org.hibernate.SQL").level, Level.ERROR
    }

    void testCustomEnvironment() {

        setEnv 'firstCustomEnv'

        log4jConfig.configure {
            environments {
                development {
                    trace 'org.hibernate.SQL'
                }
                production {
                    error 'org.hibernate.SQL'
                }
                firstCustomEnv {
                    warn 'org.hibernate.SQL'
                }
                secondCustomEnv {
                    debug 'org.hibernate.SQL'
                }
            }
        }

        assertEquals Logger.getLogger("org.hibernate.SQL").level, Level.WARN
    }

    void testTraceLevel() {
        log4jConfig.configure {
            trace 'org.hibernate.SQL'
        }

        assertEquals Logger.getLogger("org.hibernate.SQL").level, Level.TRACE
    }

    void testConfigureRootLogger() {

        log4jConfig.configure {
            root {
                debug()
                additivity = true
            }
        }

        def r = Logger.getRootLogger()

        assertEquals Level.DEBUG, r.level
        assertTrue r.additivity
        Appender a = r.allAppenders.nextElement()
        assertEquals "stdout", a.name

        LogManager.resetConfiguration()

        log4jConfig.configure {
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
        assertEquals "writerAppender", a.name

        LogManager.resetConfiguration()

        log4jConfig.configure {
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
        assertEquals "writerAppender", a.name
        a = appenders.nextElement()
        assertEquals "stdout", a.name
    }

    void testSensibleDefaults() {

        log4jConfig.configure {
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

        assertEquals "stacktrace.log", new File(fileAppender.file).name

        def logger = Logger.getLogger('org.codehaus.groovy.grails.web.servlet')
        assertEquals Level.DEBUG, logger.level
    }

    void testDefaultsWarDeployed() {

        Metadata.getInstance(new ByteArrayInputStream("""
grails.war.deployed=true
""".bytes))

        assert Environment.isWarDeployed()

        log4jConfig.configure {
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

        assertEquals "stacktrace.log", new File(fileAppender.file).name

        def logger = Logger.getLogger('org.codehaus.groovy.grails.web.servlet')
        assertEquals Level.DEBUG, logger.level
    }
    void testCustomAppender() {

        def consoleAppender
        log4jConfig.configure {
            appenders {
                console follow: true, name: 'customAppender', layout: pattern(conversionPattern: '%c{2} %m%n')
                appender name:'writerAppender', new WriterAppender()
            }
            debug customAppender: ['org.codehaus.groovy.grails.web.servlet',
                                   'org.codehaus.groovy.grails.web.pages']

            error customAppender: 'org.codehaus.groovy.grails.web.sitemesh',
                  writerAppender: 'org.codehaus.groovy.grails.web.sitemesh'
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

        log4jConfig.configure {
            appenders {
                def consoleAppender = new ConsoleAppender(follow: true, target: "System.out",
                    layout: pattern(conversionPattern: '%c{2} %m%n'))
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

        log4jConfig.configure { root ->
            root.level = Level.DEBUG
        }

        def rootLogger = Logger.rootLogger
        assertEquals Level.DEBUG, rootLogger.level
    }

    void testConfigFromCollection() {
        log4jConfig.configure([{
            debug 'org.hibernate.SQL'
        },
        {
            warn 'org.hibernate.SQL'
        }])

        assertEquals Level.WARN, Logger.getLogger("org.hibernate.SQL").level
    }

    void testConfigFromMap() {

        def configData = [:]
        configData.main = {
            debug 'org.hibernate.SQL'
        }
        configData.secondary = {
            warn 'org.hibernate.SQL'
        }
        log4jConfig.configure(configData)

        assertEquals Level.WARN, Logger.getLogger("org.hibernate.SQL").level
    }

    void testLayouts() {

        new File('log.xml').deleteOnExit()
        new File('log.html').deleteOnExit()
        new File('simple.log').deleteOnExit()

        log4jConfig.configure {
            appenders {
                file name: 'fileXml',    layout: xml,    file: 'log.xml'
                file name: 'fileHtml',   layout: html,   file: 'log.html'
                file name: 'fileSimple', layout: simple, file: 'simple.log'
            }
            root {
                debug 'fileXml', 'fileHtml', 'fileSimple'
            }
        }

        Logger root = Logger.rootLogger

        Appender appender = root.getAppender('fileXml')
        assert appender instanceof FileAppender
        FileAppender fa = appender
        assert 'log.xml' == fa.file

        assert fa.layout instanceof XMLLayout
        assert fa.name == 'fileXml'

        appender = root.getAppender('fileHtml')
        assert appender instanceof FileAppender
        fa = appender
        assert 'log.html' == fa.file
        assert fa.layout instanceof HTMLLayout
        assert fa.name == 'fileHtml'

        appender = root.getAppender('fileSimple')
        assert appender instanceof FileAppender
        fa = appender
        assert 'simple.log' == fa.file
        assert fa.layout instanceof SimpleLayout
        assert fa.name == 'fileSimple'
    }

    void testPropertyMissing() {
        shouldFail(MissingPropertyException) {
            log4jConfig.nonExistentProp
        }
    }

    void testPropertyMissingResortsToOwner() {

        ConfigObject config = new ConfigSlurper().parse("""

            configured.loggingRoot = 'configured/and/fake'

            log4j = {
                def loggingRoot = configured.loggingRoot
                appenders {
                    rollingFile name: 'quartz', file: "\${loggingRoot}/quartz.log"
                }

                debug quartz: 'org.quartz', additivity: false
            }
        """)

        new Log4jConfig(config).configure(config.log4j)

        Logger logger = Logger.getLogger('org.quartz')

        Appender appender = logger.getAppender('quartz')
        assert appender
        assert "quartz" == appender.name
        assert "configured/and/fake/quartz.log" == appender.file
    }

    private void setEnv(String name) {
        System.setProperty Environment.KEY, name
    }
}
