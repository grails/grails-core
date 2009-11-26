/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.resolve;

import grails.util.BuildSettings;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.ivy.core.settings.IvySettings;

import java.io.File;

/**
 * A resolver that resolves JAR files from plugin lib directories
 *
 * @author Graeme Rocher
 * @since 1.2
 */
public class GrailsPluginsDirectoryResolver extends FileSystemResolver{
    private static final String GRAILS_PLUGINS = "grailsPlugins";
    private static final String LIB_DIR_PATTERN = "/lib/[artifact]-[revision].[ext]";

    public GrailsPluginsDirectoryResolver(BuildSettings buildSettings, IvySettings ivySettings) {
        final File pluginsDir = buildSettings.getProjectPluginsDir();
        final File basedir = buildSettings.getBaseDir();
        if(basedir!=null) {
            addArtifactPattern(basedir.getAbsolutePath()+ LIB_DIR_PATTERN);
        }
        addPatternsForPluginsDirectory(pluginsDir);
        addPatternsForPluginsDirectory(buildSettings.getGlobalPluginsDir());
        setName(GRAILS_PLUGINS);
        setSettings(ivySettings);
    }

    private void addPatternsForPluginsDirectory(File pluginsDir) {
        if(pluginsDir!=null) {
            final File[] files = pluginsDir.listFiles();
            if(files!=null) {
                for(File f : files) {
                    if(f.isDirectory()) {
                        addArtifactPattern(f.getAbsolutePath()+ LIB_DIR_PATTERN);
                    }
                }
            }
        }
    }
}
