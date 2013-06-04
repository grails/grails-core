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
package org.codehaus.groovy.grails.cli.fork.compile

import grails.build.logging.GrailsConsole
import grails.util.BuildSettings
import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.cli.fork.ForkedGrailsProjectClassExecutor
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils

/**
 * Forked implementation of Grails project compiler
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class ForkedGrailsCompiler extends ForkedGrailsProjectClassExecutor {

    ForkedGrailsCompiler(BuildSettings buildSettings) {
        super(buildSettings)
        setReloading(false)
        setDaemon(true)
        setDaemonPort(DEFAULT_DAEMON_PORT + 1)
        setForkReserve(false)
    }

    protected ForkedGrailsCompiler() {
    }

    static void main(String[] args) {
        try {
            new ForkedGrailsCompiler().run()
            System.exit(0)
        } catch (Throwable e) {
            GrailsConsole.getInstance().error("Error running forked compilation: " + e.getMessage(), e)
            System.exit(1)
        }
    }

    @Override
    protected void configureFork(BuildSettings buildSettings) {
        final runConfig = buildSettings.forkSettings.compile
        if (runConfig instanceof Map)
            configure(runConfig)
    }

    @Override
    protected String getProjectClassType() {
        return "org.codehaus.groovy.grails.compiler.GrailsProjectCompiler"
    }

    @Override
    protected Object createInstance(Class projectComponentClass, BuildSettings buildSettings) {
        projectComponentClass.newInstance(GrailsPluginUtils.getPluginBuildSettings())
    }

    @Override
    void runInstance(instance) {
        ((GroovyObject)instance).invokeMethod("configureClasspath", null)
        ((GroovyObject)instance).invokeMethod("compileAll", null)
    }
}
