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
 * Gant script that creates a new Grails controller
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import groovy.text.SimpleTemplateEngine
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.codehaus.groovy.grails.plugins.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.control.*


Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )  
includeTargets << new File ( "${grailsHome}/scripts/Compile.groovy" )  
includeTargets << new File ( "${grailsHome}/scripts/PackagePlugins.groovy" )      

task ('default': "Packages a Grails application. Note: To create WAR use 'grails war'") {	 
	 packagePlugins()	 
     packageApp()     
}                     
      
task( packageApp : "Implementation of package task") {
	depends(createStructure,compile)
	copyDependencies()
    Ant.delete(dir:"${basedir}/web-app/WEB-INF/grails-app", failonerror:true)	
	Ant.mkdir(dir:"${basedir}/web-app/WEB-INF/grails-app/views")
	Ant.mkdir(dir:"${basedir}/web-app/WEB-INF/grails-app/i18n")
		
	Ant.copy(todir:"${basedir}/web-app/WEB-INF/grails-app/views") {
		fileset(dir:"${basedir}/grails-app/views", includes:"**") 
	} 
	Ant.native2ascii(src:"${basedir}/grails-app/i18n",
					 dest:"${basedir}/web-app/WEB-INF/grails-app/i18n",
					 includes:"messages.properties",
					 encoding:"UTF-8")   
    Ant.copy(todir:"${basedir}/web-app/WEB-INF/spring") {
		fileset(dir:"${basedir}/spring", includes:"**")
	}					
	Ant.copy(todir:"${basedir}/web-app/WEB-INF/classes") {
		fileset(dir:"${basedir}/hibernate", includes:"**")
		fileset(dir:"${basedir}/src/java") {
			include(name:"**/**")
			exclude(name:"**/*.java")
		}
	}           
	def logFile = "${basedir}/grails-app/conf/log4j.${grailsEnv}.properties"
	def logDest = "${basedir}/web-app/WEB-INF/log4j.properties"
	if(new File(logFile).exists()) {
		Ant.copy(file:logFile, tofile:logDest)
	}
	else {
		Ant.copy(file:"${grailsHome}/src/war/WEB-INF/log4j.properties", tofile:logDest)
	}
	
	generateWebXml()
}   

DEPENDENCIES = [
"ejb-3.0-persistence.jar",
"ant.jar",  
"hibernate3.jar",
"jdbc2_0-stdext.jar",
"jta.jar",
"groovy-all-1.0.1-SNAPSHOT.jar",
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
"*oro-*.jar",
"quartz-*.jar",            	            	
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

task( generateWebXml : "Generates the web.xml file") {
    new File( "${basedir}/web-app/WEB-INF/web.xml" ).withWriter { w ->
    
        StringBuffer cpath = new StringBuffer("")
        //println "Generating web.xml generator classpath: "
        def jarFiles = []
        try {
            jarFiles = resolver.getResources("lib/*.jar").toList()
        }
        catch(FileNotFoundException e) {
            // ignore
        }

        try {
            resolver.getResources("plugins/*/lib/*.jar").each { pluginJar ->  
				println("Adding Plugin jar $pluginJar to classpath")
                boolean matches = jarFiles.any { it.file.name == pluginJar.file.name }
                if(!matches) jarFiles.add(pluginJar)
            }
        }
        catch(FileNotFoundException e) {
            // ignore
        }

        def rootLoader = getClass().classLoader.rootLoader

        jarFiles.each { jar ->
            cpath << jar.file.absolutePath << File.pathSeparator
            rootLoader?.addURL(jar.URL)       
        }
        cpath << "${basedir}/web-app/WEB-INF/classes"
		rootLoader?.addURL(new File("${basedir}/web-app/WEB-INF/classes").toURL())
        cpath << "${basedir}/web-app/WEB-INF"
		rootLoader?.addURL(new File("${basedir}/web-app/WEB-INF").toURL())
    
   //     println "Classpath with which to generate web.xml: \n${cpath.toString()}"
        
    	def parentLoader = getClass().getClassLoader()
    	def compConfig = new CompilerConfiguration()
    	compConfig.setClasspath(cpath.toString());

        def classLoader = new GroovyClassLoader(parentLoader,compConfig,true)

        pluginManager = new DefaultGrailsPluginManager("classpath:plugins/**/*GrailsPlugin.groovy", new DefaultGrailsApplication(new Class[0], classLoader))
    	PluginManagerHolder.setPluginManager(pluginManager)

    	def webXml = resolver.getResource("classpath:web-app/WEB-INF/web.template.xml")
		try {
    		pluginManager.loadPlugins()  			
	    	pluginManager.doWebDescriptor(webXml, w)			
		}   
		catch(Exception e) {
			println e.message
			e.printStackTrace(System.out)
		}
    }
}  

