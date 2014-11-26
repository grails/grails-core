package org.grails.gradle.plugin.web.gsp

import org.gradle.api.Plugin
import org.gradle.api.Project
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

        project.tasks.create("compileGroovyPages", GroovyPageCompileTask) {
            destinationDir = destDir
            source = project.file("${project.projectDir}/grails-app/views")
            serverpath = "/WEB-INF/grails-app/views/"
            classpath = project.configurations.compile + project.configurations.gspCompile + project.fileTree(destDir)
        }

        project.tasks.create("compileWebappGroovyPages", GroovyPageCompileTask) {
            destinationDir = destDir
            source = project.file("${project.projectDir}/src/main/webapp")
            serverpath = "/"
            classpath = project.configurations.compile + project.configurations.gspCompile + project.fileTree(destDir)
        }


        def compileGroovyPages = project.tasks.getByName("compileGroovyPages")
        compileGroovyPages.dependsOn( project.tasks.getByName('compileWebappGroovyPages'))
        compileGroovyPages.dependsOn( project.tasks.getByName("compileGroovy") )

        def warTask = project.tasks.findByName('war')
        def assembleTask = project.tasks.findByName('jar')
        if(warTask) {
            warTask.dependsOn(compileGroovyPages)
        }
        else {
            assembleTask?.dependsOn(compileGroovyPages)
        }
    }

}


