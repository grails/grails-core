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

projectCompiler = null

target(setCompilerSettings: "Updates the compile build settings based on args") {
    depends(parseArguments)

	projectCompiler = new GrailsProjectCompiler(basedir, 
												grailsSettings.sourceDir.absolutePath, 
												pluginSettings.pluginSourceFiles, 
												compConfig, 
												classLoader)
	projectCompiler.ant = ant
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
            event("StatusFinal", ["Compilation error: ${e.message}"])
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
	ant.taskdef (name: 'gspc', classname : 'org.codehaus.groovy.grails.web.pages.GroovyPageCompilerTask')
    // compile gsps in grails-app/views directory
    File gspTmpDir = new File(grailsSettings.projectWorkDir, "gspcompile")
    ant.gspc(destdir:classesDir,
             srcdir:"${basedir}/grails-app/views",
             packagename:GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(grailsAppName),
             serverpath:"/WEB-INF/grails-app/views/",
             classpathref:"grails.compile.classpath",
             tmpdir:gspTmpDir)

    // compile gsps in web-app directory
    ant.gspc(destdir:classesDir,
             srcdir:"${basedir}/web-app",
             packagename:"${GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(grailsAppName)}_webapp",
             serverpath:"/",
             classpathref:"grails.compile.classpath",
             tmpdir:gspTmpDir)

    // compile views in plugins
    loadPlugins()
    def pluginInfos = pluginSettings.supportedPluginInfos
    if (pluginInfos) {
        for (GrailsPluginInfo info in pluginInfos) {
            File pluginViews = new File(info.pluginDir.file, "grails-app/views")
            if (pluginViews.exists()) {
                def viewPrefix="/WEB-INF/plugins/${info.name}-${info.version}/grails-app/views/"
                ant.gspc(destdir:classesDir,
                         srcdir:pluginViews,
                         packagename:GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(info.name),
                         serverpath:viewPrefix,
                         classpathref:"grails.compile.classpath",
                         tmpdir:gspTmpDir)
            }
        }
    }
}
