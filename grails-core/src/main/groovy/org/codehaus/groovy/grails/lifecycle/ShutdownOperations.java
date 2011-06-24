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
package org.codehaus.groovy.grails.lifecycle;

import groovy.lang.ExpandoMetaClass;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ApplicationHolder;
import org.codehaus.groovy.grails.commons.ClassPropertyFetcher;
import org.codehaus.groovy.grails.commons.cfg.ConfigurationHelper;
import org.codehaus.groovy.grails.plugins.PluginManagerHolder;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Operations that should be executed on shutdown
 *
 * @author Graeme Rocher
 * @since 1.4
 */
public class ShutdownOperations {

    private static final Log LOG = LogFactory.getLog(ShutdownOperations.class);

    private static final Collection<Runnable> shutdownOperations = new ConcurrentLinkedQueue<Runnable>();


    public static final Runnable DEFAULT_SHUTDOWN_OPERATION = new Runnable() {

        public void run() {
            PluginManagerHolder.setPluginManager(null);
            ApplicationHolder.setApplication(null);
            ConfigurationHelper.clearCachedConfigs();
            //ExpandoMetaClass.disableGlobally();
            ClassPropertyFetcher.clearClassPropertyFetcherCache();
        }
    };

    static {
        // default operations
        shutdownOperations.add(DEFAULT_SHUTDOWN_OPERATION);
    }

    /**
     * Runs the shutdown operations
     */
    public static void runOperations() {
        try {
            for (Runnable shutdownOperation : shutdownOperations) {
                try {
                    shutdownOperation.run();
                } catch (Exception e) {
                    LOG.warn("Error occurred running shutdown operation: " + e.getMessage(), e);
                }
            }
        } finally {
            shutdownOperations.clear();
            shutdownOperations.add(DEFAULT_SHUTDOWN_OPERATION);
        }


    }

    /**
     * Adds a shutdown operation
     * @param runnable The runnable operation
     */
    public static void addOperation(Runnable runnable) {
        shutdownOperations.add(runnable);
    }
}
