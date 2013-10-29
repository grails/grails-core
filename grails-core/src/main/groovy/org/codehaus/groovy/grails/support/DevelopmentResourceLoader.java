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
package org.codehaus.groovy.grails.support;

import grails.util.BuildSettings;
import grails.util.BuildSettingsHolder;
import grails.util.Metadata;
import grails.util.PluginBuildSettings;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * Allows files references like /WEB-INF/grails-app to be loaded from ./grails-app to support
 * the difference between wAR deployment and run-app.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class DevelopmentResourceLoader extends DefaultResourceLoader {

    private static final Log LOG = LogFactory.getLog(DevelopmentResourceLoader.class);

    private String baseLocation = ".";
    private GrailsApplication application;
    private static final String PLUGINS_PREFIX = "plugins/";
    private static final String SLASH = "/";
    private static final Pattern HAS_SCHEME_PATTERN = Pattern.compile("[^:/?#]+:.*");
    private static final String GRAILS_APP_DIR_PATTERN = "/grails-app/.*";

    public DevelopmentResourceLoader(GrailsApplication application) {
        this.application = application;
    }

    public DevelopmentResourceLoader() {
        super();
    }

    public DevelopmentResourceLoader(GrailsApplication application, String baseLocation) {
        this(application);
        this.baseLocation = baseLocation;
    }

    @Override
    public Resource getResource(String location) {
        if (Metadata.getCurrent().isWarDeployed()) {
            return super.getResource(location);
        }

        location = getRealLocationInProject(location);
        return super.getResource(location);
    }

    /**
     * Retrieves the real location of a GSP within a Grails project.
     * @param location The location of the GSP at deployment time
     * @return The location of the GSP at development time
     */
    protected String getRealLocationInProject(String location) {

        if(new File(location).exists()) {
            return "file:" + location;
        }
        // don't mess with locations that are URLs (in other words, locations that have schemes)
        if(HAS_SCHEME_PATTERN.matcher(location).matches()) return location;
        if (!location.startsWith(SLASH)) location = SLASH + location;

        // don't mess with locations that are URLs (in other words, locations that have schemes)
        if (HAS_SCHEME_PATTERN.matcher(location).matches()) return location;

        // If the location (minus the "grails-app/.*" ending so that it matches the key value used in BuildSettings for
        // the inline plugin map) matches an "inline" plugin, use the location as-is
        // for the resource location.  Otherwise, perform the logic to "normalize" the resource location based on
        // its relativity to the application (i.e. is it from a non-inline plugin, etc).
        if (BuildSettingsHolder.getSettings().isInlinePluginLocation(new File(location.replaceAll(GRAILS_APP_DIR_PATTERN, "")))) {
            return "file:" + location;
        }

        if (!location.startsWith(GrailsResourceUtils.WEB_INF)) {
            return GrailsResourceUtils.WEB_APP_DIR+location;
        }

        final String noWebInf = location.substring(GrailsResourceUtils.WEB_INF.length() + 1);
        final String defaultPath = "file:" + baseLocation + SLASH + noWebInf;
        if (!noWebInf.startsWith(PLUGINS_PREFIX)) {
            return defaultPath;
        }

        if (application != null) {

            BuildSettings settings = BuildSettingsHolder.getSettings();
            PluginBuildSettings pluginBuildSettings = org.codehaus.groovy.grails.plugins.GrailsPluginUtils.getPluginBuildSettings();
            String pluginPath = StringUtils.substringAfter(noWebInf, SLASH);
            String pluginName = StringUtils.substringBefore(pluginPath, SLASH);
            String remainingPath = StringUtils.substringAfter(pluginPath, SLASH);
            org.codehaus.groovy.grails.io.support.Resource r = pluginBuildSettings.getPluginDirForName(pluginName);
            if (r != null) {
                try {
                    return "file:" + r.getFile().getAbsolutePath() + SLASH + remainingPath;
                }
                catch (IOException e) {
                    LOG.debug("Unable to locate plugin resource -- returning default path " + defaultPath + ".", e);
                    return defaultPath;
                }
            }

            if (settings != null) {
                return "file:" + settings.getProjectPluginsDir().getAbsolutePath() + SLASH + pluginName + SLASH + remainingPath;
            }
        }

        return defaultPath;
    }
}
