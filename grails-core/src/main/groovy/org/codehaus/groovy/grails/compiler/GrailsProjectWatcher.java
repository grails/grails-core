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
import org.codehaus.groovy.grails.cli.agent.GrailsPluginManagerReloadPlugin;
import org.codehaus.groovy.grails.commons.ClassPropertyFetcher;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.support.WatchPattern;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Watches a Grails projects and re-compiles sources when they change or fires events to the pluginManager
 *
 * @author Graeme Rocher
 * @since 1.4
 */
public class GrailsProjectWatcher extends DirectoryWatcher{
    private static final Log LOG = LogFactory.getLog(GrailsProjectWatcher.class);
    private static final Map<String, ClassUpdate> classChangeEventQueue = new ConcurrentHashMap<String, ClassUpdate>();
    private static boolean active = false;
    public static final String SPRING_LOADED_PLUGIN_CLASS = "com.springsource.loaded.Plugins";


    private List<String> compilerExtensions;
    private GrailsPluginManager pluginManager;
    private GrailsProjectCompiler compiler;
    private Map<File, GrailsPlugin> descriptorToPluginMap = new ConcurrentHashMap<File, GrailsPlugin>();

    public GrailsProjectWatcher(final GrailsProjectCompiler compiler, GrailsPluginManager pluginManager) {
        super();


        this.pluginManager = pluginManager;
        this.compilerExtensions = compiler.getCompilerExtensions();
        this.compiler = compiler;
        if (isReloadingAgentPresent()) {
            GrailsPluginManagerReloadPlugin.register();
        }
    }

    public static boolean isReloadingAgentPresent() {
        return ClassUtils.isPresent(SPRING_LOADED_PLUGIN_CLASS, GrailsProjectWatcher.class.getClassLoader());
    }

    /**
     * Whether the watcher is active
     * @return True if it is
     */
    public static boolean isActive() {
        return active;
    }

    /**
     * Fire any pending class change events
     * @param updatedClass The class to update
     */
    public static void firePendingClassChangeEvents(Class<?> updatedClass) {
        if (updatedClass != null) {
            ClassUpdate classUpdate = classChangeEventQueue.remove(updatedClass.getName());
            if (classUpdate != null) {
                  classUpdate.run(updatedClass);
            }
        }
    }

    @Override
    public void run() {
        active = true;
        for (String directory : compiler.getSrcDirectories()) {
            addWatchDirectory(new File(directory), compilerExtensions);
        }
        Resource[] pluginSourceFiles = compiler.getPluginSettings().getPluginSourceFiles();
        for (Resource pluginSourceFile : pluginSourceFiles) {
            try {
                if (pluginSourceFile.getFile().isDirectory()) {
                    addWatchDirectory(pluginSourceFile.getFile(), compilerExtensions);
                }
            } catch (IOException e) {
                // ignore
            }
        }

        addListener(new FileChangeListener() {
            public void onChange(File file) {
                LOG.info("File ["+file+"] changed. Applying changes to application.");
                if (descriptorToPluginMap.containsKey(file)) {
                    reloadPlugin(file);
                }
                else {
                    compileIfSource(file);
                    informPluginManager(file, false);
                }

            }

            public void onNew(File file) {
                LOG.info("File ["+file+"] added. Applying changes to application.");
                sleep(5000);
                compileIfSource(file);
                informPluginManager(file, true);
            }
        });

        GrailsPlugin[] allPlugins = pluginManager.getAllPlugins();

        for (GrailsPlugin plugin : allPlugins) {
            // watch the plugin descriptor for changes
            GrailsPluginInfo info = compiler.getPluginSettings().getPluginInfoForName(plugin.getFileSystemShortName());

            if (info != null && info.getDescriptor() != null) {
                try {
                    Resource descriptor = info.getDescriptor();
                    plugin.setDescriptor(descriptor);
                    File pluginFile = descriptor.getFile();
                    descriptorToPluginMap.put(pluginFile, plugin);
                    addWatchFile(pluginFile);
                } catch (IOException e) {
                    // ignore
                }
            }
            List<WatchPattern> watchPatterns = plugin.getWatchedResourcePatterns();
            if (watchPatterns != null) {
                for (WatchPattern watchPattern : watchPatterns) {
                    if (watchPattern.getFile() != null) {
                        addWatchFile(watchPattern.getFile());
                    }
                    else if (watchPattern.getDirectory() != null) {
                        addWatchDirectory(watchPattern.getDirectory(),watchPattern.getExtension());
                    }
                }
            }

        }
        super.run();
    }

    private void reloadPlugin(File file) {
        GrailsPlugin grailsPlugin = descriptorToPluginMap.get(file);
        grailsPlugin.refresh();
        if (pluginManager instanceof DefaultGrailsPluginManager) {
            ((DefaultGrailsPluginManager)pluginManager).reloadPlugin(grailsPlugin);
        }
    }

    private void informPluginManager(final File file, boolean isNew) {
        if (!isSourceFile(file) || isNew) {
            try {
                pluginManager.informOfFileChange(file);
            } catch (Exception e) {
                LOG.error("Failed to reload file ["+file+"] with error: " + e.getMessage());
            }
        }
        else {
            // add to class change event queue
            String className = GrailsResourceUtils.getClassName(file.getAbsolutePath());
            if (className != null) {

                classChangeEventQueue.put(className, new ClassUpdate() {
                    public void run(Class<?> cls) {
                        try {
                            pluginManager.informOfClassChange(file, cls);
                        } catch (Exception e) {
                            LOG.error("Failed to reload file [" + file + "] with error: " + e.getMessage());
                        }
                    }
                });
            }
        }
    }

    private void compileIfSource(File file) {
        try {
            if (isSourceFile(file)) {
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
            if (file.getName().endsWith(compilerExtension)) {
                return true;
            }
        }
        return false;
    }

    private interface ClassUpdate {
        void run(Class<?> cls);
    }
}
