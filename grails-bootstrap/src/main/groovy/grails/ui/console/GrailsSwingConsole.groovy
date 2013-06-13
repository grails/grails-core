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
import org.codehaus.groovy.grails.cli.fork.ForkedGrailsProjectClassExecutor

/**
 * Forks a Groovy Swing console UI for the current application
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class GrailsSwingConsole extends ForkedGrailsProjectClassExecutor{

    GrailsSwingConsole(BuildSettings buildSettings) {
        super(buildSettings)
    }

    protected GrailsSwingConsole() {
    }

    static void main(String[] args) {
        new GrailsSwingConsole().run()
    }

    @Override
    protected URLClassLoader initializeClassLoader(BuildSettings buildSettings) {
        final classLoader = (GroovyClassLoader)super.initializeClassLoader(buildSettings)
        final existing = classLoader.URLs
        for (File f in buildSettings.testDependencies) {
            final jarURL = f.toURI().toURL()
            if (!existing.contains(jarURL)) {
                 classLoader.addURL(jarURL)
            }
        }

        return classLoader
    }

    @Override
    protected String getProjectClassType() { "org.codehaus.groovy.grails.project.ui.GrailsProjectConsole" }

    @Override
    void runInstance(instance) {
        ((GroovyObject)instance).invokeMethod("run", null)
    }
}
