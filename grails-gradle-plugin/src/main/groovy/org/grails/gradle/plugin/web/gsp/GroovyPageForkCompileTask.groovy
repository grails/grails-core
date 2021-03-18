package org.grails.gradle.plugin.web.gsp

import grails.io.ResourceUtils
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.codehaus.groovy.tools.shell.util.PackageHelper
import org.gradle.api.Action
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.process.ExecResult
import org.gradle.process.JavaExecSpec

/**
 * Abstract Gradle task for compiling templates, using GroovyPageCompilerForkTask
 * This Task is a Forked Java Task that is configurable with fork options provided
 * by {@link GspCompileOptions}
 *
 * @author David Estes
 * @since 4.0
 */
@CompileStatic
class GroovyPageForkCompileTask extends AbstractCompile {

    @Input
    @Optional
    String packageName

    @InputDirectory
    File srcDir

    @Input
    File tmpDir

    @Input
    @Optional
    String serverpath

    @Nested
    GspCompileOptions compileOptions = new GspCompileOptions()

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

    protected void compile() {

        if(packageName == null) {
            packageName = project.name
            if(!packageName) {
                packageName = project.projectDir.canonicalFile.name
            }
        }

        ExecResult result = project.javaexec(
                new Action<JavaExecSpec>() {
                    @Override
                    @CompileDynamic
                    void execute(JavaExecSpec javaExecSpec) {
                        javaExecSpec.setMain(getCompilerName())
                        javaExecSpec.setClasspath(getClasspath())

                        def jvmArgs = compileOptions.forkOptions.jvmArgs
                        if(jvmArgs) {
                            javaExecSpec.jvmArgs(jvmArgs)
                        }
                        javaExecSpec.setMaxHeapSize( compileOptions.forkOptions.memoryMaximumSize )
                        javaExecSpec.setMinHeapSize( compileOptions.forkOptions.memoryInitialSize )

                        //This is the OLD Style and seems kinda silly to be hard coded this way. but restores functionality
                        //for now
                        def configFiles = [
                            project.file("grails-app/conf/application.yml").canonicalPath,
                            project.file("grails-app/conf/application.groovy").canonicalPath
                        ].join(',')

                        def arguments = [
                            srcDir.canonicalPath,
                            destinationDir.canonicalPath,
                            tmpDir.canonicalPath,
                            targetCompatibility,
                            packageName,
                            serverpath,
                            configFiles,
                            compileOptions.encoding
                        ]

                        prepareArguments(arguments)
                        javaExecSpec.args(arguments)
                    }

                }
        )
        result.assertNormalExitValue()

    }

    void prepareArguments(List<String> arguments) {
        // no-op
    }

    protected String getCompilerName() {
        "org.grails.web.pages.GroovyPageCompilerForkTask"
    }


    String getFileExtension() {
        "gsp"
    }
}
