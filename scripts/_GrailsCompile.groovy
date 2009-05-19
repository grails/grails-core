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
    javac(classpathref:classpathId, debug:"yes")
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
                    projectName:grailsSettings.baseDir.name,
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
                    projectName:grailsSettings.baseDir.name,
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
                javac(classpathref:classpathId, debug:"yes")
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


def compileGSPClassloader = null

def compileGSPRegistry = null

target(compilepackage : "Compile & Compile GSP files") {
	depends(compile, compilegsp)
}

target(compilegsp : "Compile GSP files") {
	compileGSPClassloader=new GroovyClassLoader(classLoader) 
	compConfig.setTargetDirectory(classesDir)
	compileGSPRegistry = [:]
	
	// compile gsps in grails-app/views directory
	compileGSPFiles(new File("${basedir}/grails-app/views"), "/WEB-INF/grails-app/views/", grailsAppName)

	// compile gsps in web-app directory
	compileGSPFiles(new File("${basedir}/web-app"), "/", grailsAppName + "_webapp")

	// compile views in plugins
	loadPlugins()
	def pluginInfos = GrailsPluginUtils.getSupportedPluginInfos(pluginsHome)
	if(pluginInfos) {
		for(PluginInfo info in pluginInfos) {
			def viewPrefix="/WEB-INF/plugins/${info.name}-${info.version}/grails-app/views/"
			compileGSPFiles(new File(info.pluginDir.file, "grails-app/views"), viewPrefix, info.name)
		}
	}
	
	compileGSPClassloader=null

	// write the view registry to a properties file (this is read by GroovyPagesTemplateEngine at runtime)
	File viewregistryFile=new File(classesDir, "gsp/views.properties")
	Properties views=new Properties()
	if(viewregistryFile.exists()) {
		// only changed files are added to the mapping, read the existing mapping file
		def propinput=new FileInputStream(viewregistryFile)
		views.load(propinput)
		propinput.close()
	}	
	views.putAll(compileGSPRegistry)
	def viewsOut=new FileOutputStream(viewregistryFile)
	views.store(viewsOut, "Precompiled views for ${grailsAppName}")
	viewsOut.close()
}

compileGSPFiles = { File viewsDir, String viewPrefix, String packagePrefix ->
	if(viewsDir.exists()) {
		def gspfiles = ant.fileScanner {
			fileset(dir:viewsDir, includes:"**/*.gsp")
		}
		gspfiles.each {
			compileGSP(viewsDir, it, viewPrefix, generateJavaName(packagePrefix))    
		}
	}
}

// precompiles a single gsp file
compileGSP = { File viewsDir, File gspfile, String viewPrefix, String packagePrefix ->
	def gspgroovydir = new File(grailsSettings.projectWorkDir, "gspcompile")
	
	def relPath = relativePath(viewsDir, gspfile)
	def viewuri = viewPrefix + relPath
	
	def relPackagePath = relativePath(viewsDir, gspfile.getParentFile())
	def packageDir = "gsp/${packagePrefix}"
	if(relPackagePath.length() > 0) {
		packageDir += "/" + generateJavaName(relPackagePath)
	}

	def className = generateJavaName(gspfile.name - '.gsp')
	def classFile = new File(new File(grailsSettings.classesDir, packageDir), "${className}.class")

	// compile check
	if (gspfile.lastModified() > classFile.lastModified()) {
		ant.echo(message: "Compiling gsp ${gspfile}...")

		def packageName = packageDir.replace('/','.')
		
		def gspgroovyfile = new File(new File(gspgroovydir, packageDir), className + ".groovy")
		gspgroovyfile.getParentFile().mkdirs()
		
		InputStream gspinput=new FileInputStream(gspfile)
		org.codehaus.groovy.grails.web.pages.GroovyPageParser gpp=new org.codehaus.groovy.grails.web.pages.GroovyPageParser(viewuri - '.gsp', viewuri, gspinput)
		gpp.packageName = packageName
		gpp.className = className
		gpp.lastModified = gspfile.lastModified()
		Writer gsptarget=new FileWriter(gspgroovyfile)
		// generate gsp groovy source
		gpp.generateGsp(gsptarget)
		gsptarget.close()
		gspinput.close()
		// write static html parts to data file (read from classpath at runtime)
		def htmlDataFile = new File(new File(grailsSettings.classesDir, packageDir),  className + org.codehaus.groovy.grails.web.pages.GroovyPageMetaInfo.HTML_DATA_POSTFIX)
		htmlDataFile.getParentFile().mkdirs()
		gpp.writeHtmlParts(htmlDataFile)
		// write linenumber mapping info to data file
		def lineNumbersDataFile = new File(new File(grailsSettings.classesDir, packageDir),  className + org.codehaus.groovy.grails.web.pages.GroovyPageMetaInfo.LINENUMBERS_DATA_POSTFIX)
		gpp.writeLineNumbers(lineNumbersDataFile)
		
		// register viewuri -> classname mapping
		compileGSPRegistry[viewuri] = packageName + "." + className
	
		def unit = new CompilationUnit(compConfig, null, compileGSPClassloader)
		unit.addSource(gspgroovyfile)
		unit.compile()
	}
}

// find out the relative path from relbase to file
relativePath = { File relbase, File file ->
	def pathParts = []
	def currentFile = file
	while(currentFile != null && currentFile != relbase) {
		pathParts += currentFile.name
		currentFile = currentFile.parentFile
	}
	pathParts.reverse().join('/')
}

generateJavaName = { String str ->
	StringBuffer sb = new StringBuffer()
	int i = 0
	char ch = str.charAt(i)
	if (Character.isJavaIdentifierStart(ch)) {
		sb.append(ch)
		i++
	} else if (Character.isJavaIdentifierPart(ch)) {
		sb.append('_')
	}
	while (i < str.length()) {
		ch = str.charAt(i++)
		sb.append((ch=='/' || Character.isJavaIdentifierPart(ch)) ? ch : '_')
	}
	sb.toString()
}
