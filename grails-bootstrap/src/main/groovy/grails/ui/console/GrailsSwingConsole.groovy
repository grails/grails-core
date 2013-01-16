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
package grails.ui.console

import grails.util.BuildSettings
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.grails.cli.fork.ExecutionContext
import org.codehaus.groovy.grails.cli.fork.ForkedGrailsProcess

/**
 * Forks a Groovy Swing console UI for the current application
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class GrailsSwingConsole extends ForkedGrailsProcess{
    ExecutionContext executionContext

    GrailsSwingConsole(BuildSettings buildSettings) {
        executionContext = new ExecutionContext()
        executionContext.initialize(buildSettings)
    }


    private GrailsSwingConsole() {
        executionContext = readExecutionContext()
        if (executionContext == null) {
            throw new IllegalStateException("Forked process created without first creating execution context and calling fork()")
        }
    }

    static void main(String[] args) {
        new GrailsSwingConsole().run()
    }

    private void run() {
        ExecutionContext ec = executionContext
        BuildSettings buildSettings = initializeBuildSettings(ec)
        URLClassLoader classLoader = initializeClassLoader(buildSettings)
        initializeLogging(ec.grailsHome,classLoader)
        Thread.currentThread().setContextClassLoader(classLoader)

        final projectConsole = classLoader.loadClass("org.codehaus.groovy.grails.project.ui.GrailsProjectConsole").newInstance(buildSettings)

        runConsole(projectConsole)
    }

    void runConsole(def console) {
        ((GroovyObject)console).invokeMethod("run", null)
    }

    @Override
    ExecutionContext createExecutionContext() {
        return executionContext
    }


}
