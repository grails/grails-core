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
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import org.codehaus.groovy.grails.plugins.PluginInfo
import grails.util.GrailsNameUtils

/**
 * Gant script that compiles Groovy and Java files in the src tree
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

includeTargets << grailsScript("_GrailsInit")

ant.taskdef (name: 'groovyc', classname : 'org.codehaus.groovy.grails.compiler.GrailsCompiler')
ant.path(id: "grails.compile.classpath", compileClasspath)

compilerPaths = { String classpathId, boolean compilingTests ->

	def excludedPaths = ["views", "i18n", "conf"] // conf gets special handling

	for(dir in new File("${basedir}/grails-app").listFiles()) {
        if(!excludedPaths.contains(dir.name) && dir.isDirectory())
            src(path:"${dir}")
    }
    // Handle conf/ separately to exclude subdirs/package misunderstandings
    src(path: "${basedir}/grails-app/conf")
    // This stops resources.groovy becoming "spring.resources"
    src(path: "${basedir}/grails-app/conf/spring")

    src(path:"${basedir}/src/groovy")
    src(path:"${basedir}/src/java")
    javac(classpathref:classpathId, encoding:"UTF-8", debug:"yes")
	if(compilingTests) {
        src(path:"${basedir}/test/unit")
        src(path:"${basedir}/test/integration")
	}
}

target(compile : "Implementation of compilation phase") {
    depends(compilePlugins)

    def classesDirPath = grailsSettings.classesDir.path
    ant.mkdir(dir:classesDirPath)

    profile("Compiling sources to location [$classesDirPath]") {
        try {
            String classpathId = "grails.compile.classpath"
            ant.groovyc(destdir:classesDirPath,
	                    classpathref:classpathId,
	                    encoding:"UTF-8",
	                    compilerPaths.curry(classpathId, false))
        }
        catch(Exception e) {
            event("StatusFinal", ["Compilation error: ${e.message}"])
            exit(1)
        }
        classLoader.addURL(grailsSettings.classesDir.toURI().toURL())

        // If this is a plugin project, the descriptor is not included
        // in the compiler's source path. So, we manually compile it
        // now.
        if (isPluginProject) compilePluginDescriptor(findPluginDescriptor(grailsSettings.baseDir))

    }
}

target(compilePlugins: "Compiles source files of all referenced plugins.") {
    depends(resolveDependencies)

    def classesDirPath = grailsSettings.classesDir.path
    ant.mkdir(dir:classesDirPath)

    profile("Compiling sources to location [$classesDirPath]") {
        // First compile the plugins so that we can exclude any
        // classes that might conflict with the project's.
        def classpathId = "grails.compile.classpath"
        def pluginResources = getPluginSourceFiles()
        def excludedPaths = ["views", "i18n"] // conf gets special handling
        pluginResources = pluginResources.findAll {
            !excludedPaths.contains(it.file.name) && it.file.isDirectory()
        }

        if (pluginResources) {
            // Only perform the compilation if there are some plugins
            // installed or otherwise referenced.
            ant.groovyc(destdir:classesDirPath,
                    classpathref:classpathId,
                    encoding:"UTF-8") {
                for(dir in pluginResources.file) {
                    src(path:"${dir}")
                }
                exclude(name: "**/BootStrap.groovy")
                exclude(name: "**/BuildConfig.groovy")
                exclude(name: "**/Config.groovy")
                exclude(name: "**/*DataSource.groovy")
                exclude(name: "**/UrlMappings.groovy")
                exclude(name: "**/resources.groovy")
                javac(classpathref:classpathId, encoding:"UTF-8", debug:"yes")
            }
        }
    }
}

/**
 * Compiles a given plugin descriptor file - *GrailsPlugin.groovy.
 */
compilePluginDescriptor = { File descriptor ->
    def className = descriptor.name - '.groovy'
    def classFile = new File(grailsSettings.classesDir, "${className}.class")

    if (descriptor.lastModified() > classFile.lastModified()) {
        ant.echo(message: "Compiling plugin descriptor...")
		compConfig.setTargetDirectory(classesDir)
        def unit = new CompilationUnit(compConfig, null, new GroovyClassLoader(classLoader))
        unit.addSource(descriptor)
        unit.compile()
    }
}

/**
 * Returns the first plugin descriptor it can find in the given directory,
 * or <code>null</code> if there is none.
 */
findPluginDescriptor = { File dir ->
    File[] files = dir.listFiles({ File d, String filename ->
        return filename.endsWith("GrailsPlugin.groovy")
    } as FilenameFilter)
    return files ? files[0] : null
}


target(compilepackage : "Compile & Compile GSP files") {
	depends(compile, compilegsp)
}

ant.taskdef (name: 'gspc', classname : 'org.codehaus.groovy.grails.web.pages.GroovyPageCompilerTask')

target(compilegsp : "Compile GSP files") {
	// compile gsps in grails-app/views directory
    File gspTmpDir = new File(grailsSettings.projectWorkDir, "gspcompile")
    ant.gspc( destdir:classesDir,
              srcdir:"${basedir}/grails-app/views",
              packagename:GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(grailsAppName),
              serverpath:"/WEB-INF/grails-app/views/",
              classpathref:"grails.compile.classpath",
              tmpdir:gspTmpDir)


	// compile gsps in web-app directory
    ant.gspc( destdir:classesDir,
              srcdir:"${basedir}/web-app",
              packagename:"${GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(grailsAppName)}_webapp",
              serverpath:"/",
              classpathref:"grails.compile.classpath",
              tmpdir:gspTmpDir)

	// compile views in plugins
	loadPlugins()
	def pluginInfos = GrailsPluginUtils.getSupportedPluginInfos(pluginsHome)
	if(pluginInfos) {
		for(PluginInfo info in pluginInfos) {
            File pluginViews = new File(info.pluginDir.file, "grails-app/views")
            if(pluginViews.exists()) {                
                def viewPrefix="/WEB-INF/plugins/${info.name}-${info.version}/grails-app/views/"
                ant.gspc( destdir:classesDir,
                          srcdir:pluginViews,
                          packagename:GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(info.name),
                          serverpath:viewPrefix,
                          classpathref:"grails.compile.classpath",
                          tmpdir:gspTmpDir)
            }
		}
	}
	
}

