package org.grails.gradle.plugin.web.gsp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.grails.gradle.plugin.util.SourceSets

/**
 * A plugin that adds support for compiling Groovy Server Pages (GSP)
 *
 * @author Graeme Rocher
 * @since 3.0
 */
class GroovyPagePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {

        project.configurations {
            gspCompile
        }

        project.dependencies {
            gspCompile 'javax.servlet:javax.servlet-api:3.1.0'
        }

        def mainSourceSet = SourceSets.findMainSourceSet(project)

        def destDir = mainSourceSet?.output?.classesDir ?: new File(project.buildDir, "main/classes")

        def providedConfig = project.configurations.findByName('provided')
        def allClasspath = project.configurations.compile + project.configurations.gspCompile + project.fileTree(destDir)
        if(providedConfig) {
            allClasspath += providedConfig
        }
        project.tasks.create("compileGroovyPages", GroovyPageCompileTask) {
            destinationDir = destDir
            source = project.file("${project.projectDir}/grails-app/views")
            serverpath = "/WEB-INF/grails-app/views/"
            classpath = allClasspath
        }

        project.tasks.create("compileWebappGroovyPages", GroovyPageCompileTask) {
            destinationDir = destDir
            source = project.file("${project.projectDir}/src/main/webapp")
            serverpath = "/"
            classpath = allClasspath
        }


        def compileGroovyPages = project.tasks.getByName("compileGroovyPages")
        compileGroovyPages.dependsOn( project.tasks.getByName('compileWebappGroovyPages'))
        compileGroovyPages.dependsOn( project.tasks.getByName("compileGroovy") )

        def warTask = project.tasks.findByName('war')
        def jarTask = project.tasks.findByName('jar')
        if(warTask) {
            warTask.dependsOn(compileGroovyPages)
        }
        if(jarTask) {
            jarTask?.dependsOn(compileGroovyPages)
        }
    }

}


