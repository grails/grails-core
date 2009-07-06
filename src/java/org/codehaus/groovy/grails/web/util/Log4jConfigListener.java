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
package org.codehaus.groovy.grails.web.util;

import grails.util.Environment;
import groovy.lang.Closure;
import groovy.util.ConfigObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.LogManager;
import org.apache.log4j.helpers.LogLog;
import org.codehaus.groovy.grails.commons.ApplicationHolder;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.cfg.ConfigurationHelper;
import org.codehaus.groovy.grails.plugins.logging.Log4jConfig;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * A listener that will configure Log4j in WAR deployment using Grails Log4j DSL
 *
 * @author Graeme Rocher
 * @since 1.1
 *        <p/>
 *        Created: Sep 26, 2008
 */
public class Log4jConfigListener implements ServletContextListener {

    static final Log LOG = LogFactory.getLog(Log4jConfigListener.class);

    public void contextInitialized(ServletContextEvent event) {
        try {
            ConfigObject co = ConfigurationHolder.getConfig();
            if(co == null) {
                // in this case we're running inside a WAR deployed environment
                // create empty app to provide metadata
                GrailsApplication application = new DefaultGrailsApplication();
                try {
                    ApplicationHolder.setApplication(application);
                    co = ConfigurationHelper.loadConfigFromClasspath(application);
                }
                finally {
                    ApplicationHolder.setApplication(null);
                }

                Object o = co.get("log4j");
                if(o instanceof Closure) {
                    new Log4jConfig().configure((Closure)o);
                }
                else {
                    new Log4jConfig().configure();
                }
            }
        }
        catch (Throwable e) {
            LogLog.error("Error initializing log4j: " + e.getMessage(), e);
        }
    }

    public void contextDestroyed(ServletContextEvent event) {
        if(Environment.getCurrent() != Environment.DEVELOPMENT)
            LogManager.shutdown();
    }
}
