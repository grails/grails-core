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

/**
 * Gant script that packages a Grails application (note: does not create WAR)
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import groovy.text.SimpleTemplateEngine
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.*
import org.codehaus.groovy.grails.plugins.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.cfg.*
import org.codehaus.groovy.control.*
import org.springframework.util.Log4jConfigurer
import grails.util.*


Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Compile.groovy" )  
includeTargets << new File ( "${grailsHome}/scripts/_PackagePlugins.groovy" ) 

scaffoldDir = "${basedir}/web-app/WEB-INF/templates/scaffolding"     
configFile = new File("${basedir}/grails-app/conf/Config.groovy")
webXmlFile = new File("${resourcesDirPath}/web.xml")
log4jFile = new File("${resourcesDirPath}/log4j.properties")
generateLog4jFile = false

target ('default': "Packages a Grails application. Note: To create WAR use 'grails war'") {
     depends( checkVersion)

     packageApp()
}                     
  
target( createConfig: "Creates the configuration object") {
   if(configFile.exists()) {
       def configClass
       try {
           configClass = classLoader.loadClass("Config")
       } catch (ClassNotFoundException cnfe) {
           // ignore, empty config
       }
       if(configClass) {
           try {
               config = configSlurper.parse(configClass)
               config.setConfigFile(configFile.toURI().toURL())

               ConfigurationHolder.setConfig(config)
           }
           catch(Exception e) {
               e.printStackTrace()

               event("StatusFinal", ["Failed to compile configuration file ${configFile}: ${e.message}"])
               exit(1)
           }
       }
   } 
   def dataSourceFile = new File("${basedir}/grails-app/conf/DataSource.groovy")
   if(dataSourceFile.exists()) {
		try {
		   def dataSourceConfig = configSlurper.parse(classLoader.loadClass("DataSource"))
		   config.merge(dataSourceConfig)
		   ConfigurationHolder.setConfig(config)
		}
		catch(Exception e) {
			println "WARNING: DataSource.groovy not found, assuming dataSource bean is configured by Spring..."
		}
   }
   ConfigurationHelper.initConfig(config, null, classLoader)
}    
target( packageApp : "Implementation of package target") {
	depends(createStructure,packagePlugins)

	try {
        profile("compile") {
            compile()
        }
	}
	catch(Exception e) {
		event("StatusFinal", ["Compilation error: ${e.message}"])
		e.printStackTrace()
		exit(1)
	}
    profile("creating config") {
        createConfig()
    }

    // Get the application context path by looking for a property named 'app.context' in the following order of precedence:
    //	System properties
    //	application.properties
    //	config
    //	default to grailsAppName if not specified

    serverContextPath = System.getProperty("app.context")
    serverContextPath = serverContextPath ?: Ant.antProject.properties.'app.context'
    serverContextPath = serverContextPath ?: config.grails.app.context
    serverContextPath = serverContextPath ?: grailsAppName
    
    if(!serverContextPath.startsWith('/')) {
        serverContextPath = "/${serverContextPath}"
    }

    String i18nDir = "${resourcesDirPath}/grails-app/i18n"
    Ant.mkdir(dir:i18nDir)

    def files = Ant.fileScanner {
        fileset(dir:"${basedir}/grails-app/views", includes:"**/*.jsp")
    }

    if(files.iterator().hasNext()) {
        Ant.mkdir(dir:"${basedir}/web-app/WEB-INF/grails-app/views")
        Ant.copy(todir:"${basedir}/web-app/WEB-INF/grails-app/views") {
            fileset(dir:"${basedir}/grails-app/views", includes:"**/*.jsp")
        }
    }

	if(config.grails.enable.native2ascii == true) {
		profile("converting native message bundles to ascii") {
			Ant.native2ascii(src:"${basedir}/grails-app/i18n",
							 dest:i18nDir,
							 includes:"*.properties",
							 encoding:"UTF-8")   					
		}
	}                                        
	else {
	    Ant.copy(todir:i18nDir) {
			fileset(dir:"${basedir}/grails-app/i18n", includes:"*.properties")
		}							
	}
    Ant.copy(todir:classesDirPath) {
		fileset(dir:"${basedir}", includes:"application.properties")
	}					
	Ant.copy(todir:resourcesDirPath, failonerror:false) {
		fileset(dir:"${basedir}/grails-app/conf", includes:"**", excludes:"*.groovy, log4j*, hibernate, spring")
		fileset(dir:"${basedir}/grails-app/conf/hibernate", includes:"**/**")
		fileset(dir:"${basedir}/src/java") {
			include(name:"**/**")
			exclude(name:"**/*.java")
		}
	}           


    if(configFile.lastModified() > log4jFile.lastModified() || generateLog4jFile) {
        generateLog4j()
    }
    else if(!log4jFile.exists()) {
        createDefaultLog4J(log4jFile)
    }
    startLogging()
        
    loadPlugins()
    generateWebXml()
    event("PackagingEnd",[])
}

target(startLogging:"Bootstraps logging") {
    Log4jConfigurer.initLogging("file:${log4jFile.absolutePath}")    
}
target(generateLog4j:"Generates the Log4j config File") {
    profile("log4j-generation") {
        def log4jConfig = config.log4j
        try {
            if(log4jConfig) {
                if(log4jConfig instanceof ConfigObject) {
                    def props = log4jConfig.toProperties("log4j")
                    log4jFile.withOutputStream { out ->
                        props.store(out, "Grails' Log4j Configuration")
                    }
                }
                else {
                    log4jFile.write(log4jConfig.toString())
                }
            }
            else {
                // default log4j settings
                createDefaultLog4J(log4jFile)
            }
        }
        catch(Exception e) {
            event("StatusFinal", [ "Error creating Log4j config: " + e.message ])
            exit(1)
        }
    }
}
   
def createDefaultLog4J(logDest) {
	logDest <<  '''
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.rootLogger=error,stdout
'''
	
}

target(loadPlugins:"Loads Grails' plugins") {
    if(!PluginManagerHolder.pluginManager) { // plugin manager already loaded?
		compConfig.setTargetDirectory(classesDir)
	    def unit = new CompilationUnit ( compConfig , null , new GroovyClassLoader(classLoader) )	          
		def pluginFiles = pluginResources.file

		for(plugin in pluginFiles) {
	        def className = plugin.name - '.groovy'
	        def classFile = new File("${classesDirPath}/${className}.class")
	        if(plugin.lastModified() > classFile.lastModified())
	              unit.addSource ( plugin )		
		}

	    try {   
			profile("compiling plugins") {
	    		unit.compile ()								
			}
			def application
            def pluginClasses = []
            profile("construct plugin manager with ${pluginFiles.inspect()}") {
				for(plugin in pluginFiles) {
				   def className = plugin.name - '.groovy'
	               pluginClasses << classLoader.loadClass(className)
				}                              
				if(grailsApp == null) {
                    grailsApp = new DefaultGrailsApplication(new Class[0], new GroovyClassLoader(classLoader))
                }
                pluginManager = new DefaultGrailsPluginManager(pluginClasses as Class[], grailsApp)

                PluginManagerHolder.setPluginManager(pluginManager)
	        }
	        profile("loading plugins") {
				event("PluginLoadStart", [pluginManager])
	            pluginManager.loadPlugins()

                
                def loadedPlugins = pluginManager.allPlugins?.findAll { pluginClasses.contains(it.instance.getClass()) }*.name
                if(loadedPlugins)
                    event("StatusUpdate", ["Loading with installed plug-ins: ${loadedPlugins}"])

                pluginManager.doArtefactConfiguration()
                grailsApp.initialise()
                event("PluginLoadEnd", [pluginManager])
            }
	    }
        catch (Exception e) {
            GrailsUtil.deepSanitize(e).printStackTrace()
            event("StatusFinal", [ "Error loading plugin manager: " + e.message ])
			exit(1)
	    }
    }
    else {
        // Add the plugin manager to the binding so that it can be accessed
        // from any target.
        pluginManager = PluginManagerHolder.pluginManager
    }
}

target( generateWebXml : "Generates the web.xml file") {
	depends(classpath)
	
	if(config.grails.config.base.webXml) {
		def customWebXml =resolveResources(config.grails.config.base.webXml) 
		if(customWebXml)
			webXml = customWebXml[0]
		else {
			event("StatusError", [ "Custom web.xml defined in config [${config.grails.config.base.webXml}] could not be found." ])
			exit(1)			
		}        
	}
	else {
	    webXml = new FileSystemResource("${basedir}/src/templates/war/web.xml")
        def tmpWebXml = "${userHome}/.grails/${grailsVersion}/projects/${baseName}/web.xml.tmp"
        if(!webXml.exists()) {
	    	Ant.copy(file:"${grailsHome}/src/war/WEB-INF/web${servletVersion}.template.xml", tofile:tmpWebXml, overwrite:true)
        }
        else {
            Ant.copy(file:webXml.file, tofile:tmpWebXml, overwrite:true)
        }
        webXml = new FileSystemResource(tmpWebXml)
        Ant.replace(file:tmpWebXml, token:"@grails.project.key@", value:"${grailsAppName}-${grailsEnv}-${grailsAppVersion}")
    }
	def sw = new StringWriter()

    try {
        profile("generating web.xml from $webXml") {
			event("WebXmlStart", [webXml.filename])
            pluginManager.doWebDescriptor(webXml, sw)
            webXmlFile.withWriter {
                it << sw.toString()
            }         
			event("WebXmlEnd", [webXml.filename])
        }
    }
    catch(Exception e) {
        event("StatusError", [ e.message ])
		exit(1)
    }

}      

target(packageTemplates: "Packages templates into the app") {
	Ant.mkdir(dir:scaffoldDir)
	if(new File("${basedir}/src/templates/scaffolding").exists()) {
		Ant.copy(todir:scaffoldDir, overwrite:true) {
			fileset(dir:"${basedir}/src/templates/scaffolding", includes:"**")
		}			
	}   
	else {   
		Ant.copy(todir:scaffoldDir, overwrite:true) {
			fileset(dir:"${grailsHome}/src/grails/templates/scaffolding", includes:"**")
		}			
	}
	
}


// Checks whether the project's sources have changed since the last
// compilation, and then performs a recompilation if this is the case.
// Returns the updated 'lastModified' value.
recompileCheck = { lastModified, callback ->
    try {
        def ant = new AntBuilder()
        ant.taskdef ( 	name : 'groovyc' ,
                        classname : 'org.codehaus.groovy.grails.compiler.GrailsCompiler' )
        def grailsDir = resolveResources("file:${basedir}/grails-app/*")
        def pluginLibs = resolveResources("file:${basedir}/plugins/*/lib")
        ant.path(id:"grails.classpath",grailsClasspath.curry(pluginLibs, grailsDir))

        ant.groovyc(destdir:classesDirPath,
                    classpathref:"grails.classpath",
                    resourcePattern:"file:${basedir}/**/grails-app/**/*.groovy",
                    encoding:"UTF-8",
                    projectName:baseName) {
                    src(path:"${basedir}/src/groovy")
                    src(path:"${basedir}/grails-app/domain")
                    src(path:"${basedir}/grails-app/utils")
                    src(path:"${basedir}/src/java")
                    javac(classpathref:"grails.classpath", debug:"yes")

                }
        ant = null
    }
    catch(Exception e) {
        compilationError = true
        event("StatusUpdate", ["Error automatically restarting container: ${e.message}"])
        GrailsUtil.sanitize(e)
        e.printStackTrace()
    }

    def tmp = classesDir.lastModified()
    if(lastModified < tmp) {

        // run another compile JIT
        try {
            callback()
        }
        catch(Exception e) {
            event("StatusUpdate", ["Error automatically restarting container: ${e.message}"])
            e.printStackTrace()
        }

        finally {
           lastModified = classesDir.lastModified()
        }
    }

    return lastModified
}
