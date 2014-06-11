/*
 * Copyright 2014 the original author or authors.
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

package org.codehaus.groovy.grails.cli.fork;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Shared shutdown hook for forked Grails processes.
 * 
 * Takes care of terminating forked processes when the parent Grails process is terminated.
 * 
 * @author Lari Hotari
 *
 */
public class ForkedProcessShutdownHooks {
    private static final Set<Process> processes = new LinkedHashSet<Process>();
    private static Thread shutdownHook;

    public static synchronized boolean add(Process process) {
        if (shutdownHook == null) {
            shutdownHook = new Thread("ForkedGrailsProcess Shutdown Hook") {
                @Override
                public void run() {
                    destroyProcesses();
                }
            };
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
        return processes.add(process);
    }

    public static synchronized boolean remove(Process process) {
        boolean wasRemoved = processes.remove(process);
        if (processes.isEmpty() && shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (Throwable e) {
                // ignore
            }
            shutdownHook = null;
        }
        return wasRemoved;
    }
    
    private static synchronized void destroyProcesses() {
        for (Process process : processes) {
            try {
                process.destroy();
            } catch (Throwable e) {
                // ignore
            }
        }
        processes.clear();
    }
}