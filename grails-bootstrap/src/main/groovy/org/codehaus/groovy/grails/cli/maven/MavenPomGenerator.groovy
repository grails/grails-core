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
import grails.util.Metadata
import org.codehaus.groovy.grails.resolve.EnhancedDefaultDependencyDescriptor
import org.apache.ivy.core.module.descriptor.DependencyDescriptor
import org.codehaus.groovy.grails.resolve.IvyDependencyManager
import grails.util.PluginBuildSettings

/**
 * Generates a POM for a Grails application
 * 
 * @author Graeme Rocher
 * @since 2.1
 */
class MavenPomGenerator extends BaseSettingsApi{
    MavenPomGenerator(BuildSettings buildSettings) {
        super(buildSettings, false)
    }

    void generate(String group) {

        def rootTemplate = buildSettings.isPluginProject() ?
                                grailsResource("src/grails/templates/maven/plugin-single.pom") :
                                grailsResource("src/grails/templates/maven/single.pom")

        def baseDir = buildSettings.baseDir

        final metadata = Metadata.getCurrent()
        def name = metadata.getApplicationName()
        def version = readVersion(buildSettings, metadata)

        final pomFile = new File(baseDir, "pom.xml")
        copyGrailsResource(pomFile,rootTemplate)

        def dependencyManager = buildSettings.dependencyManager
        List<String> dependencies = []
        addDependenciesForScope(dependencyManager, "compile", dependencies)
        addDependenciesForScope(dependencyManager, "runtime", dependencies)
        addDependenciesForScope(dependencyManager, "test", dependencies)
        addDependenciesForScope(dependencyManager, "provided", dependencies)
        addDependenciesForScope(dependencyManager, "build", dependencies, "", "provided")
        List<String> plugins = []
        addDependenciesForScope(dependencyManager, "compile", plugins, "<type>zip</type>")
        addDependenciesForScope(dependencyManager, "runtime", plugins, "<type>zip</type>")
        addDependenciesForScope(dependencyManager, "test", plugins, "<type>zip</type>")
        addDependenciesForScope(dependencyManager, "provided", plugins, "<type>zip</type>")


        def ant = new AntBuilder()
        ant.replace(file:pomFile) {
            replacefilter token:"@grailsVersion@", value:buildSettings.grailsVersion
            replacefilter token:"@group@", value:group
            replacefilter token:"@name@", value:name
            replacefilter token:"@version@", value:version
            replacefilter token:"@dependencies@", value:dependencies.join(System.getProperty("line.separator"))
            replacefilter token:"@plugins@", value:plugins.join(System.getProperty("line.separator"))

        }
    }

    private String readVersion(BuildSettings buildSettings, metadata) {
        if(buildSettings.isPluginProject()) {
            def pluginSettings = new PluginBuildSettings(buildSettings)
            final info = pluginSettings.getPluginInfo(buildSettings.getBaseDir().absolutePath)
            return info.version
        }
        else {
            return metadata.getApplicationVersion()
        }
    }

    def addDependenciesForScope(IvyDependencyManager dependencyManager, String scope, ArrayList<String> dependencies, String type = "", String newScope=scope) {
        final appDependencies = type ? dependencyManager.pluginDependencyDescriptors : dependencyManager.getApplicationDependencyDescriptors(scope)
        dependencies.addAll(appDependencies.findAll {  EnhancedDefaultDependencyDescriptor dd -> dd.scope == scope }.collect() {  EnhancedDefaultDependencyDescriptor dd ->
            """
    <dependency>
        <groupId>$dd.dependencyId.organisation</groupId>
        <artifactId>$dd.dependencyId.name</artifactId>
        <version>$dd.dependencyRevisionId.revision</version>
        <scope>runtime</scope>
        $type
    </dependency>
                    """.toString()
        })
    }
}
