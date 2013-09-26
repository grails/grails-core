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

import groovy.text.SimpleTemplateEngine

import org.codehaus.groovy.grails.cli.api.BaseSettingsApi
import org.codehaus.groovy.grails.io.support.FileSystemResource
import org.codehaus.groovy.grails.plugins.AstPluginDescriptorReader
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo
import org.codehaus.groovy.grails.resolve.Dependency
import org.codehaus.groovy.grails.resolve.DependencyManager

/**
 * Generates a POM for a Grails application.
 *
 * @author Graeme Rocher
 * @author Peter Ledbrook
 * @since 2.1
 */
class MavenPomGenerator extends BaseSettingsApi {
    /**
     * The <em>grails-binary-plugin</em> packaging equates to a {@code null} type,
     * i.e. the default 'jar' type.
     */
    private static Map packagingToTypeMap = ["grails-app": "war", "grails-plugin": "zip"]

    MavenPomGenerator(BuildSettings buildSettings) {
        super(buildSettings, false)
    }

    /**
     * Creates a <tt>pom.xml</tt> file for the current Grails project, i.e. the
     * one pointed to by the build's base directory. The generated POM will include
     * the JAR and plugin dependencies declared in <tt>BuildConfig.groovy</tt>.
     * @param group The string to use for the POM's <tt>groupId</tt> element.
     */
    void generate(String group) {
        generatePom(
            buildSettings.baseDir,
            "src/grails/templates/maven/project.pom",
            createModel(group, Metadata.getCurrent(), getPluginInfo(buildSettings.baseDir)))
    }

    /**
     * This does the same as {@link #generate(java.lang.String)}, but the generated
     * POM includes a <tt>parent</tt> element containing the details of whatever POM
     * is found in the current project's parent directory. The current project is
     * defined as the one in the build's base directory.
     * @param group The string to use for the POM's <tt>groupId</tt> element.
     * @throws FileNotFoundException if a POM cannot be found in the current
     * project's parent directory.
     */
    void generateWithParent(String group) {
        def projDir = buildSettings.baseDir
        def parentPomFile = new File(projDir.parentFile, "pom.xml")

        if (!parentPomFile.exists()) {
            throw new FileNotFoundException("No POM found in the parent directory of this project")
        }

        def baseModel = createModel(group, Metadata.getCurrent(), getPluginInfo(projDir))

        generatePom(
            projDir,
            "src/grails/templates/maven/project.pom",
            baseModel + getParentModel(parentPomFile))
    }

    /**
     * Creates a <tt>pom.xml</tt> file for the Grails project in the given
     * directory. It not only allows you to provide a value for the <tt>groupId</tt>,
     * but you can also provide additional values for the POM template.
     * @param group The string to use for the POM's <tt>groupId</tt> element.
     * @param projDir The directory containing the Grails project you want to
     * create a POM for. It must exist!
     * @param addedModel A map of additional values for the template. Supported
     * variables are:
     * <ul>
     *     <li>parent - with <tt>group</tt>, <tt>name</tt> and <tt>version</tt> sub-keys</li>
     *     <li>group - the <tt>groupId</tt> string</li>
     *     <li>name - the <tt>artifactId</tt> string</li>
     *     <li>packaging - the <tt>packaging</tt> string, e.g. 'zip'</li>
     *     <li>version - the <tt>versions</tt> string</li>
     *     <li>dependencies - a list of JAR {@link DependencyInfo} instances that will be
     *     merged with the ones derived from <tt>BuildConfig.groovy</tt></li>
     *     <li>plugins - a list of plugin {@link DependencyInfo} instances that will be
     *     merged with the ones derived from <tt>BuildConfig.groovy</tt></li>
     * </ul>
     */
    DependencyInfo generate(String group, File projDir, Map addedModel) {
        def dependencies = addedModel.remove("dependencies")
        def plugins = addedModel.remove("plugins")
        def model = createModel(group, projDir) + addedModel

        if (dependencies) model["dependencies"] = dependencies + (model["dependencies"] ?: [])
        if (plugins) model["plugins"] = plugins + (model["plugins"] ?: [])

        generatePom projDir, "src/grails/templates/maven/project.pom", model

        return new DependencyInfo(
            group: model.group,
            name: model.name,
            version: model.version,
            scope: "compile",
            type: packagingToTypeMap[model.packaging])
    }

    /**
     * Creates a <tt>pom.xml</tt> file in the target directory using the template
     * located at the given path combined with the given model. The template path
     * is resolved as a Grails resource (which may be in the grails-resources JAR
     * or in <tt>$GRAILS_HOME</tt> on the filesystem).
     * @param projDir The target directory to put the POM.
     * @param templatePath A relative Grails resource path to the template file to
     * use. The template must conform to Groovy's <tt>SimpleTemplateEngine</tt>
     * syntax.
     * @param model A map of variables and values that will be substituted into the
     * template.
     */
    void generatePom(File projDir, String templatePath, Map model) {
        final pomFile = new File(projDir, "pom.xml")
        copyGrailsResource(pomFile, grailsResource(templatePath))

        def templateEngine = new SimpleTemplateEngine()
        def tmpl = templateEngine.createTemplate(pomFile.getText("UTF-8")).make(model)
        tmpl.writeTo(pomFile.newWriter("UTF-8"))
    }

    /**
     * Creates a standard template model based on the target directory containing
     * a Grails project. The method will pick out the project's name, version, and
     * dependencies from the appropriate files. It works for both application and
     * plugin projects.
     * @param group This is used to populate the model with the variable used for
     * the POM's <tt>groupId</tt> element.
     * @param projDir The directory containing the Grails project of interest.
     * @return A standard template model.
     */
    protected Map createModel(String group, File projDir) {
        return createModel(
            group,
            Metadata.getInstance(new File(projDir, "application.properties")),
            getPluginInfo(projDir))
    }

    protected Map createModel(String group, Metadata projInfo, GrailsPluginInfo pluginInfo = null) {
        // Check that Grails version in metadata matches the Grails version in
        // the build settings. A mismatch (only likely when generating a multi-project
        // build) will almost certainly cause build problems as the Maven plugin
        // is attuned to a specific version of Grails.
        if (projInfo.getGrailsVersion() != buildSettings.grailsVersion) {
            throw new IllegalStateException(
                "When generating a POM, the version of Grails used to create the POM" +
                " should match the version of Grails for the project [Project ${projInfo.getApplicationName() ?: pluginInfo?.name}]")
        }

        def dependencyManager = buildSettings.dependencyManager
        def dependencies = []
        dependencies.addAll getDependenciesForScope(dependencyManager, "compile")
        dependencies.addAll getDependenciesForScope(dependencyManager, "runtime")
        dependencies.addAll getDependenciesForScope(dependencyManager, "test")
        dependencies.addAll getDependenciesForScope(dependencyManager, "provided")
        dependencies.addAll getDependenciesForScope(dependencyManager, "build", "", "provided")

        def plugins = []
        plugins.addAll getDependenciesForScope(dependencyManager, "compile", "zip")
        plugins.addAll getDependenciesForScope(dependencyManager, "runtime", "zip")
        plugins.addAll getDependenciesForScope(dependencyManager, "test", "zip")
        plugins.addAll getDependenciesForScope(dependencyManager, "provided", "zip")
        plugins.addAll getDependenciesForScope(dependencyManager, "build", "zip", "provided")

        def packaging = "grails-app"
        if (pluginInfo) {
            packaging = pluginInfo.packaging == "binary" ? "grails-binary-plugin" : "grails-plugin"
        }

        return [
            grailsVersion: buildSettings.grailsVersion,
            group: group,
            name: pluginInfo?.name ?: projInfo.getApplicationName(),
            packaging: packaging,
            version: readVersion(projInfo, pluginInfo),
            dependencies: dependencies,
            plugins: plugins,
            pluginProject: pluginInfo != null ]
    }

    protected Map getParentModel(File pomFile) {
        def xml = new XmlSlurper().parse(pomFile)
        return [parent: [group: xml.groupId.text(), name: xml.artifactId.text(), version: xml.version.text()]]
    }

    private String readVersion(Metadata metadata, GrailsPluginInfo pluginInfo) {
        if (!pluginInfo) {
            return metadata.getApplicationVersion()
        }

        return pluginInfo.version
    }

    /**
     * Reads the plugin information from the plugin descriptor in the given
     * directory. If no plugin descriptor is found, this method will return
     * {@code null}.
     */
    private GrailsPluginInfo getPluginInfo(File pluginDir) {
        def reader = new AstPluginDescriptorReader()
        def descriptor = pluginDir.listFiles({ File f -> f.name.endsWith("GrailsPlugin.groovy")} as FileFilter)

        if (!descriptor) return null

        return reader.readPluginInfo(new FileSystemResource(descriptor[0]))
    }

    /**
     * Returns a list of plugin or application dependencies stored in the given
     * dependency manager.
     * @param scope The dependency scope you're interested in, e.g. 'compile',
     * 'provided', etc.
     * @param type The type of the dependencies you want. This can by {@code null}
     * or an empty string, both of which imply you want just JAR dependencies. Any
     * other type is assumed to mean you want the plugin dependencies. The returned
     * {@link DependencyInfo} instances will use the type you specify here.
     * @param newScope If specified, the returned {@link DependencyInfo} instances
     * will use this for the <tt>scope</tt> property. Otherwise, they will use the
     * value from the <tt>scope</tt> argument.
     * @return A list of {@link DependencyInfo} instances for either the resolved
     * JAR dependencies or plugin dependencies.
     */
    protected List<DependencyInfo> getDependenciesForScope(
            DependencyManager dependencyManager,
            String scope,
            String type = "",
            String newScope = null) {
        def deps = type ? dependencyManager.getPluginDependencies(scope) : dependencyManager.getApplicationDependencies(scope)
        return deps.findAll { it.exported }.collect { Dependency dd ->
            new DependencyInfo(
                group: dd.group,
                name: dd.name,
                version: dd.version,
                scope: newScope ?: scope,
                type: type)
        }
    }
}

class DependencyInfo {
    String group
    String name
    String version
    String scope
    String type
}
