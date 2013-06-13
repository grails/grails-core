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

import grails.util.BuildSettings
import grails.util.Metadata
import grails.util.PluginBuildSettings

import org.codehaus.groovy.grails.cli.api.BaseSettingsApi
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import org.codehaus.groovy.grails.resolve.Dependency
import org.codehaus.groovy.grails.resolve.DependencyManager

/**
 * Generates a POM for a Grails application.
 *
 * @author Graeme Rocher
 * @since 2.1
 */
class MavenPomGenerator extends BaseSettingsApi {

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
        addDependenciesForScope(dependencyManager, "build", plugins, "<type>zip</type>", "provided")

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
        if (!buildSettings.isPluginProject()) {
            return metadata.getApplicationVersion()
        }

        def pluginSettings = GrailsPluginUtils.getPluginBuildSettings(buildSettings)
        final info = pluginSettings.getPluginInfo(buildSettings.getBaseDir().absolutePath)
        return info.version
    }

    protected void addDependenciesForScope(DependencyManager dependencyManager, String scope, ArrayList<String> dependencies, String type = "", String newScope = null) {
        Collection<Dependency> appDependencies = type ? dependencyManager.getPluginDependencies(scope) : dependencyManager.getApplicationDependencies(scope)
        dependencies.addAll(appDependencies.collect {  Dependency dd  ->
            """
    <dependency>
        <groupId>$dd.group</groupId>
        <artifactId>$dd.name</artifactId>
        <version>$dd.version</version>
        <scope>${ newScope ?: scope }</scope>
        $type
    </dependency>
                    """.toString()
        })
    }
}
