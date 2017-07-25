package org.grails.gradle.plugin.run

import grails.util.BuildSettings
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
import org.springframework.boot.gradle.SpringBootPluginExtension

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
        def bootExtension = project.extensions.findByType(SpringBootPluginExtension)
        if ( bootExtension != null ) {
            def mainClass = findMainClass()
            if(mainClass != null) {
                bootExtension.setMainClass(mainClass)
                JavaExec javaExec = (JavaExec)project.tasks.findByName("bootRun")
                javaExec.setMain(mainClass)

                ExtraPropertiesExtension extraProperties = (ExtraPropertiesExtension) getProject()
                        .getExtensions().getByName("ext");
                extraProperties.set("mainClassName", mainClass)
            }
        }
    }

    protected String findMainClass() {
        Project project = this.project

        def buildDir = project.buildDir
        buildDir.mkdirs()
        def mainClassFile = new File(buildDir, ".mainClass")
        if(mainClassFile.exists()) {
            return mainClassFile.text
        }
        else {

            // Try the SpringBoot extension setting
            def bootExtension = project.extensions.findByType( SpringBootPluginExtension )
            if(bootExtension?.mainClass) {
                return bootExtension.mainClass
            }

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

    @CompileDynamic
    protected FileCollection resolveClassesDirs(SourceSetOutput output, Project project) {
        FileCollection classesDirs
        try {
            classesDirs = output?.classesDirs ?: project.files(new File(project.buildDir, "classes/main"))
        }
        catch(e) {
            classesDirs = output?.classesDir ? project.files(output.classesDir) : project.files(new File(project.buildDir, "classes/main"))
        }
        return classesDirs
    }

    protected MainClassFinder createMainClassFinder() {
        new MainClassFinder()
    }
}
