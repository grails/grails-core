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
import org.codehaus.groovy.control.*    
import grails.util.*


Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Compile.groovy" )  
includeTargets << new File ( "${grailsHome}/scripts/PackagePlugins.groovy" ) 

scaffoldDir = "${basedir}/web-app/WEB-INF/templates/scaffolding"     
config = new ConfigObject()
configFile = new File("${basedir}/grails-app/conf/Config.groovy")
webXmlFile = new File("${basedir}/web-app/WEB-INF/web.xml")

task ('default': "Packages a Grails application. Note: To create WAR use 'grails war'") {
     depends( checkVersion)
	 packagePlugins()	 
     packageApp()                           
}                     
  
task( createConfig: "Creates the configuration object") {
   def configSlurper = new ConfigSlurper(grailsEnv)
   if(configFile.exists()) { 
		try {
			config = configSlurper.parse(classLoader.loadClass("Config"))
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
            e.printStackTrace()
            
            event("StatusFinal", ["Failed to compile data source file $dataSourceFile: ${e.message}"])
			exit(1)
		}
   }
}    
task( packageApp : "Implementation of package task") {
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

	profile("dependencies") {

        copyDependencies()
    }
	Ant.mkdir(dir:"${basedir}/web-app/WEB-INF/grails-app/i18n")
	
	if(!GrailsUtil.isDevelopmentEnv()) {
		Ant.mkdir(dir:"${basedir}/web-app/WEB-INF/grails-app/views")		
	    Ant.copy(todir:"${basedir}/web-app/WEB-INF/grails-app/views") {
			fileset(dir:"${basedir}/grails-app/views", includes:"**")
		} 
		packageTemplates()   						
	}	   
	if(config.grails.enable.native2ascii == true) {
		Ant.native2ascii(src:"${basedir}/grails-app/i18n",
						 dest:"${basedir}/web-app/WEB-INF/grails-app/i18n",
						 includes:"*.properties",
						 encoding:"UTF-8")   		
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

	def logDest = new File("${basedir}/web-app/WEB-INF/classes/log4j.properties")

    profile("log4j-creation") {

        if(configFile.lastModified() > logDest.lastModified()) {

            def log4jConfig = config.log4j
        try {
            if(log4jConfig) {
                def props = log4jConfig.toProperties("log4j")
                logDest.withOutputStream { out ->
                    props.store(out, "Grails' Log4j Configuration")
                }
            }
            else {
                // default log4j settings
                logDest <<  '''
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.rootLogger=error,stdout
log4j.logger.org.codehaus.groovy.grails.plugins=info,stdout
log4j.logger.org.codehaus.groovy.grails.commons=info,stdout
                '''
            }
        }
        catch(Exception e) {
            println e.message
             e.printStackTrace()
        }
    }
    }

    loadPlugins()
    if(!webXmlFile.exists()) {
        generateWebXml()
    }

}   

DEPENDENCIES = [
"ejb-3.0-persistence.jar",
"ant.jar",  
"hibernate3.jar",
"jdbc2_0-stdext.jar",
"jta.jar",
"groovy-all-*.jar",
"springmodules-sandbox.jar",
"spring-webflow.jar",
"spring-binding.jar",
"standard-${servletVersion}.jar",
"jstl-${servletVersion}.jar",          
"antlr-*.jar",
"cglib-*.jar",
"dom4j-*.jar", 
"ehcache-*.jar", 
"junit-*.jar", 
"commons-logging-*.jar",
"sitemesh-*.jar",
"spring-*.jar",
"commons-lang-*.jar",
"log4j-*.jar",
"ognl-*.jar",
"hsqldb-*.jar",
"commons-collections-*.jar",
"commons-beanutils-*.jar",
"commons-pool-*.jar",
"commons-dbcp-*.jar",
"commons-cli-*.jar",
"commons-validator-*.jar",
"commons-fileupload-*.jar",
"commons-io-*.jar", 
"commons-io-*.jar",  
"*oro-*.jar",    
"jaxen-*.jar",
"xstream-1.2.1.jar",
"xpp3_min-1.1.3.4.O.jar"
]    
JAVA_5_DEPENDENCIES = [        
"hibernate-annotations.jar",
"ejb3-persistence.jar",	
]                                      

task( copyDependencies : "Copies the necessary dependencies (jar files) into the lib dir") {
	Ant.sequential {
		mkdir(dir:"${basedir}/web-app/WEB-INF/lib")
		mkdir(dir:"${basedir}/web-app/WEB-INF/spring")
		mkdir(dir:"${basedir}/web-app/WEB-INF/tld")
		copy(todir:"${basedir}/web-app/WEB-INF/lib") {
			fileset(dir:"${grailsHome}/lib") {
				for(d in DEPENDENCIES) {
					include(name:d)
				}
				if(antProject.properties."ant.java.version" == "1.5") {
					for(d in JAVA_5_DEPENDENCIES) {
						include(name:d)
					}
				}				
			}  
			fileset(dir:"${basedir}/lib")
		}   
	}
}

task(loadPlugins:"Loads Grails' plugins") {
    def classLoader = new GroovyClassLoader(rootLoader,compConfig,true)

    try {
        pluginManager = new DefaultGrailsPluginManager(pluginResources as Resource[], new DefaultGrailsApplication(new Class[0], classLoader))
        PluginManagerHolder.setPluginManager(pluginManager)
        profile("loading plugins") {
            pluginManager.loadPlugins()
        }
    } catch (Exception e) {
        event("StatusError", [ e.message ])
        e.printStackTrace(System.out)
        
    }



}
task( generateWebXml : "Generates the web.xml file") {                
	depends(classpath)

    boolean fileExists = webXmlFile.exists()

    def webXml = new FileSystemResource(webXmlFile)
    if(!fileExists) webXml = resolver.getResource("file:${grailsHome}/src/war/WEB-INF/web${servletVersion}.template.xml")

	def sw = new StringWriter()

    println "Generating from ${webXml.file.absolutePath}"

    try {
        profile("generating web.xml") {
            pluginManager.doWebDescriptor(webXml, sw)
            webXmlFile.withWriter {
                it << sw.toString()
            }         
        }
    }
    catch(Exception e) {
        event("StatusError", [ e.message ])
        e.printStackTrace(System.out)
    }

}      

task(packageTemplates: "Packages templates into the app") {  
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

