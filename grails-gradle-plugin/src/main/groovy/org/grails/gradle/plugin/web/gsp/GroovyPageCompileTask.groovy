package org.grails.gradle.plugin.web.gsp

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.internal.project.IsolatedAntBuilder
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

/**
 * A task for compiling GSPs
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class GroovyPageCompileTask extends AbstractCompile {

    @Input
    @Optional
    String packagename

    @Input
    @Optional
    String serverpath

    @InputDirectory
    File srcDir

    @Override
    void setSource(Object source) {
        try {
            srcDir = project.file(source)
            if(srcDir.exists() && !srcDir.isDirectory()) {
                throw new IllegalArgumentException("The source for GSP compilation must be a single directory, but was $source")
            }
            super.setSource(source)
        } catch (e) {
            throw new IllegalArgumentException("The source for GSP compilation must be a single directory, but was $source")
        }
    }

    @TaskAction
    void execute(IncrementalTaskInputs inputs) {
        compile()
    }

    @CompileDynamic
    @Override
    protected void compile() {

        def compileTask = this
        Project gradleProject = project
        def antBuilder = gradleProject.services.get(IsolatedAntBuilder)
        String packagename = packagename ?: project.name
        String serverpath = serverpath ?: "/"

        antBuilder.withClasspath(classpath).execute {
            taskdef(name: 'gspc', classname: 'org.grails.web.pages.GroovyPageCompilerTask')
            def dest = compileTask.destinationDir
            def tmpdir = new File(gradleProject.buildDir, "gsptmp")
            dest.mkdirs()

            gspc(destdir: dest,
                    srcdir: compileTask.srcDir,
                    packagename: packagename,
                    serverpath: serverpath,
                    tmpdir: tmpdir) {
                delegate.configs {
                    pathelement(path: gradleProject.file('grails-app/conf/application.yml').absolutePath)
                    pathelement(path: gradleProject.file('grails-app/conf/application.groovy').absolutePath)
                }
                delegate.classpath {
                    pathelement(path: dest.absolutePath)
                    pathelement(path: compileTask.classpath.asPath)
                }
            }
        }
    }
}
