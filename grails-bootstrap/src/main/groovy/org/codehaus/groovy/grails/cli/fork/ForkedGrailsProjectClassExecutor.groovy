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

import grails.util.BuildSettings
import groovy.transform.CompileStatic

/**
 * Base class that deals with the setup logic needed in order to run a Grails build system component (GrailsProjectCompiler, GrailsProjectLoader, GrailsProjectRunner etc.) in a forked process
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
abstract class ForkedGrailsProjectClassExecutor extends ForkedGrailsProcess{

    ForkedGrailsProjectClassExecutor(BuildSettings buildSettings) {
        executionContext = new ExecutionContext()
        executionContext.initialize(buildSettings)
    }


    protected ForkedGrailsProjectClassExecutor() {
        executionContext = readExecutionContext()
        if (executionContext == null) {
            throw new IllegalStateException("Forked process created without first creating execution context and calling fork()")
        }
    }


    protected final void run() {
        ExecutionContext ec = executionContext
        BuildSettings buildSettings = initializeBuildSettings(ec)
        URLClassLoader classLoader = initializeClassLoader(buildSettings)
        initializeLogging(ec.grailsHome,classLoader)
        Thread.currentThread().setContextClassLoader(classLoader)

        final projectComponentClass = classLoader.loadClass(getProjectClassType())
        final projectClassInstance = createInstance(projectComponentClass, buildSettings)

        if (!isReserveProcess()) {

            runInstance(projectClassInstance)
        }
        else {
            waitForResume()
            runInstance(projectClassInstance)
        }
    }


    protected Object createInstance(Class projectComponentClass, BuildSettings buildSettings) {
        projectComponentClass.newInstance(buildSettings)
    }

    protected abstract String getProjectClassType()

    abstract void runInstance(def instance)

    @Override
    ExecutionContext createExecutionContext() {
        return executionContext
    }


}
