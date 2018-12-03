package org.grails.gradle.plugin.run

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.api.tasks.TaskAction
import org.grails.gradle.plugin.util.SourceSets
import org.grails.io.support.MainClassFinder
import org.springframework.boot.gradle.tasks.bundling.BootArchive
import org.springframework.boot.gradle.tasks.run.BootRun

/**
 * A task that finds the main task, differs slightly from Boot's version as expects a subclass of GrailsConfiguration
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class FindMainClassTask extends DefaultTask {

    @TaskAction
    void setMainClassProperty() {
        Project project = this.project
        BootRun bootRun = (BootRun)project.tasks.findByName("bootRun")
        if ( bootRun != null ) {
            String mainClass = findMainClass()
            if(mainClass != null) {
                bootRun.setMain(mainClass)
                ExtraPropertiesExtension extraProperties = (ExtraPropertiesExtension) getProject()
                        .getExtensions().getByName("ext")
                extraProperties.set("mainClassName", mainClass)
            }
        }
    }

    protected String findMainClass() {
        Project project = this.project

        File buildDir = project.buildDir
        buildDir.mkdirs()
        File mainClassFile = new File(buildDir, ".mainClass")
        if(mainClassFile.exists()) {
            return mainClassFile.text
        }
        else {

/*            // Try the SpringBoot extension setting
            def bootExtension = project.extensions.findByType( SpringBootPluginExtension )
            if(bootExtension?.mainClass) {
                return bootExtension.mainClass
            }*/

            SourceSet mainSourceSet = SourceSets.findMainSourceSet(project)

            if(!mainSourceSet) return null

            MainClassFinder mainClassFinder = createMainClassFinder()

            Set<File> classesDirs = resolveClassesDirs(mainSourceSet.output, project).files
            String mainClass = null
            for(File classesDir in classesDirs) {

                mainClass = mainClassFinder.findMainClass(classesDir)
                if(mainClass != null) {
                    mainClassFile.text = mainClass
                    break
                }
            }

            if(mainClass == null) {
                mainClass = mainClassFinder.findMainClass(new File(project.buildDir, "classes/groovy/main"))
                if(mainClass != null) {
                    mainClassFile.text = mainClass
                }
                else {
                    throw new RuntimeException("Could not find Application main class. Please set 'springBoot.mainClass'.")
                }
            }
            return mainClass
        }

    }

    protected FileCollection resolveClassesDirs(SourceSetOutput output, Project project) {
        output?.classesDirs ?: project.files(new File(project.buildDir, "classes/main"))
    }

    protected MainClassFinder createMainClassFinder() {
        new MainClassFinder()
    }
}
