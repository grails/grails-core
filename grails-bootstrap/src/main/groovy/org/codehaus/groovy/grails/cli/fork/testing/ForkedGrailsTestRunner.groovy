/*
 * Copyright 2012 SpringSource
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
package org.codehaus.groovy.grails.cli.fork.testing

import grails.build.logging.GrailsConsole
import grails.util.BuildSettings
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.grails.cli.fork.ExecutionContext
import org.codehaus.groovy.grails.cli.fork.ForkedGrailsProjectClassExecutor
import org.codehaus.groovy.grails.cli.support.GrailsBuildEventListener
import org.codehaus.groovy.grails.cli.support.ScriptBindingInitializer
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils

/**
 *
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class ForkedGrailsTestRunner extends ForkedGrailsProjectClassExecutor {
    ForkedGrailsTestRunner(BuildSettings buildSettings) {
        super(buildSettings)
        setReloading(false)
        setForkReserve(true)
    }

    protected ForkedGrailsTestRunner() {
    }

    @Override
    protected ExecutionContext createExecutionContext() {
        return new TestExecutionContext()
    }

    @Override
    protected GroovyClassLoader createClassLoader(BuildSettings buildSettings) {
        final classLoader = super.createClassLoader(buildSettings)
        final urls = classLoader.URLs.toList()

        for(File f in buildSettings.testDependencies) {
            def url = f.toURI().toURL()
            if (!urls.contains(url)) {
                classLoader.addURL(url)
            }
        }
        return classLoader
    }

    static void main(String[] args) {
        try {
            new ForkedGrailsTestRunner().run()
            System.exit(0)
        } catch (Throwable e) {
            GrailsConsole.getInstance().error("Error running forked test-app: " + e.getMessage(), e)
            System.exit(1)
        }

    }


    @Override
    protected String getProjectClassType() {
        return "org.codehaus.groovy.grails.test.runner.GrailsProjectTestRunner"
    }

    @Override
    protected Object createInstance(Class projectComponentClass, BuildSettings buildSettings) {
        final scriptBinding = new Binding()
        final pluginSettings = GrailsPluginUtils.getPluginBuildSettings(buildSettings)
        ScriptBindingInitializer.initBinding(scriptBinding, buildSettings, (URLClassLoader)forkedClassLoader, GrailsConsole.getInstance())
        GrailsBuildEventListener eventListener = (GrailsBuildEventListener)scriptBinding.getVariable("eventListener")
        scriptBinding.setVariable("pluginSettings", pluginSettings)
        scriptBinding.setVariable("grailsSettings", buildSettings)
        eventListener.initialize()
        final projectCompiler = forkedClassLoader.loadClass("org.codehaus.groovy.grails.compiler.GrailsProjectCompiler").newInstance(pluginSettings)

        final projectPackager = forkedClassLoader.loadClass("org.codehaus.groovy.grails.project.packaging.GrailsProjectPackager").newInstance(projectCompiler, eventListener)

        final testRunner = projectComponentClass.newInstance(projectPackager)

        final projectLoader = forkedClassLoader.loadClass("org.codehaus.groovy.grails.project.loader.GrailsProjectLoader").newInstance(projectPackager)
        final warCreator = forkedClassLoader.loadClass("org.codehaus.groovy.grails.project.packaging.GrailsProjectWarCreator").newInstance(buildSettings, eventListener,projectPackager)
        final integrationPhaseConfigurer = instantiateIntegrationPhaseConfig(testRunner, projectLoader)

        final projectRunner = forkedClassLoader.loadClass("org.codehaus.groovy.grails.project.container.GrailsProjectRunner").newInstance(projectPackager, warCreator,forkedClassLoader)
        final functionalPhaseConfigurer = forkedClassLoader.loadClass("org.codehaus.groovy.grails.test.runner.phase.FunctionalTestPhaseConfigurer").newInstance(projectRunner)

        setTestExecutionContext(testRunner, scriptBinding,integrationPhaseConfigurer, functionalPhaseConfigurer)
        return testRunner
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected Object instantiateIntegrationPhaseConfig(testRunner, projectLoader) {
        forkedClassLoader.loadClass("org.codehaus.groovy.grails.test.runner.phase.IntegrationTestPhaseConfigurer").newInstance(testRunner.projectTestCompiler, projectLoader)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void setTestExecutionContext(testRunner, Binding scriptBinding,integrationPhaseConfigurer, functionalPhaseConfigurer) {
        testRunner.initialiseContext(scriptBinding)
        testRunner.testFeatureDiscovery.configurers.integration = integrationPhaseConfigurer
        testRunner.testFeatureDiscovery.configurers.functional = functionalPhaseConfigurer
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void runInstance(instance) {

        instance.projectPackager.projectCompiler.configureClasspath()
        instance.projectPackager.packageApplication()
        instance.runAllTests(executionContext.argsMap)
    }


}
class TestExecutionContext extends ExecutionContext {
    @Override
    protected List<File> buildMinimalIsolatedClasspath(BuildSettings buildSettings) {
        final classpath = super.buildMinimalIsolatedClasspath(buildSettings)
        classpath << buildSettings.testDependencies.find { File f -> f.name.startsWith('junit') }
        return classpath
    }
}
