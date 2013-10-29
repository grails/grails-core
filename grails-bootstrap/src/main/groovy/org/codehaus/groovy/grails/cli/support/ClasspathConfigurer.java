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
package org.codehaus.groovy.grails.cli.support;

import grails.build.logging.GrailsConsole;
import grails.util.BuildSettings;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.groovy.grails.resolve.DependencyReport;

/**
 * Support class that configures the Grails classpath when executing command line scripts.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class ClasspathConfigurer {

    private BuildSettings settings;
    private boolean skipPlugins;
    private PluginPathDiscoverySupport pluginPathSupport;
    private boolean exitOnResolveError = true;

    public ClasspathConfigurer(PluginPathDiscoverySupport pluginPathSupport, BuildSettings settings, boolean skipPlugins) {
        this.settings = settings;
        this.skipPlugins = skipPlugins;
        this.pluginPathSupport = pluginPathSupport;
    }

    public ClasspathConfigurer(BuildSettings build, boolean skipPlugins) {
        this(new PluginPathDiscoverySupport(build), build, skipPlugins);
    }

    public URLClassLoader configuredClassLoader() {
        // The class loader we will use to run Gant. It's the root
        // loader plus all the application's compiled classes.
        URLClassLoader classLoader;
        try {
            // JARs already on the classpath should be ed.
            Set<String> existingJars = new HashSet<String>();
            for (URL url : settings.getRootLoader().getURLs()) {
                existingJars.add(url.getFile());
            }

            URL[] urls = getClassLoaderUrls(settings, new File(settings.getProjectWorkDir(), "scriptCache"), existingJars, skipPlugins);
            addUrlsToRootLoader(settings.getRootLoader(), urls);

            // The compiled classes of the application!
            urls = new URL[]{settings.getClassesDir().toURI().toURL(), settings.getPluginClassesDir().toURI().toURL()};
            classLoader = new URLClassLoader(urls, settings.getRootLoader());
            Thread.currentThread().setContextClassLoader(classLoader);
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Invalid classpath URL", ex);
        }
        return classLoader;
    }

    /**
     * Creates a new root loader with the Grails libraries and the
     * application's plugin libraries on the classpath.
     */
    protected URL[] getClassLoaderUrls(BuildSettings settings, File cacheDir, Set<String> excludes,
                                       boolean skipPlugins) throws MalformedURLException {
        List<URL> urls = new ArrayList<URL>();

        // If 'grailsHome' is set, make sure the script cache directory takes precedence
        // over the "grails-scripts" JAR by adding it first.
        if (settings.getGrailsHome() != null) {
            urls.add(cacheDir.toURI().toURL());
        }

        // Add the "resources" directory so that config files and the
        // like can be picked up off the classpath.
        if (settings.getResourcesDir() != null && settings.getResourcesDir().exists()) {
            urls.add(settings.getResourcesDir().toURI().toURL());
        }

        // Add build-only dependencies to the project
        final boolean dependenciesExternallyConfigured = settings.isDependenciesExternallyConfigured();
        // add dependencies required by the build system
        final List<File> buildDependencies;
        buildDependencies = settings.getBuildDependencies();
        if (!dependenciesExternallyConfigured && buildDependencies.isEmpty()) {
            GrailsConsole.getInstance().error("Required Grails build dependencies were not found. This is normally due to internet connectivity issues (such as a misconfigured proxy) or missing repositories in grails-app/conf/BuildConfig.groovy. Please verify your configuration to continue.");
            cleanResolveCache(settings);

            System.exit(1);
        }
        addDependenciesToURLs(excludes, urls, buildDependencies);
        // add dependencies required at development time, but not at deployment time
        addDependenciesToURLs(excludes, urls, settings.getProvidedDependencies());
        // Add the project's test dependencies (which include runtime dependencies) because most of them
        // will be required for the build to work.
        addDependenciesToURLs(excludes, urls, settings.getTestDependencies());

        // Important, we call these so they're properly initialized!
        settings.getRuntimeDependencies();

        settings.getCompileDependencies();

        // Add the libraries of both project and global plugins.
        if (!skipPlugins) {
            for (File dir : pluginPathSupport.listKnownPluginDirs()) {
                addPluginLibs(dir, urls, settings);
            }
        }

        DependencyReport buildResolveReport = settings.getBuildResolveReport();
        if (buildResolveReport != null && buildResolveReport.hasError()) {
            handleResolveError(settings, buildResolveReport);
        }
        DependencyReport compileResolveReport = settings.getCompileResolveReport();
        if (compileResolveReport != null && compileResolveReport.hasError()) {
            handleResolveError(settings, compileResolveReport);
        }
        DependencyReport runtimeResolveReport = settings.getRuntimeResolveReport();
        if (runtimeResolveReport != null && runtimeResolveReport.hasError()) {
            handleResolveError(settings, runtimeResolveReport);
        }
        DependencyReport testResolveReport = settings.getTestResolveReport();
        if (testResolveReport != null && testResolveReport.hasError()) {
            handleResolveError(settings, testResolveReport);
        }
        DependencyReport providedResolveReport = settings.getProvidedResolveReport();
        if (providedResolveReport != null && providedResolveReport.hasError()) {
            handleResolveError(settings, providedResolveReport);
        }
        settings.storeDependencyCache();
        return urls.toArray(new URL[urls.size()]);
    }

    private void handleResolveError(BuildSettings settings, DependencyReport buildResolveReport) {
        cleanResolveCache(settings);
        GrailsConsole grailsConsole = GrailsConsole.getInstance();
        grailsConsole.error(buildResolveReport.getResolveError().getMessage());
        grailsConsole.addStatus("Run 'grails dependency-report' for further information.");
        if (exitOnResolveError) {
            System.exit(1);
        }
    }

    public static void cleanResolveCache(BuildSettings settings) {
        File projectWorkDir = settings.getProjectWorkDir();
        if (projectWorkDir != null) {
            File[] files = projectWorkDir.listFiles(new FilenameFilter() {

                public boolean accept(File file, String s) {
                    return s.endsWith(".resolve");
                }
            });
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }

    protected void addDependenciesToURLs(Set<String> excludes, List<URL> urls, List<File> runtimeDeps) throws MalformedURLException {
        if (runtimeDeps == null) {
            return;
        }

        for (File file : runtimeDeps) {
            if (file == null ) {
                continue;
            }


            if (file.getName().contains("xercesImpl")) {
                // workaround for GRAILS-9708
                System.setProperty("javax.xml.parsers.DocumentBuilderFactory","com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
            }
            if (excludes != null && !excludes.contains(file.getName())) {
                URL url = file.toURI().toURL();
                if (urls.contains(url)) continue;

                urls.add(url);
                excludes.add(file.getName());
            }
        }
    }

    /**
     * Adds all the libraries in a plugin to the given list of URLs.
     *
     * @param pluginDir The directory containing the plugin.
     * @param urls      The list of URLs to add the plugin JARs to.
     * @param settings
     */
    protected void addPluginLibs(File pluginDir, List<URL> urls, BuildSettings settings) throws MalformedURLException {
        if (!pluginDir.exists()) return;

        // otherwise just add them
        File libDir = new File(pluginDir, "lib");
        if (libDir.exists()) {
            addLibs(libDir, urls, Collections.emptyList());
        }
    }

    /**
     * Adds all the JAR files in the given directory to the list of URLs. Excludes any
     * "standard-*.jar" and "jstl-*.jar" because these are added to the classpath in another
     * place. They depend on the servlet version of the app and so need to be treated specially.
     */
    protected void addLibs(File dir, List<URL> urls, Collection<?> excludes) throws MalformedURLException {
        if (!dir.exists()) {
            return;
        }

        for (File file : dir.listFiles()) {
            boolean include = true;
            for (Object me : excludes) {
                String exclude = me.toString();
                if (file.getName().contains(exclude)) {
                    include = false;
                    break;
                }
            }
            if (include) {
                urls.add(file.toURI().toURL());
            }
        }
    }

    /**
     * <p>A Groovy RootLoader should be used to load GrailsScriptRunner,
     * but this leaves us with a problem. If we want to extend its
     * classpath by adding extra URLs, we have to use the addURL()
     * method that is only public on RootLoader (it's protected on
     * URLClassLoader). Unfortunately, due to the nature of Groovy's
     * RootLoader a declared type of RootLoader in this class is not
     * the same type as GrailsScriptRunner's class loader <i>because
     * the two are loaded by different class loaders</i>.</p>
     * <p>In other words, we can't add URLs via the addURL() method
     * because we can't "see" it from Java. Instead, we use reflection
     * to invoke it.</p>
     *
     * @param loader The root loader whose classpath we want to extend.
     * @param urls   The URLs to add to the root loader's classpath.
     */
    protected void addUrlsToRootLoader(URLClassLoader loader, URL[] urls) {
        try {
            Class<?> loaderClass = loader.getClass();
            Method method = loaderClass.getMethod("addURL", URL.class);
            for (URL url : urls) {
                method.invoke(loader, url);
            }
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Cannot dynamically add URLs to GrailsScriptRunner's" +
                            " class loader - make sure that it is loaded by Groovy's" +
                            " RootLoader or a sub-class.");
        }
    }

    public void setExitOnResolveError(boolean exitOnResolveError) {
        this.exitOnResolveError = exitOnResolveError;
    }
}
