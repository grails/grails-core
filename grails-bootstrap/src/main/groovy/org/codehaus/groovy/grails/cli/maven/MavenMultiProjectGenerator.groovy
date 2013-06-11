/*
 * Copyright 2012 GoPivotal, Inc. All Rights Reserved
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
 * @since 2.1
 */
class MavenMultiProjectGenerator extends BaseSettingsApi{
    MavenMultiProjectGenerator(BuildSettings buildSettings) {
        super(buildSettings, false)
    }

    void generate(String group, String name, String version) {
        def rootTemplate = grailsResource("src/grails/templates/maven/parent.pom")

        def baseDir = buildSettings.baseDir
        def parentPom = new File(baseDir, "pom.xml")
        copyGrailsResource(parentPom, rootTemplate)
        def ant = new AntBuilder()

        List<File> allModules = baseDir.listFiles().findAll { File dir ->
            dir.isDirectory() && !dir.isHidden() && (new File(dir, 'grails-app').exists() || dir.listFiles().find { File f -> f.name.endsWith("GrailsPlugin.groovy")})
        }

        def moduleNames = allModules.collect() { File f -> f.name }

        def plugins = allModules.findAll() { File dir -> dir.listFiles().find { File f -> f.name.endsWith("GrailsPlugin.groovy")} }
        def apps  = allModules.findAll() { File dir -> !dir.listFiles().find { File f -> f.name.endsWith("GrailsPlugin.groovy")} }

        def reader = new AstPluginDescriptorReader()
        def binaryPlugins = []
        def sourcePlugins = []
        for (File pluginDir in plugins) {
            def descriptor = pluginDir.listFiles().find { it.name.endsWith("GrailsPlugin.groovy")}
            def info = reader.readPluginInfo(new FileSystemResource(descriptor))
            def packaging = info.packaging ?: "source"
            def isBinary = "binary" == packaging
            def template = isBinary ? grailsResource("src/grails/templates/maven/binary-plugin.pom") :  grailsResource("src/grails/templates/maven/plugin.pom")
            def pluginPom = new File(pluginDir, "pom.xml")
            if (!pluginPom.exists()) {
                copyGrailsResource(pluginPom, template)
                def pluginGroup = info.group ?: "org.grails.plugins"
                ant.replace(file:pluginPom) {
                    replacefilter token:"@parent.group@", value:group
                    replacefilter token:"@group@", value:pluginGroup
                    replacefilter token:"@parent@", value:name
                    replacefilter token:"@parent.version@", value:version
                    replacefilter token:"@grailsVersion@", value:buildSettings.grailsVersion
                    replacefilter token:"@name@", value:info.name
                    replacefilter token:"@version@", value:info.version

                }

                (isBinary ? binaryPlugins : sourcePlugins ) << [group:pluginGroup, name: info.name, version:info.version]
            }
        }

        for (File appDir in apps) {
            def template = grailsResource("src/grails/templates/maven/app.pom")
            def appPom = new File(appDir, "pom.xml")
            if (!appPom.exists()) {
                copyGrailsResource(appPom, template)

                def appProps = new File(appDir, "application.properties")
                def props = Metadata.getInstance(appProps)

                List<String> dependencies = binaryPlugins.collect() {
                    """
    <dependency>
        <groupId>$it.group</groupId>
        <artifactId>$it.name</artifactId>
        <version>$it.version</version>
    </dependency>
                    """
                }
                dependencies.addAll( sourcePlugins.collect {
"""
    <dependency>
        <groupId>$it.group</groupId>
        <artifactId>$it.name</artifactId>
        <version>$it.version</version>
        <type>zip</type>
        <scope>compile</scope>
    </dependency>

""".toString()
                })

                ant.replace(file:appPom) {
                    replacefilter token:"@parent.group@", value:group
                    replacefilter token:"@group@", value:group
                    replacefilter token:"@parent@", value:name
                    replacefilter token:"@parent.version@", value:version
                    replacefilter token:"@grailsVersion@", value:buildSettings.grailsVersion
                    replacefilter token:"@name@", value:props.getApplicationName()
                    replacefilter token:"@version@", value:props.getApplicationVersion()
                    replacefilter token:"@plugins@", value: dependencies.join('')
                }
            }
        }

        ant.replace(file:parentPom) {
            replacefilter token:"@group@", value:group
            replacefilter token:"@name@", value:name
            replacefilter token:"@version@", value:version
            replacefilter token:"@modules@", value:moduleNames.collect { "<module>$it</module>"}.join(System.getProperty("line.separator"))
        }
    }
}
