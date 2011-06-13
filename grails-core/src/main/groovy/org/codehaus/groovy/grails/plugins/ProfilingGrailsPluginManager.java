/*
 * Copyright 2010 the original author or authors.
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
package org.codehaus.groovy.grails.plugins;

import grails.build.logging.GrailsConsole;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClassRegistry;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.exceptions.PluginException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

/**
 * A GrailsPluginManager implementation that outputs profile data to a logger
 *
 * @author Graeme Rocher
 * @since 1.4
 */
public class ProfilingGrailsPluginManager extends DefaultGrailsPluginManager {

    public ProfilingGrailsPluginManager(Class<?>[] plugins, GrailsApplication application) {
        super(plugins, application);
    }

    public ProfilingGrailsPluginManager(Resource[] pluginFiles, GrailsApplication application) {
        super(pluginFiles, application);
    }

    public ProfilingGrailsPluginManager(String resourcePath, GrailsApplication application) {
        super(resourcePath, application);
    }

    public ProfilingGrailsPluginManager(String[] pluginResources, GrailsApplication application) {
        super(pluginResources, application);
    }

    @Override
    public void loadPlugins() throws PluginException {
        GrailsConsole console = GrailsConsole.getInstance();
        long time = System.currentTimeMillis();
        console.addStatus("Loading plugins started");
        super.loadPlugins();
        console.addStatus("Loading plugins took " + (System.currentTimeMillis() - time));
    }

    @Override
    public void doDynamicMethods() {
        long time = System.currentTimeMillis();
        GrailsConsole console = GrailsConsole.getInstance();
        console.addStatus("doWithDynamicMethods started");
        checkInitialised();
        // remove common meta classes just to be sure
        MetaClassRegistry registry = GroovySystem.getMetaClassRegistry();
        for (Class<?> COMMON_CLASS : COMMON_CLASSES) {
            registry.removeMetaClass(COMMON_CLASS);
        }
        for (GrailsPlugin plugin : pluginList) {
            if (plugin.supportsCurrentScopeAndEnvironment()) {
                try {
                    long pluginTime = System.currentTimeMillis();
                    console.addStatus("doWithDynamicMethods for plugin [" + plugin.getName() + "] started");

                    plugin.doWithDynamicMethods(applicationContext);

                    console.addStatus("doWithDynamicMethods for plugin [" + plugin.getName() + "] took " + (System.currentTimeMillis() - pluginTime));
                }
                catch (Throwable t) {
                    console.error(t);
                    console.error("Error configuring dynamic methods for plugin " + plugin + ": " + t.getMessage());
                }
            }
        }
        console.addStatus("doWithDynamicMethods took " + (System.currentTimeMillis() - time));
    }

    @Override
    public void doRuntimeConfiguration(RuntimeSpringConfiguration springConfig) {
        long time = System.currentTimeMillis();

        GrailsConsole console = GrailsConsole.getInstance();
        console.addStatus("doWithSpring started");
        checkInitialised();
        for (GrailsPlugin plugin : pluginList) {
            if (plugin.supportsCurrentScopeAndEnvironment()) {
                long pluginTime = System.currentTimeMillis();
                console.addStatus("doWithSpring for plugin [" + plugin.getName() + "] started");
                plugin.doWithRuntimeConfiguration(springConfig);
                console.addStatus("doWithSpring for plugin [" + plugin.getName() + "] took " + (System.currentTimeMillis() - pluginTime));
            }
        }
        console.addStatus("doWithSpring took " + (System.currentTimeMillis() - time));
    }

    @Override
    public void doPostProcessing(ApplicationContext ctx) {
        long time = System.currentTimeMillis();
        GrailsConsole console = GrailsConsole.getInstance();
        console.addStatus("doWithApplicationContext started");
        checkInitialised();
        for (GrailsPlugin plugin : pluginList) {
            if (plugin.supportsCurrentScopeAndEnvironment()) {
                long pluginTime = System.currentTimeMillis();
                console.addStatus("doWithApplicationContext for plugin [" + plugin.getName() + "] started");
                plugin.doWithApplicationContext(ctx);
                console.addStatus("doWithApplicationContext for plugin [" + plugin.getName() + "] took " + (System.currentTimeMillis() - pluginTime));
            }
        }
        console.addStatus("doWithApplicationContext took " + (System.currentTimeMillis() - time));
    }

    @Override
    public void doArtefactConfiguration() {
        GrailsConsole console = GrailsConsole.getInstance();
        long time = System.currentTimeMillis();
        console.addStatus("doArtefactConfiguration started");
        super.doArtefactConfiguration();
        console.addStatus("doArtefactConfiguration took " + (System.currentTimeMillis() - time));
    }
}
