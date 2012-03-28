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

import org.apache.tools.ant.BuildException
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import org.codehaus.groovy.grails.web.pages.GspTagInfo
import org.codehaus.groovy.grails.web.pages.GspTagParser
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoaderHolder
import org.springframework.core.io.FileSystemResource

/**
 * Gant script that compiles Groovy and Java files in the src tree.
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
    depends(compilePlugins, generateGspTags)
    profile("Compiling sources to location [$classesDirPath]") {
        withCompilationErrorHandling {
            projectCompiler.compile()
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
        withCompilationErrorHandling {
            projectCompiler.compilePlugins()
        }
    }
}

private withCompilationErrorHandling(Closure callable) {
    try {
        callable.call()
    }
    catch (BuildException e) {
        if (e.cause instanceof MultipleCompilationErrorsException) {
            event("StatusError", ["Compilation error: ${e.cause.message}"])
        }
        else {
            grailsConsole.error "Fatal error during compilation ${e.class.name}: ${e.message}", e
        }
        exit 1
    }
    catch(Throwable e) {
        grailsConsole.error "Fatal error during compilation ${e.class.name}: ${e.message}", e
        exit 1
    }
}

target(compilepackage : "Compile & Compile GSP files") {
    depends(compile, compilegsp)
}

target(compilegsp : "Compile GSP files") {
    try {
        projectCompiler.compileGroovyPages(grailsAppName, grailsSettings.classesDir)
    }
    catch (e) {
        if (e.cause instanceof GrailsTagException) {
            event("StatusError", ["GSP Compilation error in file $e.cause.fileName at line $e.cause.lineNumber: $e.cause.message"])
            exit(1)
        }
        else {
            event("StatusError", ["Compilation error: ${e.cause?.message ?: e.message}"])
            exit(1)
        }
    }
}

target(generateGspTags: "Generate taglib code for GSP tags") {
    def gspTags = resolveResources("file:${basedir}/grails-app/taglib/**/*.gsp") as List
    def pluginInfos = pluginSettings.compileScopePluginInfo.pluginInfos
    if (pluginInfos) {
        for (info in pluginInfos) {
            gspTags.addAll(resolveResources("file:${info.pluginDir.file}/grails-app/taglib/**/*.gsp"))
        }
    }

    File generatedTagsDir = new File(projectGeneratedSourcesDir, "grails-app/taglib")
    projectCompiler.addSrcDirectory(generatedTagsDir.canonicalFile.absolutePath)
    for (gsp in gspTags.file) {
        def tagInfo = new GspTagInfo(gsp)
        File destinationDir = new File(generatedTagsDir, tagInfo.packageName.replace('.', '/'))
        File destination = new File(destinationDir, tagInfo.tagLibFileName)
        GrailsResourceLoaderHolder.resourceLoader.addResource(new FileSystemResource(destination))
        if (!destination.exists() || destination.lastModified() < gsp.lastModified()) {
            destinationDir.mkdirs()
            def parser = new GspTagParser(tagInfo)
            destination.text = parser.parse().text
        }
    }
    if (gspTags) {
        //force reload of pluginSettings in order to pick up the new artifacts
        pluginSettings.clearCache()
    }
}
