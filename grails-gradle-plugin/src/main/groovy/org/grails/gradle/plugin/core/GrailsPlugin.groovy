package org.grails.gradle.plugin.core

import grails.util.BuildSettings
import grails.util.Environment
import groovy.transform.CompileStatic
import org.apache.tools.ant.filters.EscapeUnicode
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.GroovyCompile
import org.grails.gradle.plugin.agent.AgentTasksEnhancer
import org.grails.gradle.plugin.run.FindMainClassTask
import org.grails.gradle.plugin.watch.GrailsWatchPlugin
import org.grails.gradle.plugin.watch.WatchConfig

class GrailsPlugin extends GroovyPlugin {

    void apply(Project project) {
        super.apply(project)
        project.extensions.create("grails", GrailsExtension)

        enableNative2Ascii(project)
        registerFindMainClassTask(project)

        def projectDir = project.projectDir

        def grailsSourceDirs = []
        def excludedDirs = ['views', 'migrations', 'assets', 'i18n']
        new File("$projectDir/grails-app").eachDir { File subdir ->
            def dirName = subdir.name
            if(!subdir.hidden && !dirName.startsWith(".") && !excludedDirs.contains(dirName)) {
                grailsSourceDirs << subdir.absolutePath
            }
        }

        grailsSourceDirs << "$projectDir/src/main/groovy"

        System.setProperty( BuildSettings.APP_BASE_DIR, project.projectDir.absolutePath)
        def environment = Environment.current


        enableFileWatch(environment, project)


        project.sourceSets {
            main {
                groovy {
                    srcDirs = grailsSourceDirs
                    filter {
                        exclude "$projectDir/grails-app/conf/hibernate"
                        exclude "$projectDir/grails-app/conf/spring"
                        exclude "$projectDir/grails-app/conf/logging"
                    }
                    resources {
                        srcDirs = [
                                "$projectDir/grails-app/conf/logging",
                                "$projectDir/grails-app/conf/hibernate",
                                "$projectDir/grails-app/conf/spring",
                                "$projectDir/grails-app/views",
                                "$projectDir/grails-app/i18n",
                                "$projectDir/src/main/webapp"
                        ]
                    }
                }
            }
        }
    }

    protected void enableFileWatch(Environment environment, Project project) {
        if (environment.isReloadEnabled()) {
//            configureWatchPlugin(project)

            project.configurations {
                agent
            }
            project.dependencies {
                agent "org.springframework:springloaded:1.2.1.RELEASE"
            }
            project.afterEvaluate(new AgentTasksEnhancer())
        }
    }

    private void configureWatchPlugin(Project project) {
        new GrailsWatchPlugin().apply(project)
        NamedDomainObjectContainer<WatchConfig> watchConfigs = project.extensions.getByName('watch')
        def grailsConfig = watchConfigs.create("grailsApp")
        grailsConfig.directory = project.file("grails-app")
        grailsConfig.extensions = ['groovy', 'java']
        grailsConfig.tasks('compileGroovy')

        def groovyConfig = watchConfigs.create("groovyConfig")
        groovyConfig.directory = project.file("src/main/groovy")
        groovyConfig.extensions = ['groovy', 'java']
        groovyConfig.tasks('compileGroovy')
    }

    @CompileStatic
    protected void registerFindMainClassTask(Project project) {
        def findMainClassTask = project.tasks.create(name: "findMainClass", type: FindMainClassTask, overwrite: true)
        findMainClassTask.mustRunAfter project.tasks.withType(GroovyCompile)
        def bootRepackageTask = project.tasks.findByName("bootRepackage")
        if(bootRepackageTask) {
            bootRepackageTask.dependsOn findMainClassTask
        }
    }

    /**
     * Enables native2ascii processing of resource bundles
     **/
    protected void enableNative2Ascii(Project project) {
        for (SourceSet sourceSet in project.sourceSets) {
            project.tasks.getByName(sourceSet.processResourcesTaskName) { CopySpec task ->
                def grailsExt = project.extensions.getByType(GrailsExtension)
                if (grailsExt.native2ascii) {
                    task.from(sourceSet.resources) {
                        include '**/*.properties'
                        filter(EscapeUnicode)
                    }
                    task.from(sourceSet.resources) {
                        exclude '**/*.properties'
                    }
                }
            }
        }
    }

}
