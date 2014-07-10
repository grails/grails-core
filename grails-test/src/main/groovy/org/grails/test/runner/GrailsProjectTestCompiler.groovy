/*
 * Copyright 2013 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.test.runner

import grails.build.logging.GrailsConsole
import grails.util.BuildSettings
import grails.util.PluginBuildSettings
import org.codehaus.groovy.grails.cli.support.GrailsBuildEventListener
import org.codehaus.groovy.grails.project.compiler.GrailsProjectCompiler
import org.grails.test.GrailsTestType

/**
 * Compiles test with the appropriate classpath for the given {@link  GrailsTestType}
 *
 * @author Graeme Rocher
 */
class GrailsProjectTestCompiler extends GrailsProjectCompiler{

    static final GrailsConsole CONSOLE = GrailsConsole.getInstance()
    GrailsBuildEventListener buildEventListener
    BuildSettings buildSettings

    GrailsProjectTestCompiler(GrailsBuildEventListener buildEventListener, PluginBuildSettings pluginBuildSettings, ClassLoader rootLoader = Thread.currentThread().getContextClassLoader()) {
        super(pluginBuildSettings, rootLoader)
        this.buildEventListener = buildEventListener
        this.buildSettings = pluginBuildSettings.buildSettings
    }

    @Override
    AntBuilder getAnt() {
        final ant = super.getAnt()

        ant.path(id: "grails.test.classpath", testClasspath)
        ant.taskdef (name: 'testc', classname:'org.codehaus.groovy.grails.test.compiler.GrailsTestCompiler', classpathref:"grails.test.classpath")
        ant.taskdef (name: 'itestc', classname:'org.codehaus.groovy.grails.test.compiler.GrailsIntegrationTestCompiler', classpathref:"grails.test.classpath")

        return ant
    }
/**
     * Compiles all the test classes for a particular type of test, for
     * example "unit" or "webtest". Assumes that the source files are in
     * the "test/$type" directory. It also compiles the files to distinct
     * directories for each test type: "$testClassesDir/$type".
     * @param type The type of the tests to compile (not the test phase!)
     * For example, "unit", "jsunit", "webtest", etc.
     */
    void compileTests( GrailsTestType type, File source, File dest ) {
        buildEventListener.triggerEvent("TestCompileStart", type)

        ant.mkdir(dir: dest.path)
        try {
            def classpathId = "grails.test.classpath"
            def compilerName = 'groovyc'
            if(type.name == 'unit') {
                compilerName = 'testc'
            }
            else if(type.name == 'integration') {
                compilerName = 'itestc'
            }

            ant."${compilerName}"(destdir: dest, classpathref: classpathId,
                verbose: buildSettings.verboseCompile, listfiles: buildSettings.verboseCompile) {
                javac(classpathref: classpathId, debug: "yes")
                src(path: source)
            }

        }
        catch (e) {
            CONSOLE.error "Compilation error compiling [$type.name] tests: ${e.cause ? e.cause.message : e.message}", e.cause ? e.cause : e
            exit 1
        }

        buildEventListener.triggerEvent("TestCompileEnd", type)
    }

    /**
     * Puts some useful things on the classpath for integration tests.
     **/
    void packageTests(boolean triggerEvents = true) {
        if (triggerEvents)
            buildEventListener.triggerEvent("PackageTestsStart")
        ant.copy(todir: new File(buildSettings.testClassesDir, "integration").path) {
            fileset(dir: "${basedir}", includes: metadataFile.name)
        }
        ant.copy(todir: buildSettings.testClassesDir.path, failonerror: false) {
            fileset(dir: "${basedir}/grails-app/conf", includes: "**", excludes: "*.groovy, log4j*, hibernate, spring")
            fileset(dir: "${basedir}/grails-app/conf/hibernate", includes: "**/**")
            fileset(dir: "${buildSettings.sourceDir}/java") {
                include(name: "**/**")
                exclude(name: "**/*.java")
            }
            fileset(dir: "${buildSettings.testSourceDir}/unit") {
                include(name: "**/**")
                exclude(name: "**/*.java")
                exclude(name: "**/*.groovy")
            }
            fileset(dir: "${buildSettings.testSourceDir}/integration") {
                include(name: "**/**")
                exclude(name: "**/*.java")
                exclude(name: "**/*.groovy")
            }
        }
        if (triggerEvents)
            buildEventListener.triggerEvent("PackageTestsEnd")
    }
}
