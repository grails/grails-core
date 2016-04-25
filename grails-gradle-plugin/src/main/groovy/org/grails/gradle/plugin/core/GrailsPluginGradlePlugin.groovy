/*
 * Copyright 2015 original authors
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
package org.grails.gradle.plugin.core

import groovy.transform.CompileDynamic
import grails.util.Environment
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.internal.tasks.DefaultTaskDependency
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskDependency
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.grails.gradle.plugin.util.SourceSets

import javax.inject.Inject


/**
 * A Gradle plugin for Grails plugins
 *
 * @author Graeme Rocher
 * @since 3.0
 *
 */
class GrailsPluginGradlePlugin extends GrailsGradlePlugin {

    @Inject
    GrailsPluginGradlePlugin(ToolingModelBuilderRegistry registry) {
        super(registry)
    }

    @Override
    @CompileStatic
    void apply(Project project) {
        super.apply(project)

        checkForConfigurationClash(project)

        configureAstSources(project)

        configureProjectNameAndVersionASTMetadata(project)

        configurePluginResources(project)

        configurePluginJarTask(project)

        configureSourcesJarTask(project)

        configureExplodedDirConfiguration(project)

    }

    @CompileDynamic
    @Override
    void addDefaultProfile(Project project, Configuration profileConfig) {
        project.dependencies {
            profile  ":${System.getProperty("grails.profile") ?: 'web-plugin'}:"
        }
    }

    /**
     * Configures an exploded configuration that can be used to build the classpath of the application from subprojects that are plugins without contructing a JAR file
     *
     * @param project The project instance
     */
    @CompileStatic
    protected void configureExplodedDirConfiguration(Project project) {

        def allConfigurations = project.configurations

        def runtimeConfiguration = allConfigurations.findByName('runtime')
        if(Environment.isDevelopmentRun()) {
            def explodedConfig = allConfigurations.create('exploded')
            runtimeConfiguration.artifacts.clear()
            explodedConfig.extendsFrom(runtimeConfiguration)
            // add the subproject classes as outputs
            def allTasks = project.tasks

            GroovyCompile groovyCompile = (GroovyCompile) allTasks.findByName('compileGroovy')
            ProcessResources processResources = (ProcessResources) allTasks.findByName("processResources")

            runtimeConfiguration.artifacts.add(new ExplodedDir( groovyCompile.destinationDir, groovyCompile, processResources) )
            explodedConfig.artifacts.add(new ExplodedDir( processResources.destinationDir, groovyCompile, processResources) )
        }
    }

    @Override
    protected Task createBuildPropertiesTask(Project project) {
        // no-op
    }

    @CompileStatic
    protected void configureSourcesJarTask(Project project) {
        def taskContainer = project.tasks
        if(taskContainer.findByName('sourcesJar') == null) {
            def jarTask = taskContainer.create("sourcesJar", Jar)
            jarTask.classifier = 'sources'
            jarTask.from SourceSets.findMainSourceSet(project).allSource
        }
    }

    protected void configureAstSources(Project project) {
        def mainSourceSet = SourceSets.findMainSourceSet(project)
        def sourceSets = SourceSets.findSourceSets(project)
        project.sourceSets {
            ast {
                groovy {
                    compileClasspath += project.configurations.compile
                }
            }
            main {
                compileClasspath += sourceSets.ast.output
            }
            test {
                compileClasspath += sourceSets.ast.output
            }
        }

        def copyAstClasses = project.task(type: Copy, "copyAstClasses") {
            from sourceSets.ast.output
            into mainSourceSet.output.classesDir
        }

        def taskContainer = project.tasks
        taskContainer.getByName('classes').dependsOn(copyAstClasses)

        taskContainer.withType(JavaExec) {
            classpath += sourceSets.ast.output
        }

        def javadocTask = taskContainer.findByName('javadoc')
        def groovydocTask = taskContainer.findByName('groovydoc')
        if (javadocTask) {
            javadocTask.configure {
                source += sourceSets.ast.allJava
            }
        }

        if (groovydocTask) {
            if( taskContainer.findByName('javadocJar') == null) {
                taskContainer.create("javadocJar", Jar).configure {
                    classifier = 'javadoc'
                    from groovydocTask.outputs
                }.dependsOn(javadocTask)
            }

            groovydocTask.configure {
                source += sourceSets.ast.allJava
            }
        }
    }

    protected void configurePluginJarTask(Project project) {
        project.jar {
            exclude "application.yml"
            exclude "application.groovy"
            exclude "logback.groovy"
        }
    }

    protected void configurePluginResources(Project project) {
        project.afterEvaluate() {
            ProcessResources processResources = (ProcessResources) project.tasks.getByName('processResources')
            GrailsExtension grailsExtension = project.extensions.findByType(GrailsExtension)

            def processResourcesDependencies = []

            processResourcesDependencies << project.task(type: Copy, "copyCommands") {
                from "${project.projectDir}/src/main/scripts"
                into "${processResources.destinationDir}/META-INF/commands"
            }

            processResourcesDependencies << project.task(type: Copy, "copyTemplates") {
                from "${project.projectDir}/src/main/templates"
                into "${processResources.destinationDir}/META-INF/templates"
            }

            processResources.dependsOn(*processResourcesDependencies)
            project.processResources {
                exclude "spring/resources.groovy"
                exclude "**/*.gsp"
            }
        }

    }

    protected void configureProjectNameAndVersionASTMetadata(Project project) {
        def configScriptTask = project.tasks.create('configScript')

        def configFile = project.file("$project.buildDir/config.groovy")
        configScriptTask.outputs.file(configFile)

        def projectName = project.name
        def projectVersion = project.version
        configScriptTask.inputs.property('name', projectName)
        configScriptTask.inputs.property('version', projectVersion)
        configScriptTask.doLast {
            configFile.parentFile.mkdirs()
            configFile.text = """
withConfig(configuration) {
    inline(phase: 'CONVERSION') { source, context, classNode ->
        classNode.putNodeMetaData('projectVersion', '$projectVersion')
        classNode.putNodeMetaData('projectName', '$projectName')
        classNode.putNodeMetaData('isPlugin', 'true')
    }
}
"""
        }
        project.tasks.getByName('compileGroovy').dependsOn(configScriptTask)
        project.compileGroovy {
            groovyOptions.configurationScript = configFile
        }
    }

    protected void checkForConfigurationClash(Project project) {
        File yamlConfig = new File(project.projectDir,"grails-app/conf/plugin.yml")
        File groovyConfig = new File(project.projectDir,"grails-app/conf/plugin.groovy")
        if (yamlConfig.exists() && groovyConfig.exists()) {
            throw new RuntimeException("A plugin may define a plugin.yml or a plugin.groovy, but not both")
        }
    }

    @CompileStatic
    static class ExplodedDir implements PublishArtifact {
        final String extension = ""
        final String type = "dir"
        final Date date = new Date()

        final File file
        final TaskDependency buildDependencies

        ExplodedDir(File file, Object...tasks) {
            this.file = file
            this.buildDependencies = new DefaultTaskDependency().add(tasks)
        }

        @Override
        String getName() {
            file.name
        }

        @Override
        String getClassifier() {
            ""
        }
    }
}
