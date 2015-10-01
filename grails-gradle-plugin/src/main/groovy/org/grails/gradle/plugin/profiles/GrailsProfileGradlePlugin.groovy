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
package org.grails.gradle.plugin.profiles

import grails.io.IOUtils
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.CopySpec
import org.gradle.api.internal.artifacts.ivyservice.projectmodule.ProjectPublicationRegistry
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.api.internal.java.JavaLibrary
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.configuration.project.ProjectConfigurationActionContainer
import org.grails.cli.profile.commands.script.GroovyScriptCommand
import org.grails.gradle.plugin.profiles.tasks.ProfileCompilerTask

import javax.inject.Inject
/**
 * A plugin that is capable of compiling a Grails profile into a JAR file for distribution
 *
 * @author Graeme Rocher
 * @since 3.1
 */
@CompileStatic
class GrailsProfileGradlePlugin extends BasePlugin {

    @Inject
    GrailsProfileGradlePlugin( ProjectPublicationRegistry publicationRegistry, ProjectConfigurationActionContainer configurationActionContainer) {
        super(publicationRegistry, configurationActionContainer)
    }

    @Override
    void apply(Project project) {
        super.apply(project)

        def profileConfiguration = project.configurations.create("profile")

        def profileYml = project.file("profile.yml")
        if(!profileYml.exists()) {
            throw new RuntimeException("No 'profile.yml' file found. Not a valid Grails profile.")
        }
        else {
            def commandsDir = project.file("commands")
            def resourcesDir = new File(project.buildDir, "resources/profile")
            def templatesDir = project.file("templates")
            def skeletonsDir = project.file("skeleton")

            def spec1 = project.copySpec { CopySpec spec ->
                spec.from(commandsDir)
                spec.exclude("*.groovy")
                spec.into("META-INF/commands")
            }
            def spec2 = project.copySpec { CopySpec spec ->
                spec.from(templatesDir)
                spec.into("META-INF/templates")
            }
            def spec3 = project.copySpec { CopySpec spec ->
                spec.from(skeletonsDir)
                spec.into("META-INF/skeleton")
            }

            def processResources = project.tasks.create("processResources", Copy) { Copy c ->
                c.with(spec1, spec2, spec3)
                c.from(profileYml) { CopySpec it ->
                    it.into('META-INF')
                }
                c.into(resourcesDir)
            }

            def classsesDir = new File(project.buildDir, "classes/profile")
            def compileTask = project.tasks.create("compileProfile", ProfileCompilerTask) { ProfileCompilerTask task ->
                task.destinationDir = classsesDir
                task.source = commandsDir
                task.classpath = project.configurations.getByName("profile") + project.files(IOUtils.findJarFile(GroovyScriptCommand))
            }

            def jarTask = project.tasks.create("jar", Jar) { Jar jar ->
                jar.dependsOn(processResources, compileTask)
                jar.from(resourcesDir)
                jar.from(classsesDir)
                jar.destinationDir = new File(project.buildDir, "libs")
                jar.setDescription("Assembles a jar archive containing the profile classes.")
                jar.setGroup(BUILD_GROUP)

                ArchivePublishArtifact jarArtifact = new ArchivePublishArtifact(jar)
                project.getComponents().add(new JavaLibrary(jarArtifact, profileConfiguration.getAllDependencies()));
            }

            project.tasks.findByName("assemble").dependsOn jarTask
        }
    }
}
