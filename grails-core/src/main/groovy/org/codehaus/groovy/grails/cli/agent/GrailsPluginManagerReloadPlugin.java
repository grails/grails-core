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
package org.codehaus.groovy.grails.cli.agent;

import org.springsource.loaded.Plugins;
import org.springsource.loaded.ReloadEventProcessorPlugin;
import org.codehaus.groovy.grails.commons.ClassPropertyFetcher;
import org.codehaus.groovy.grails.compiler.GrailsProjectWatcher;
import org.springframework.beans.CachedIntrospectionResults;
import java.beans.Introspector;

/**
 * Reloading agent plugin for use with the GrailsPluginManager.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class GrailsPluginManagerReloadPlugin implements ReloadEventProcessorPlugin {

    public boolean shouldRerunStaticInitializer(String typename, Class<?> aClass, String encodedTimestamp) {
        return GrailsProjectWatcher.isActive();
    }

    public void reloadEvent(String typename, Class<?> aClass, String encodedTimestamp) {
        CachedIntrospectionResults.clearClassLoader(aClass.getClassLoader());
        ClassPropertyFetcher.clearClassPropertyFetcherCache();
        if (GrailsProjectWatcher.isActive()) {
            Introspector.flushFromCaches(aClass);
            GrailsProjectWatcher.firePendingClassChangeEvents(aClass);
        }
    }

    public static void register() {
        Plugins.registerGlobalPlugin(new GrailsPluginManagerReloadPlugin());
    }
}
