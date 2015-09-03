package org.grails.gradle.plugin.run

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
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
            bootExtension.setMainClass(mainClass)
            JavaExec javaExec = (JavaExec)project.tasks.findByName("bootRun")
            javaExec.setMain(mainClass)

            ExtraPropertiesExtension extraProperties = (ExtraPropertiesExtension) getProject()
                    .getExtensions().getByName("ext");
            extraProperties.set("mainClassName", mainClass)
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

            def mainClass = mainClassFinder.findMainClass(mainSourceSet.output.classesDir)
            mainClassFile.text = mainClass
            return mainClass
        }

    }

    protected MainClassFinder createMainClassFinder() {
        new MainClassFinder()
    }
}
