/*
 * Copyright 2004-2005 the original author or authors.
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

import org.codehaus.groovy.grails.project.compiler.GrailsProjectCompiler

/**
 * Gant script containing the Grails classpath setup.
 *
 * @author Peter Ledbrook
 * @author Graeme Rocher
 *
 * @since 1.1
 */

// No point doing this stuff more than once.
if (getBinding().variables.containsKey("_grails_classpath_called")) return
_grails_classpath_called = true

includeTargets << grailsScript("_GrailsSettings")

classpathSet = false
includePluginJarsOnClasspath = true
projectCompiler = new GrailsProjectCompiler(pluginSettings, classLoader)
projectCompiler.ant = ant

target(name:'classpath', description: "Sets the Grails classpath", prehook:null, posthook:null) {
    // Make sure the following code is only executed once.
    if (classpathSet) return

    projectCompiler.configureClasspath()
    compConfig = projectCompiler.config
    classpathSet = true
}

// The following variables are here for compatibility with older versions of Grails
getPluginLibDirs = pluginSettings.&pluginLibDirectories
getPluginJarFiles =  pluginSettings.&pluginJarFiles
getJarFiles = projectCompiler.&getJarFiles
getExtraDependencies =  projectCompiler.&getExtraDependencies
commonClasspath = projectCompiler.commonClasspath
compileClasspath = projectCompiler.compileClasspath
testClasspath = projectCompiler.testClasspath
runtimeClasspath = projectCompiler.runtimeClasspath


