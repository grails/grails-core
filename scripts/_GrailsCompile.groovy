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

import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo
import org.codehaus.groovy.grails.compiler.GrailsProjectCompiler
import grails.util.GrailsNameUtils

/**
 * Gant script that compiles Groovy and Java files in the src tree
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */
includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("_GrailsEvents")
includeTargets << grailsScript("_GrailsArgParsing")

ant.taskdef (name: 'groovyc', classname : 'org.codehaus.groovy.grails.compiler.Grailsc')
ant.path(id: "grails.compile.classpath", compileClasspath)

target(setCompilerSettings: "Updates the compile build settings based on args") {
    depends(parseArguments)

    if (argsMap.containsKey('verboseCompile')) {
        grailsSettings.verboseCompile = argsMap.verboseCompile as boolean
        projectCompiler.verbose = grailsSettings.verboseCompile
    }
}

target(compile : "Implementation of compilation phase") {
    depends(compilePlugins)

    profile("Compiling sources to location [$classesDirPath]") {
        try {
            projectCompiler.compile(grailsSettings.classesDir)
        }
        catch (Exception e) {
            event("StatusFinal", ["Compilation error: ${e.cause.message}"])
            exit(1)
        }

        classLoader.addURL(grailsSettings.classesDir.toURI().toURL())
        classLoader.addURL(grailsSettings.pluginClassesDir.toURI().toURL())
    }
}

target(compilePlugins: "Compiles source files of all referenced plugins.") {
    depends(setCompilerSettings, resolveDependencies)

    profile("Compiling sources to location [$classesDirPath]") {
        // First compile the plugins so that we can exclude any
        // classes that might conflict with the project's.
        projectCompiler.compilePlugins(grailsSettings.pluginClassesDir)
    }
}


target(compilepackage : "Compile & Compile GSP files") {
    depends(compile, compilegsp)
}

target(compilegsp : "Compile GSP files") {
    projectCompiler.compileGroovyPages(grailsAppName, grailsSettings.classesDir)
}
