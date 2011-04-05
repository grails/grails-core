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

import grails.util.BuildSettings;
import grails.util.GrailsNameUtils;
import grails.util.Metadata;
import grails.util.PluginBuildSettings;
import groovy.lang.Closure;
import groovy.util.ConfigSlurper;
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.uaa.client.UaaService;
import org.springframework.uaa.client.UaaServiceFactory;
import org.springframework.uaa.client.VersionHelper;
import org.springframework.uaa.client.protobuf.UaaClient;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.util.Map;
import java.util.Properties;

/**
 * Utility methods used on the command line
 *
 * @author Graeme Rocher
 */
public class BaseSettingsApi {

    private static final String MESSAGE = "Grails wants to send information to VMware domains to improve your experience. We include anonymous usage information as part of these downloads.\n"
           + "\n"
           + "The Grails team gathers anonymous usage information to improve your Grails experience, not for marketing purposes. We also use this information to help guide our roadmap, prioritizing the features and Grails plugins most valued by the community and enabling us to optimize the compatibility of technologies frequently used together.\n"
           + "\n"
           + "Please see the Grails User Agent Analysis (UAA) Terms of Use at http://www.springsource.org/uaa/terms_of_use for more information on what information is collected and how such information is used. There is also an FAQ at http://www.springsource.org/uaa/faq for your convenience.\n"
           + "\n"
           + "To consent to the Terms of Use, please enter 'Y'. Enter 'N' to indicate your do not consent and anonymous data collection will remain disabled.\n"
           + "Enter Y or N:";

    private static final Resource[] NO_RESOURCES = new Resource[0];
    private BuildSettings buildSettings;
    private Properties buildProps;
    private PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private File grailsHome;
    private Metadata metadata;
    private File metadataFile;
    private boolean enableProfile;
    private boolean isInteractive;
    private String pluginsHome;
    private PluginBuildSettings pluginSettings;
    private String grailsAppName;
    private Object appClassName;
    private ConfigSlurper configSlurper;
    private UaaService uaaService;

    public BaseSettingsApi(final BuildSettings buildSettings, boolean interactive) {
        this.buildSettings = buildSettings;
        this.buildProps = buildSettings.getConfig().toProperties();
        this.grailsHome = buildSettings.getGrailsHome();

        metadataFile = new File(buildSettings.getBaseDir()+"/application.properties");

        metadata = metadataFile.exists() ? Metadata.getInstance(metadataFile) : Metadata.getCurrent();

        this.metadataFile = metadata.getMetadataFile();
        this.enableProfile = Boolean.valueOf(getPropertyValue("grails.script.profile", false).toString());
        this.pluginsHome = buildSettings.getProjectPluginsDir().getPath();
        this.pluginSettings = new PluginBuildSettings(buildSettings);
        this.grailsAppName = metadata.getApplicationName();
        this.isInteractive = interactive;

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
        this.configSlurper = buildSettings.createConfigSlurper();
        this.configSlurper.setEnvironment(buildSettings.getGrailsEnv());

        this.uaaService = UaaServiceFactory.getUaaService();

        final UaaClient.Privacy.PrivacyLevel privacyLevel = uaaService.getPrivacyLevel();
        if(!uaaService.isUaaTermsOfUseAccepted() && isInteractive()) {
            // prompt for UAA choice
            if(privacyLevel.equals( UaaClient.Privacy.PrivacyLevel.UNDECIDED_TOU )) {
                while (true) {
                    System.out.print(MESSAGE);
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    try {
                        String selection = br.readLine().trim();
                        if("y".equalsIgnoreCase(selection)) {
                            uaaService.setPrivacyLevel(UaaClient.Privacy.PrivacyLevel.ENABLE_UAA);
                        }
                        else if("n".equalsIgnoreCase(selection)) {
                            uaaService.setPrivacyLevel(UaaClient.Privacy.PrivacyLevel.DECLINE_TOU);
                        }
                    } catch (IOException e) {
                        break;
                    }

                }
            }
        }
        else if(isUaaAccepted(privacyLevel)) {
            Thread uaaThread = new Thread(new Runnable() {
                public void run() {
                    final UaaClient.Product product = VersionHelper.getProduct("Grails", buildSettings.getGrailsVersion());
                    uaaService.registerProductUsage(product);
                    final GrailsPluginInfo[] pluginInfos = pluginSettings.getPluginInfos();
                    for (GrailsPluginInfo pluginInfo : pluginInfos) {
                        uaaService.registerFeatureUsage(product, VersionHelper.getFeatureUse(pluginInfo.getName(), pluginInfo.getVersion()));
                    }
                }
            });
            uaaThread.setDaemon(true);
            uaaThread.start();
        }
    }

    private boolean isUaaAccepted(UaaClient.Privacy.PrivacyLevel privacyLevel) {
        return privacyLevel.equals( UaaClient.Privacy.PrivacyLevel.ENABLE_UAA ) || privacyLevel.equals( UaaClient.Privacy.PrivacyLevel.ENABLE_UAA );
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
        return Integer.valueOf(getPropertyValue("grails.server.port.http", 8080).toString());
    }

    public int getServerPortHttps() {
        return Integer.valueOf(getPropertyValue("grails.server.port.https", 8443).toString());
    }

    public String getServerHost() {
        return (String)getPropertyValue("grails.server.host", null);
    }

    public String getGrailsAppName() { return this.grailsAppName; }
    public String getGrailsAppVersion() { return metadata.getApplicationVersion(); }
    public String getAppGrailsVersion() { return metadata.getGrailsVersion(); }
    public String getServletVersion() { return metadata.getServletVersion() != null ? metadata.getServletVersion() : "2.5"; }

    public String getPluginsHome() {
        return this.pluginsHome;
    }

    public PluginBuildSettings getPluginBuildSettings() {
        return this.pluginSettings;
    }

    public PluginBuildSettings getPluginSettings() {
        return this.pluginSettings;
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

    public boolean isInteractive() {
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

    public void copyGrailsResource(Object targetFile, Resource resource, boolean overwrite ) throws FileNotFoundException, IOException {
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
    public Object getPropertyValue( String propName, Object defaultValue ) {
        // First check whether we have a system property with the given name.
        Object value = System.getProperty(propName);
        if (value != null) return value;

        // Now try the BuildSettings settings.
        value = buildProps.get(propName);

        // Return the BuildSettings value if there is one, otherwise use the default.
        return value != null ? value : defaultValue;
    }

    /**
     * Modifies the application's metadata, as stored in the "application.properties"
     * file. If it doesn't exist, the file is created.
     */
    public void updateMetadata(@SuppressWarnings("rawtypes") Map entries) {
        @SuppressWarnings("hiding") Metadata metadata = Metadata.getCurrent();
        for (Object key : entries.keySet()) {
            final Object value = entries.get(key);
            if (value != null) {
                metadata.put(key, value.toString());
            }
        }

        metadata.persist();
    }

    /**
     * Times the execution of a closure, which can include a target. For
     * example,
     *
     *   profile("compile", compile)
     *
     * where 'compile' is the target.
     */
    public void profile(String name, Closure<?> callable ) {
        if (enableProfile) {
            long now = System.currentTimeMillis();
            System.out.println("Profiling ["+name+"] start");

            callable.call();
            long then = System.currentTimeMillis() - now;
            System.out.println("Profiling ["+name+"] finish. Took "+then+" ms");
        }
        else {
            callable.call();
        }
    }
}
