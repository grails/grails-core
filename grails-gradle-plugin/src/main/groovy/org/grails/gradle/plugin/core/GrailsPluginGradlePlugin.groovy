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

import grails.util.Environment
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.internal.tasks.DefaultTaskDependency
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskDependency
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.grails.gradle.plugin.util.SourceSets
import org.springframework.boot.gradle.tasks.bundling.BootJar

import javax.inject.Inject

/**
 * A Gradle plugin for Grails plugins
 *
 * @author Graeme Rocher
 * @since 3.0
 *
 */
@CompileStatic
class GrailsPluginGradlePlugin extends GrailsGradlePlugin {

    @Inject
    GrailsPluginGradlePlugin(ToolingModelBuilderRegistry registry) {
        super(registry)
    }

    @Override
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

    protected String getDefaultProfile() {
        'web-plugin'
    }

    /**
     * Configures an exploded configuration that can be used to build the classpath of the application from subprojects that are plugins without contructing a JAR file
     *
     * @param project The project instance
     */
    protected void configureExplodedDirConfiguration(Project project) {

        ConfigurationContainer allConfigurations = project.configurations

        def runtimeConfiguration = allConfigurations.findByName('runtime')
        def explodedConfig = allConfigurations.create('exploded')
        explodedConfig.extendsFrom(runtimeConfiguration)
        if(Environment.isDevelopmentRun() && isExploded(project)) {
            runtimeConfiguration.artifacts.clear()
            // add the subproject classes as outputs
            TaskContainer allTasks = project.tasks

            GroovyCompile groovyCompile = (GroovyCompile) allTasks.findByName('compileGroovy')
            ProcessResources processResources = (ProcessResources) allTasks.findByName("processResources")

            runtimeConfiguration.artifacts.add(new ExplodedDir( groovyCompile.destinationDir, groovyCompile, processResources) )
            explodedConfig.artifacts.add(new ExplodedDir( processResources.destinationDir, groovyCompile, processResources) )
        }
    }

    @CompileDynamic
    private boolean isExploded(Project project) {
        Boolean.valueOf(project.properties.getOrDefault('exploded', 'false').toString())
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

    @Override
    protected void applySpringBootPlugin(Project project) {
        super.applySpringBootPlugin(project)
        project.tasks.withType(BootJar) { BootJar bootJar ->
            bootJar.enabled = false
        }
    }

    @CompileDynamic
    protected void configureAstSources(Project project) {
        SourceSet mainSourceSet = SourceSets.findMainSourceSet(project)
        SourceSetContainer sourceSets = SourceSets.findSourceSets(project)
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
            into "${project.buildDir}/classes/groovy/main"
        }

        def taskContainer = project.tasks
        taskContainer.getByName('classes').dependsOn(copyAstClasses)

        taskContainer.withType(JavaExec) {
            classpath += sourceSets.ast.output
        }

        Task javadocTask = taskContainer.findByName('javadoc')
        Task groovydocTask = taskContainer.findByName('groovydoc')
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
        Jar jarTask = (Jar)project.tasks.findByName('jar')
        // re-enable, since Boot disable this
        project.getTasks().getByName(JavaPlugin.JAR_TASK_NAME).setEnabled(true)
        jarTask.exclude "application.yml"
        jarTask.exclude "application.groovy"
        jarTask.exclude "logback.groovy"
    }

    @CompileDynamic
    protected void configurePluginResources(Project project) {
        project.afterEvaluate() {
            ProcessResources processResources = (ProcessResources) project.tasks.getByName('processResources')

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

    @CompileDynamic
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
