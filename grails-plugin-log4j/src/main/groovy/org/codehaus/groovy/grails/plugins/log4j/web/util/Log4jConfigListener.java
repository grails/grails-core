/*
 * Copyright 2004-2005 Graeme Rocher
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
import groovy.util.ConfigObject;

import java.lang.reflect.Method;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.LogManager;
import org.apache.log4j.helpers.LogLog;
import org.codehaus.groovy.grails.plugins.exceptions.PluginException;
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
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            Object grailsApplication = createGrailsApplication(contextClassLoader);
            ConfigObject co = getConfig(grailsApplication);
            Log4jConfig.initialize(co);
        }
        catch (Throwable e) {
            LogLog.error("Error initializing log4j: " + e.getMessage(), e);
        }
    }

    private Object createGrailsApplication(ClassLoader contextClassLoader) {
        try {
            Class<?> applicationClass = contextClassLoader.loadClass("org.codehaus.groovy.grails.commons.DefaultGrailsApplication");
            return applicationClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new PluginException("Error instantiating GrailsApplication during logging initialization: " + e.getMessage(), e);
        } catch (InstantiationException e) {
            throw new PluginException("Error instantiating GrailsApplication during logging initialization: " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new PluginException("Error instantiating GrailsApplication during logging initialization: " + e.getMessage(), e);
        }
    }

    private ConfigObject getConfig(Object grailsApplication) {
        try {
            Method getConfigMethod = grailsApplication.getClass().getMethod("getConfig", new Class[0]);
            return (ConfigObject) getConfigMethod.invoke(grailsApplication);
        } catch (Throwable e) {
            return null;
        }
    }

    public void contextDestroyed(ServletContextEvent event) {
        if (Environment.getCurrent() != Environment.DEVELOPMENT) {
            LogManager.shutdown();
        }
    }
}
