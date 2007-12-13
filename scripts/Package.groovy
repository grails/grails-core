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
includeTargets << new File ( "${grailsHome}/scripts/PackagePlugins.groovy" ) 

scaffoldDir = "${basedir}/web-app/WEB-INF/templates/scaffolding"     
config = new ConfigObject()
configFile = new File("${basedir}/grails-app/conf/Config.groovy")
webXmlFile = new File("${userHome}/.grails/${grailsVersion}/projects/${baseName}/web.xml")
log4jFile = new File("${basedir}/web-app/WEB-INF/classes/log4j.properties")
generateLog4jFile = false

target ('default': "Packages a Grails application. Note: To create WAR use 'grails war'") {
     depends( checkVersion)
	 packagePlugins()	 
     packageApp()
}                     
  
target( createConfig: "Creates the configuration object") {
   def configSlurper = new ConfigSlurper(grailsEnv)
   if(configFile.exists()) { 
		try {              
            configSlurper.setBinding(grailsHome:grailsHome, appName:grailsAppName, appVersion:grailsAppVersion, userHome:userHome, basedir:basedir)			
			config = configSlurper.parse(classLoader.loadClass("Config"))
			config.setConfigFile(configFile.toURL())

            ConfigurationHolder.setConfig(config)
		}   
		catch(Exception e) {
            e.printStackTrace()
            
			event("StatusFinal", ["Failed to compile configuration file ${configFile}: ${e.message}"])
			exit(1)
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
	depends(createStructure)

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

	Ant.mkdir(dir:"${basedir}/web-app/WEB-INF/grails-app/i18n")

	Ant.mkdir(dir:"${basedir}/web-app/WEB-INF/grails-app/views")
	if(!GrailsUtil.isDevelopmentEnv() && shouldPackageTemplates) {
	    Ant.copy(todir:"${basedir}/web-app/WEB-INF/grails-app/views") {
			fileset(dir:"${basedir}/grails-app/views", includes:"**")
		} 
		packageTemplates()   						
	}
    Ant.copy(todir:"${basedir}/web-app/WEB-INF/grails-app/views") {
        fileset(dir:"${basedir}/grails-app/views", includes:"**/*.jsp")
    }

	if(config.grails.enable.native2ascii == true) {
		profile("converting native message bundles to ascii") {
			Ant.native2ascii(src:"${basedir}/grails-app/i18n",
							 dest:"${basedir}/web-app/WEB-INF/grails-app/i18n",
							 includes:"*.properties",
							 encoding:"UTF-8")   					
		}
	}                                        
	else {
	    Ant.copy(todir:"${basedir}/web-app/WEB-INF/grails-app/i18n") {
			fileset(dir:"${basedir}/grails-app/i18n", includes:"*.properties")
		}							
	}
    Ant.copy(todir:"${basedir}/web-app/WEB-INF/spring", failonerror:false) {
		fileset(dir:"${basedir}/grails-app/conf/spring", includes:"**")
	}					
    Ant.copy(todir:classesDirPath) {
		fileset(dir:"${basedir}", includes:"application.properties")
	}					
	Ant.copy(todir:classesDirPath, failonerror:false) {
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
    Log4jConfigurer.initLogging("file:${log4jFile.absolutePath}")
        
    loadPlugins()
    generateWebXml()
    event("PackagingEnd",[])
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
log4j.logger.org.codehaus.groovy.grails.plugins=info,stdout
log4j.logger.org.codehaus.groovy.grails.commons=info,stdout'''
	
}

target(loadPlugins:"Loads Grails' plugins") {
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
		profile("construct plugin manager") {
			def pluginClasses = []
			for(plugin in pluginFiles) {
			   def className = plugin.name - '.groovy'
               pluginClasses << classLoader.loadClass(className)
			}                              
			if(pluginClasses) {
				event("StatusUpdate", ["Loading with installed plug-ins: ${pluginClasses.name}"])				
			}                    
	        application = new DefaultGrailsApplication(new Class[0], new GroovyClassLoader(classLoader))
	        application.initialise()
            pluginManager = new DefaultGrailsPluginManager(pluginClasses as Class[], application)
            PluginManagerHolder.setPluginManager(pluginManager)            
        }
        profile("loading plugins") {
            pluginManager.loadPlugins()
            pluginManager.doArtefactConfiguration()
        } 
    } catch (Exception e) {
        event("StatusFinal", [ "Error loading plugin manager: " + e.message ])
		exit(1)
    }
}
target( generateWebXml : "Generates the web.xml file") {
	depends(classpath)

    webXml = new FileSystemResource("${basedir}/src/templates/war/web.xml")
    if(!webXml.exists()) {
    	def tmpWebXml = "${userHome}/.grails/${grailsVersion}/projects/${baseName}/web.xml.tmp"
    	Ant.copy(file:"${grailsHome}/src/war/WEB-INF/web${servletVersion}.template.xml", tofile:tmpWebXml)
    	
		Ant.replace(file:tmpWebXml, token:"@grails.project.key@", value:"${baseName}")
    	
       webXml = new FileSystemResource(tmpWebXml)
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
        e.printStackTrace(System.out)
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

