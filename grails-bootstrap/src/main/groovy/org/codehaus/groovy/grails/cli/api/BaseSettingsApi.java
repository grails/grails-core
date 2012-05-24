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

package org.codehaus.groovy.grails.cli.api;

import grails.build.logging.GrailsConsole;
import grails.util.BuildSettings;
import grails.util.GrailsNameUtils;
import grails.util.Metadata;
import grails.util.PluginBuildSettings;
import groovy.lang.Closure;
import groovy.util.ConfigSlurper;
import org.codehaus.groovy.grails.cli.ScriptExitException;
import org.codehaus.groovy.grails.cli.support.GrailsBuildEventListener;
import org.codehaus.groovy.grails.cli.support.UaaIntegration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * Utility methods used on the command line.
 *
 * @author Graeme Rocher
 */
public class BaseSettingsApi {

    private static final Resource[] NO_RESOURCES = new Resource[0];
    protected BuildSettings buildSettings;
    protected Properties buildProps;
    protected PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    protected File grailsHome;
    protected Metadata metadata;
    protected File metadataFile;
    protected boolean enableProfile;
    protected boolean isInteractive;
    protected String pluginsHome;
    protected PluginBuildSettings pluginSettings;
    protected String grailsAppName;
    protected Object appClassName;
    protected ConfigSlurper configSlurper;
    private GrailsBuildEventListener buildEventListener;

    public BaseSettingsApi(final BuildSettings buildSettings, boolean interactive) {
        this(buildSettings, null, interactive);
    }

    public BaseSettingsApi(BuildSettings settings, GrailsBuildEventListener buildEventListener, boolean interactive) {
        this.buildSettings = settings;
        buildProps = buildSettings.getConfig().toProperties();
        grailsHome = buildSettings.getGrailsHome();

        metadataFile = new File(buildSettings.getBaseDir(), "application.properties");

        metadata = metadataFile.exists() ? Metadata.getInstance(metadataFile) : Metadata.getCurrent();

        metadataFile = metadata.getMetadataFile();
        enableProfile = Boolean.valueOf(getPropertyValue("grails.script.profile", false).toString());
        pluginsHome = buildSettings.getProjectPluginsDir().getPath();
        pluginSettings = new PluginBuildSettings(buildSettings);
        grailsAppName = metadata.getApplicationName();
        isInteractive = interactive;

        // If no app name property (upgraded/new/edited project) default to basedir.
        if (grailsAppName == null) {
            grailsAppName = buildSettings.getBaseDir().getName();
        }

        if (grailsAppName.indexOf('/') >-1) {
            appClassName = grailsAppName.substring(grailsAppName.lastIndexOf('/'), grailsAppName.length());
        }
        else {
            appClassName = GrailsNameUtils.getClassNameRepresentation(grailsAppName);
        }
        configSlurper = buildSettings.createConfigSlurper();
        configSlurper.setEnvironment(buildSettings.getGrailsEnv());
        this.buildEventListener = buildEventListener;
    }

    public void enableUaa() {
        if (UaaIntegration.isAvailable()) {
            UaaIntegration.enable(buildSettings, pluginSettings, isInteractive);
        }
    }

    public ConfigSlurper getConfigSlurper() {
        return configSlurper;
    }

    public Object getAppClassName() {
        return appClassName;
    }

    // server port options
    // these are legacy settings
    public int getServerPort() {
        int serverPort = Integer.valueOf(getPropertyValue("server.port", 8080).toString());
        serverPort = Integer.valueOf(getPropertyValue("grails.server.port.http", serverPort).toString());
        return serverPort;
    }

    public int getServerPortHttps() {
        int serverPortHttps = Integer.valueOf(getPropertyValue("server.port.https", 8443).toString());
        serverPortHttps = Integer.valueOf(getPropertyValue("grails.server.port.https", serverPortHttps).toString());
        return serverPortHttps;
    }

    public String getServerHost() {
        return (String)getPropertyValue("grails.server.host", null);
    }

    public String getGrailsAppName() { return grailsAppName; }
    public String getGrailsAppVersion() { return metadata.getApplicationVersion(); }
    public String getAppGrailsVersion() { return metadata.getGrailsVersion(); }
    public String getServletVersion() { return buildSettings.getServletVersion(); }

    public String getPluginsHome() {
        return pluginsHome;
    }

    public PluginBuildSettings getPluginBuildSettings() {
        return pluginSettings;
    }

    public PluginBuildSettings getPluginSettings() {
        return pluginSettings;
    }

    public BuildSettings getBuildSettings() {
        return buildSettings;
    }

    public Properties getBuildProps() {
        return buildProps;
    }

    public PathMatchingResourcePatternResolver getResolver() {
        return resolver;
    }

    public File getGrailsHome() {
        return grailsHome;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public File getMetadataFile() {
        return metadataFile;
    }

    public boolean isEnableProfile() {
        return enableProfile;
    }

    public boolean getIsInteractive() {
        return isInteractive;
    }

    public Resource[] resolveResources(String pattern) {
        try {
            return resolver.getResources(pattern);
        }
        catch (Exception e) {
            return NO_RESOURCES;
        }
    }

    /** Closure that returns a Spring Resource - either from $GRAILS_HOME
        if that is set, or from the classpath.*/
    public Resource grailsResource(String path) {
        if (grailsHome != null) {
            FileSystemResource resource = new FileSystemResource(grailsHome + "/" + path);
            if (!resource.exists()) {
                resource = new FileSystemResource(grailsHome + "/grails-resources/" + path);
            }
            return resource;
        }
        return new ClassPathResource(path);
    }

    /** Copies a Spring resource to the file system.*/
    public void copyGrailsResource(Object targetFile, Resource resource) throws FileNotFoundException, IOException {
        copyGrailsResource(targetFile, resource, true);
    }

    public void copyGrailsResource(Object targetFile, Resource resource, boolean overwrite) throws FileNotFoundException, IOException {
        File file = new File(targetFile.toString());
        if (overwrite || !file.exists()) {
            FileCopyUtils.copy(resource.getInputStream(), new FileOutputStream(file));
        }
    }

    public void copyGrailsResources(Object destDir, Object pattern) throws FileNotFoundException, IOException {
        copyGrailsResources(destDir, pattern, true);
    }
    // Copies a set of resources to a given directory. The set is specified
    // by an Ant-style path-matching pattern.
    public void copyGrailsResources(Object destDir, Object pattern, boolean overwrite) throws FileNotFoundException, IOException {
        new File(destDir.toString()).mkdirs();
        Resource[] resources = resolveResources("classpath:"+pattern);
        for (Resource resource : resources) {
            if (resource.isReadable()) {
                copyGrailsResource(destDir+"/"+resource.getFilename(),resource, overwrite);
            }
        }
    }

    /**
     * Resolves the value for a given property name. It first looks for a
     * system property, then in the BuildSettings configuration, and finally
     * uses the given default value if other options are exhausted.
     */
    public Object getPropertyValue(String propName, Object defaultValue) {
        // First check whether we have a system property with the given name.
        Object value = System.getProperty(propName);
        if (value != null) return value;

        // Now try the BuildSettings settings.
        value = buildProps.get(propName);

        // Return the BuildSettings value if there is one, otherwise use the default.
        return value != null ? value : defaultValue;
    }

    public void updateMetadata(@SuppressWarnings("hiding") Metadata metadata, @SuppressWarnings("rawtypes") Map entries) {
        for (Object key : entries.keySet()) {
            final Object value = entries.get(key);
            if (value != null) {
                metadata.put(key, value.toString());
            }
        }

        metadata.persist();
    }

    /**
     * Modifies the application's metadata, as stored in the "application.properties"
     * file. If it doesn't exist, the file is created.
     */
    public void updateMetadata(@SuppressWarnings("rawtypes") Map entries) {
        @SuppressWarnings("hiding") Metadata metadata = Metadata.getCurrent();
        updateMetadata(metadata, entries);
    }

    /**
     * Times the execution of a closure, which can include a target. For
     * example,
     *
     *   profile("compile", compile)
     *
     * where 'compile' is the target.
     */
    public void profile(String name, Closure<?> callable) {
        if (enableProfile) {
            long now = System.currentTimeMillis();
            GrailsConsole console = GrailsConsole.getInstance();
            console.addStatus("Profiling ["+name+"] start");

            callable.call();
            long then = System.currentTimeMillis() - now;
            console.addStatus("Profiling ["+name+"] finish. Took "+then+" ms");
        }
        else {
            callable.call();
        }
    }

    public String makeRelative(String path) {
        if (buildSettings != null && path != null) {
            String absolutePath = buildSettings.getBaseDir().getAbsolutePath();
            if (path.startsWith(absolutePath)) {
                return path.substring(absolutePath.length()+1);
            }
        }
        return path;
    }

    public String makeRelative(File file) {
        return makeRelative(file.getAbsolutePath());
    }

    /**
     * Exits the build immediately with a given exit code.
     */
   public void exit(int code){
       if(buildEventListener != null) {
           buildEventListener.triggerEvent("Exiting", code);
       }
       
        // Prevent system.exit during unit/integration testing
        if (System.getProperty("grails.cli.testing") != null || System.getProperty("grails.disable.exit") != null) {
            throw new ScriptExitException(code);
        }
        GrailsConsole.getInstance().flush();
        System.exit(code);       
    }

    /**
     * Interactive prompt that can be used by any part of the build. Echos
     * the given message to the console and waits for user input that matches
     * either 'y' or 'n'. Returns <code>true</code> if the user enters 'y',
     * <code>false</code> otherwise.
     */
    public boolean confirmInput(String message, String code ) {
        if(code == null) code = "confirm.message";
        GrailsConsole grailsConsole = GrailsConsole.getInstance();
        if (!isInteractive) {
            grailsConsole.error("Cannot ask for input when --non-interactive flag is passed. Please switch back to interactive mode.");
            exit(1);
        }
        return "y".equalsIgnoreCase(grailsConsole.userInput(message, new String[] { "y","n" }));
    }

    public boolean confirmInput(String message ) {
        return confirmInput(message, "confirm.message");
    }

    // Note: the following only work if you also include _GrailsEvents.
    public void logError( String message, Throwable t ) {
        GrailsConsole.getInstance().error(message, t);
    }

    public void logErrorAndExit( String message, Throwable t ) {
        logError(message, t);
        exit(1);
    }
    
}
