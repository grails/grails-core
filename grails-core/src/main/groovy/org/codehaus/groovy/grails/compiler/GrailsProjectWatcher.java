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
package org.codehaus.groovy.grails.compiler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.BuildException;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.grails.commons.ClassPropertyFetcher;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.support.WatchPattern;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Watches a Grails projects and re-compiles sources when they change or fires events to the pluginManager
 *
 * @author Graeme Rocher
 * @since 1.4
 */
public class GrailsProjectWatcher extends DirectoryWatcher{
    private static final Log LOG = LogFactory.getLog(GrailsProjectWatcher.class);
    private static final Queue<Runnable> classChangeEventQueue = new ConcurrentLinkedQueue<Runnable>();


    private List<String> compilerExtensions;
    private GrailsPluginManager pluginManager;
    private GrailsProjectCompiler compiler;

    public GrailsProjectWatcher(final GrailsProjectCompiler compiler, GrailsPluginManager pluginManager) {
        super();


        this.pluginManager = pluginManager;
        this.compilerExtensions = compiler.getCompilerExtensions();
        this.compiler = compiler;
    }


    /**
     * Fire any pending class change events
     */
    public static void firePendingClassChangeEvents() {
        for (Runnable runnable : classChangeEventQueue) {
            runnable.run();
        }
        classChangeEventQueue.clear();
    }

    @Override
    public void run() {
        for (String directory : compiler.getSrcDirectories()) {
            addWatchDirectory(new File(directory), compilerExtensions);
        }
        Resource[] pluginSourceFiles = compiler.getPluginSettings().getPluginSourceFiles();
        for (Resource pluginSourceFile : pluginSourceFiles) {
            try {
                if(pluginSourceFile.getFile().isDirectory()) {
                    addWatchDirectory(pluginSourceFile.getFile(), compilerExtensions);
                }
            } catch (IOException e) {
                // ignore
            }
        }

        addListener(new FileChangeListener() {
            public void onChange(File file) {
                LOG.info("File ["+file+"] changed. Applying changes to application.");
                compileIfSource(file);
                informPluginManager(file);
            }

            public void onNew(File file) {
                LOG.info("File ["+file+"] added. Applying changes to application.");
                sleep(5000);
                compileIfSource(file);
                informPluginManager(file);
            }
        });

        GrailsPlugin[] allPlugins = pluginManager.getAllPlugins();

        for (GrailsPlugin plugin : allPlugins) {
            List<WatchPattern> watchPatterns = plugin.getWatchedResourcePatterns();
            if(watchPatterns != null) {
                for (WatchPattern watchPattern : watchPatterns) {
                    if(watchPattern.getFile() != null) {
                        addWatchFile(watchPattern.getFile());
                    }
                    else if(watchPattern.getDirectory() != null) {
                        addWatchDirectory(watchPattern.getDirectory(),watchPattern.getExtension());
                    }
                }
            }

        }
        super.run();
    }

    private void informPluginManager(final File file) {
        if(!isSourceFile(file)) {
            try {
                pluginManager.informOfFileChange(file);
            } catch (Exception e) {
                LOG.error("Failed to reload file ["+file+"] with error: " + e.getMessage());
            }
        }
        else {
            // add to class change event queue
            classChangeEventQueue.add(new Runnable() {
                public void run() {
                    pluginManager.informOfFileChange(file);
                }
            });
        }
    }

    private void compileIfSource(File file) {
        try {
            if(isSourceFile(file)) {
                compiler.compileAll();
                ClassPropertyFetcher.clearClassPropertyFetcherCache();
            }
        }
        catch (MultipleCompilationErrorsException e) {
            LOG.error("Compilation Error: " + e.getMessage());
        }
        catch(CompilationFailedException e) {
            LOG.error("Compilation Error: " + e.getMessage());
        }
        catch(BuildException e) {
            LOG.error("Compilation Error: " + e.getCause().getMessage());
        }
    }


    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private boolean isSourceFile(File file) {
        for (String compilerExtension : compilerExtensions) {
            if(file.getName().endsWith(compilerExtension))
                return true;
        }
        return false;
    }
}
