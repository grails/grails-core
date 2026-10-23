/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.grails.gradle.plugin.views.gsp

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import javax.inject.Inject

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import org.gradle.api.Action
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.FileTree
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.JavaExecSpec

import grails.util.Environment
import org.grails.gradle.plugin.views.ViewCompileDestination
import org.grails.gradle.plugin.views.ViewCompileEnvironment
import org.grails.gradle.plugin.views.ViewCompileOptions

/**
 * Abstract Gradle task for compiling templates, using GroovyPageForkedCompiler
 * This Task is a Forked Java Task that is configurable with fork options provided
 * by {@link ViewCompileOptions}
 *
 * <p>Cacheable. What a page compiles to depends on the Groovy that compiles it and the classes it
 * is compiled against, which {@code AbstractCompile} declares as the classpath input, on the Java it
 * is compiled for, and on the Grails environment whose configuration it is compiled with, which is
 * {@link #getGrailsEnvironment()}.</p>
 *
 * <p>Compiles every page, from an empty destination, each time it runs; see
 * {@link ViewCompileDestination}. The compiler's own economy, of leaving a page alone whose class is
 * newer than its source, would otherwise carry pages of another environment, classpath or
 * configuration, or pages that no longer exist, into the archive and the build cache.</p>
 *
 * @author David Estes
 * @since 4.0
 */
@CompileStatic
@CacheableTask
abstract class GroovyPageForkCompileTask extends AbstractCompile {

    @Input
    @Optional
    final Property<String> packageName

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    final ConfigurableFileCollection grailsConfigurationPaths

    /**
     * The directory of the pages, which the compiler names them under and finds them by. Internal
     * rather than an input directory, which has to exist, where a project without
     * {@code src/main/webapp} has none for its webapp pages; the pages beneath it are the input, as
     * {@link #getAllPages()}.
     */
    @Internal
    final DirectoryProperty srcDir

    @Nested
    final ViewCompileOptions compileOptions

    @LocalState
    String tmpDirPath

    @Input
    @Optional
    final Property<String> serverpath

    /**
     * The Grails environment the pages are compiled for, by name.
     *
     * <p>A page is compiled against the configuration of one environment: the forked compiler merges
     * that environment's block of {@code application.yml} and {@code application.groovy} over the rest
     * before it reads the codecs and the other settings a page carries once compiled. The environment
     * is therefore part of what the pages are, and part of the cache key, so pages compiled for one
     * environment are not restored into a build for another.</p>
     *
     * <p>Defaults as {@link ViewCompileEnvironment#defaultFor} says: to an environment the fork
     * options of {@link #getCompileOptions()} already name, else to the one the build runs with, else
     * to {@code development}. Read as {@link #getStatedEnvironment()}, which is the input.</p>
     *
     * @since 7.0.18
     */
    @Internal
    final Property<String> grailsEnvironment

    private ExecOperations execOperations

    /** The build directory, which {@link #getDestinationDirectory()} has to be within. */
    private final DirectoryProperty buildDirectory

    /**
     * Where the pages are compiled to. Emptied before each compilation, so a directory of the task's
     * own, within the build directory; see {@link ViewCompileDestination}.
     */
    @OutputDirectory
    final DirectoryProperty destinationDirectory

    @Inject
    GroovyPageForkCompileTask(ExecOperations execOperations, ObjectFactory objectFactory) {
        this.execOperations = execOperations
        packageName = objectFactory.property(String).convention(project.name ?: project.projectDir.canonicalFile.name)
        srcDir = objectFactory.directoryProperty()
        compileOptions = objectFactory.newInstance(ViewCompileOptions)
        serverpath = objectFactory.property(String)
        grailsEnvironment = objectFactory.property(String).convention(ViewCompileEnvironment.defaultFor(project.providers, compileOptions))
        // the input is what the compiler gets: a change to it once the input is read would be compiled but not keyed
        grailsEnvironment.finalizeValueOnRead()
        grailsConfigurationPaths = objectFactory.fileCollection()
        grailsConfigurationPaths.from(
                project.layout.projectDirectory.file('grails-app/conf/application.yml'),
                project.layout.projectDirectory.file('grails-app/conf/application.groovy')
        )
        buildDirectory = project.layout.buildDirectory
        destinationDirectory = objectFactory.directoryProperty().convention(buildDirectory.dir('gsp-classes/main'))
    }

    /**
     * The environment stated to the forked compiler, as the {@code grails.env} system property in
     * place of any the fork options name, so that nothing the compiler inherits decides:
     * {@link #getGrailsEnvironment()} by the environment's full name, which is what the pages are
     * compiled for and so what the cache key holds. A blank {@code grailsEnvironment} fails the build
     * here, before anything is compiled or removed.
     *
     * @since 7.0.18
     */
    @Input
    String getStatedEnvironment() {
        ViewCompileEnvironment.forCompiler(grailsEnvironment, path)
    }

    /** Gradle's file operations, which empty the destination; Gradle supplies them to the task it generates. */
    @Inject
    protected FileSystemOperations getFileSystemOperations() {
        throw new UnsupportedOperationException()
    }

    @Override
    @PathSensitive(PathSensitivity.RELATIVE)
    FileTree getSource() {
        return super.getSource()
    }

    /**
     * Every page beneath {@link #getSrcDir()}, as the compiler reads them: it finds the pages by the
     * directory and compiles them all, so a page the task's own filters leave out of
     * {@link #getSource()} is compiled all the same, and is an input all the same. Fingerprinted by
     * path under the directory, so a checkout elsewhere reuses what this one compiled.
     *
     * @since 7.0.18
     */
    @InputFiles
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    FileTree getAllPages() {
        srcDir.asFileTree.matching { PatternFilterable pages -> pages.include('**/*.' + fileExtension) }
    }

    @Override
    void setSource(Object source) {
        if (Directory.isAssignableFrom(source.class)) {
            this.srcDir.set(source as Directory)
        }
        else if (File.isAssignableFrom(source.class)) {
            this.srcDir.set(source as File)
            if (!srcDir.getAsFile().get().isDirectory()) {
                throw new IllegalArgumentException("The source for ${getFileExtension().toUpperCase()} compilation must be a single directory, but was $source")
            }
        }
        else if (DirectoryProperty.isAssignableFrom(source.class)) {
            this.srcDir.set(source as DirectoryProperty)
        }
        else {
            throw new RuntimeException("Unsupported source type: ${source.class.name}")
        }
        super.setSource(source)
    }

    @TaskAction
    void execute() {
        compile()
    }

    protected void compile() {
        String environment = statedEnvironment
        logger.info('Compiling the pages for the {} environment', environment)
        ViewCompileDestination.empty(fileSystemOperations, destinationDirectory.get().asFile, buildDirectory.get().asFile, classpath.files, path)
        ExecResult result = execOperations.javaexec(
                new Action<JavaExecSpec>() {
                    @Override
                    @CompileDynamic
                    void execute(JavaExecSpec javaExecSpec) {
                        javaExecSpec.mainClass.set(getCompilerName())
                        javaExecSpec.setClasspath(getClasspath())

                        List<String> jvmArgs = ViewCompileEnvironment.withoutStated(compileOptions.forkOptions.jvmArgs)
                        if (jvmArgs) {
                            javaExecSpec.jvmArgs(jvmArgs)
                        }
                        javaExecSpec.setMaxHeapSize(compileOptions.forkOptions.memoryMaximumSize)
                        javaExecSpec.setMinHeapSize(compileOptions.forkOptions.memoryInitialSize)
                        javaExecSpec.systemProperty(Environment.KEY, environment)

                        String configFiles = grailsConfigurationPaths.files.collect { it.canonicalPath }.join(',')

                        Path path = Paths.get(tmpDirPath)
                        File tmp = Files.exists(path) ? path.toFile() : Files.createDirectories(path).toFile()
                        List<String> arguments = [
                                srcDir.get().asFile.canonicalPath,
                                destinationDirectory.get().asFile.canonicalPath,
                                tmp.canonicalPath,
                                targetCompatibility,
                                packageName.get() as String,
                                serverpath.getOrNull() as String,
                                configFiles,
                                compileOptions.encoding.get()
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

    @Input
    protected String getCompilerName() {
        'org.grails.web.pages.GroovyPageForkedCompiler'
    }

    @Input
    String getFileExtension() {
        'gsp'
    }
}
