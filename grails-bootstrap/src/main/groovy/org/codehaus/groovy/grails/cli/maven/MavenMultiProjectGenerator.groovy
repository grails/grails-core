/*
 * Copyright 2012 SpringSource
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
package org.codehaus.groovy.grails.cli.maven

import org.codehaus.groovy.grails.cli.api.BaseSettingsApi
import grails.util.BuildSettings
import org.codehaus.groovy.grails.plugins.AstPluginDescriptorReader
import grails.util.Metadata
import org.codehaus.groovy.grails.io.support.FileSystemResource

/**
 * Generates a Maven multi-module build structure for a Grails project and plugins
 *
 * @author Graeme Rocher
 * @author Peter Ledbrook
 * @since 2.1
 */
class MavenMultiProjectGenerator extends BaseSettingsApi{
    MavenPomGenerator pomGenerator

    MavenMultiProjectGenerator(BuildSettings buildSettings) {
        super(buildSettings, false)
        pomGenerator = new MavenPomGenerator(buildSettings)
    }

    /**
     * <p>Looks in the build's base directory for Grails plugin and application
     * directories and then creates a parent POM in the base directory plus
     * child POMs in each of the plugin and application folders. The parent
     * POM just contains the modules, i.e. the plugins and apps.</p>
     * <p>This method delegates to {@link MavenPomGenerator#generatePom(java.io.File, java.lang.String, java.util.Map)}
     * for the child POM generation.</p>
     * @param group The string to use for the parent and child POMs' <tt>groupId</tt>
     * element.
     * @param name The string to use for the parent and child POMs' <tt>artifactId</tt>,
     * <tt>name</tt> and <tt>description</tt> elements.
     * @param version The version string for the POMs.
     */
    void generate(String group, String name, String version) {
        // Generate the parent POM first.
        def baseDir = buildSettings.baseDir
        def plugins = baseDir.listFiles({ File f -> f.directory && !f.hidden && isPluginDir(f) } as FileFilter)
        def apps  = baseDir.listFiles({ File f -> f.directory && !f.hidden && isAppDir(f)} as FileFilter)
        def moduleNames = (plugins + apps)*.name

        def parentModel = [group: group, name: name, version: version]
        pomGenerator.generatePom(
            baseDir,
            "src/grails/templates/maven/parent.pom",
            parentModel + [modules: moduleNames])

        def dependentPlugins = []
        for (File pluginDir in plugins) {
            dependentPlugins << pomGenerator.generate(group, pluginDir, [parent: parentModel])
        }

        for (File appDir in apps) {
            pomGenerator.generate(group, appDir, [parent: parentModel, plugins: dependentPlugins])
        }
    }

    /**
     * Returns {@code true} if the given directory contains a Grails application
     * (not a plugin). This is based on whether <tt>dir</tt> contains a <tt>grails-app</tt>
     * sub-directory or not.
     */
    private boolean isAppDir(File dir) {
        new File(dir, "grails-app").exists() && !isPluginDir(dir)
    }

    /**
     * Returns {@code true} if the given directory contains a Grails plugin,
     * i.e. it has a plugin descriptor file.
     */
    private boolean isPluginDir(File dir) {
        dir.listFiles({ File p, String name -> name.endsWith("GrailsPlugin.groovy")} as FilenameFilter)
    }
}
