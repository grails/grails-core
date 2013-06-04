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
package org.codehaus.groovy.grails.cli.fork

import grails.build.logging.GrailsConsole
import grails.util.BuildSettings
import grails.util.PluginBuildSettings
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import org.codehaus.groovy.grails.cli.support.GrailsBuildEventListener
import org.codehaus.groovy.grails.cli.support.ScriptBindingInitializer

/**
 * Base class that deals with the setup logic needed to run a Grails build system component
 * (GrailsProjectCompiler, GrailsProjectLoader, GrailsProjectRunner etc.) in a forked process.
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
abstract class ForkedGrailsProjectClassExecutor extends ForkedGrailsProcess {

    ForkedGrailsProjectClassExecutor(BuildSettings buildSettings) {
        executionContext = createExecutionContext()
        executionContext.initialize(buildSettings)
    }

    protected ExecutionContext createExecutionContext() {
        new ExecutionContext(this)
    }

    protected ForkedGrailsProjectClassExecutor() {
        executionContext = readExecutionContext()
        if (executionContext == null) {
            throw new IllegalStateException("Forked process created without first creating execution context and calling fork()")
        }
    }

    protected final void run() {

        if (isDaemonProcess()) {
            startDaemon { cmd ->
                def projectClassInstance = initializeProjectInstance()
                runInstance(projectClassInstance)
            }
        }
        else if (isReserveProcess()) {
            // don't wait if the resume directory already exists, another process exists
            if (!resumeDir.exists()) {
                executionContext = readExecutionContext()
                Object projectClassInstance = initializeProjectInstance()
                waitForResume()
                runInstance(projectClassInstance)
            }
        }
        else {
            Object projectClassInstance = initializeProjectInstance()
            runInstance(projectClassInstance)
        }
    }

    protected Object initializeProjectInstance() {
        ExecutionContext ec = executionContext
        BuildSettings buildSettings = initializeBuildSettings(ec)
        URLClassLoader classLoader = initializeClassLoader(buildSettings)
        initializeLogging(ec.grailsHome, classLoader)
        Thread.currentThread().setContextClassLoader(classLoader)

        final projectComponentClass = classLoader.loadClass(getProjectClassType())
        final projectClassInstance = createInstance(projectComponentClass, buildSettings)
        projectClassInstance
    }

    protected Object createInstance(Class projectComponentClass, BuildSettings buildSettings) {
        projectComponentClass.newInstance(buildSettings)
    }

    protected GrailsBuildEventListener createEventListener(Binding executionContext) {
        GrailsBuildEventListener eventListener = (GrailsBuildEventListener) executionContext.getVariable("eventListener")
        GrailsConsole grailsConsole = GrailsConsole.getInstance()
        eventListener.globalEventHooks = [
            StatusFinal:  [ {message -> grailsConsole.addStatus message.toString() } ],
            StatusUpdate: [ {message -> grailsConsole.updateStatus message.toString() } ],
            StatusError:  [ {message -> grailsConsole.error message.toString() } ]
        ]

        eventListener.initialize()
        addEventHookToBinding(executionContext, eventListener)
        eventListener
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void addEventHookToBinding(Binding executionContext, eventListener) {
        executionContext.setVariable("event", { String name, List args ->
            eventListener.triggerEvent(name, * args)
        })
    }

    protected Binding createExecutionContext(BuildSettings buildSettings, PluginBuildSettings pluginSettings) {
        final scriptBinding = new Binding()
        ScriptBindingInitializer.initBinding(scriptBinding, buildSettings, (URLClassLoader) forkedClassLoader, GrailsConsole.getInstance(), false)
        scriptBinding.setVariable("pluginSettings", pluginSettings)
        scriptBinding.setVariable(ScriptBindingInitializer.GRAILS_SETTINGS, buildSettings)
        scriptBinding.setVariable(ScriptBindingInitializer.ARGS_MAP, executionContext.argsMap)
        scriptBinding
    }

    protected abstract String getProjectClassType()

    abstract void runInstance(instance)
}
