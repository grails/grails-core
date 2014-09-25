package org.grails.gradle.plugin.web.gsp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.project.IsolatedAntBuilder

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

        def compileGroovyPages = project.tasks.create("compileGroovyPages") << {
            def antBuilder = project.services.get(IsolatedAntBuilder)

            antBuilder.withClasspath(project.configurations.compile).execute {
                taskdef (name: 'gspc', classname : 'org.grails.web.pages.GroovyPageCompilerTask')
                def dest = new File(project.buildDir, "classes/main")
                def tmpdir = new File(project.buildDir, "gsptmp")
                dest.mkdirs()

                gspc(destdir: dest,
                    srcdir:"${project.projectDir}/grails-app/views",
                    packagename: project.name,
                    serverpath:"/WEB-INF/grails-app/views/",
                    tmpdir: tmpdir) {
                    classpath {
                        pathelement( path: dest.absolutePath )
                        pathelement( path: (project.configurations.gspCompile + project.configurations.compile ).asPath)
                    }
                }

                def webAppDir = new File(project.projectDir, "web-app")
                if(webAppDir.exists()) {
                    gspc(destdir: dest,
                            srcdir:webAppDir,
                            packagename: project.name,
                            serverpath:"/",
                            tmpdir: tmpdir) {
                        classpath {
                            pathelement( path: dest.absolutePath )
                            pathelement( path: (project.configurations.gspCompile + project.configurations.compile ).asPath)
                        }
                    }
                }

                def groovyTemplatesDir = new File(project.projectDir, "src/main/templates")
                if(groovyTemplatesDir.exists()) {
                    gspc(destdir: dest,
                            srcdir:groovyTemplatesDir,
                            packagename: project.name,
                            serverpath:"/",
                            tmpdir: tmpdir) {
                        classpath {
                            pathelement( path: dest.absolutePath )
                            pathelement( path: (project.configurations.gspCompile + project.configurations.compile ).asPath)
                        }
                    }
                }
            }
        }


        compileGroovyPages.dependsOn( project.tasks.getByName("compileGroovy") )
        project.tasks.getByName('assemble').dependsOn(compileGroovyPages)
    }
}
