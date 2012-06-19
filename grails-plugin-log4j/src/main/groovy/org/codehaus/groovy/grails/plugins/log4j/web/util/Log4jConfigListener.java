/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.plugins.log4j.web.util;

import grails.util.Environment;
import grails.util.GrailsWebUtil;
import groovy.util.ConfigObject;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.LogManager;
import org.apache.log4j.helpers.LogLog;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.log4j.Log4jConfig;

/**
 * Configures Log4j in WAR deployment using Grails Log4j DSL.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
public class Log4jConfigListener implements ServletContextListener {

    public void contextInitialized(ServletContextEvent event) {
        try {
            GrailsApplication grailsApplication = GrailsWebUtil.lookupApplication(event.getServletContext());

            ConfigObject co = grailsApplication != null ? grailsApplication.getConfig() : null;
            if (co == null) {
                // in this case we're running inside a WAR deployed environment
                // create empty app to provide metadata
                GrailsApplication application = new DefaultGrailsApplication();
                co = application.getConfig();
                Log4jConfig.initialize(co);
            }
        }
        catch (Throwable e) {
            LogLog.error("Error initializing log4j: " + e.getMessage(), e);
        }
    }

    public void contextDestroyed(ServletContextEvent event) {
        if (Environment.getCurrent() != Environment.DEVELOPMENT) {
            LogManager.shutdown();
        }
    }
}
