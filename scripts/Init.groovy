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
 * Gant script that handles general initialization of a Grails applications
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */    
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU

Ant.property(environment:"env")       

servletVersion = System.getProperty("servlet.version") ? System.getProperty("servlet.version") : "2.4"
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"   
Ant.property(file:"${grailsHome}/build.properties")

grailsVersion =  Ant.antProject.properties.'grails.version'
grailsEnv = System.getProperty("grails.env") 
defaultEnv = System.getProperty("grails.default.env") == "true" ? true : false 
serverPort = System.getProperty('server.port') ? System.getProperty('server.port').toInteger() : 8080   
basedir = System.getProperty("base.dir")    
baseFile = new File(basedir)
baseName = baseFile.name

args = System.getProperty("grails.cli.args")     

task ( createStructure: "Creates the application directory structure") {
	Ant.sequential {
	    mkdir(dir:"${basedir}/src")
	    mkdir(dir:"${basedir}/src/java")
	    mkdir(dir:"${basedir}/src/groovy")
	    mkdir(dir:"${basedir}/grails-app")
	    mkdir(dir:"${basedir}/grails-app/controllers")
	    mkdir(dir:"${basedir}/grails-app/services")
	    mkdir(dir:"${basedir}/grails-app/domain")
	    mkdir(dir:"${basedir}/grails-app/taglib")
	    mkdir(dir:"${basedir}/grails-app/views")
	    mkdir(dir:"${basedir}/grails-app/views/layouts")
	    mkdir(dir:"${basedir}/grails-app/i18n")
	    mkdir(dir:"${basedir}/grails-app/conf")
	    mkdir(dir:"${basedir}/grails-tests")
	    mkdir(dir:"${basedir}/web-app")
	    mkdir(dir:"${basedir}/web-app")
	    mkdir(dir:"${basedir}/web-app")
	    mkdir(dir:"${basedir}/web-app/js")
	    mkdir(dir:"${basedir}/web-app/css")
	    mkdir(dir:"${basedir}/web-app/images")
	    mkdir(dir:"${basedir}/web-app/WEB-INF/classes")
	    mkdir(dir:"${basedir}/web-app/META-INF")
	    mkdir(dir:"${basedir}/lib")
	    mkdir(dir:"${basedir}/spring")
	    mkdir(dir:"${basedir}/hibernate") 
	}
}  

task( init: "main init task") {
	depends( createStructure )
	Ant.sequential {
		copy(todir:"${basedir}/web-app") {
			fileset(dir:"${grailsHome}/src/war") {
				include(name:"**/**")
				exclude(name:"WEB-INF/**")
			} 
		}
		copy(todir:"${basedir}/web-app/WEB-INF") {
			fileset(dir:"${grailsHome}/src/war/WEB-INF") {
				include(name:"applicationContext.xml")
				exclude(name:"log4j.properties")
				include(name:"sitemesh.xml")								
			}			
		}
		
		copy(file:"${grailsHome}/src/war/WEB-INF/log4j.properties",
			 tofile:"${basedir}/grails-app/conf/log4j.development.properties")
		copy(file:"${grailsHome}/src/war/WEB-INF/log4j.properties",
			 tofile:"${basedir}/grails-app/conf/log4j.test.properties")
		copy(file:"${grailsHome}/src/war/WEB-INF/log4j.properties",
			 tofile:"${basedir}/grails-app/conf/log4j.production.properties")	
			
		copy(todir:"${basedir}/grails-app") {
			fileset(dir:"${grailsHome}/src/grails/grails-app")
		}		 
		copy(file:"${grailsHome}/src/war/WEB-INF/web${servletVersion}.template.xml", 
			 tofile:"${basedir}/web-app/WEB-INF/web.template.xml") 
			
		if(servletVersion != "2.3") {
			replace(file:"${basedir}/web-app/index.jsp", token:"http://java.sun.com/jstl/core",
					value:"http://java.sun.com/jsp/jstl/core")
		}  
		
		copy(todir:"${basedir}/web-app/WEB-INF/tld") {
			fileset(dir:"${grailsHome}/src/war/WEB-INF/tld/${servletVersion}")	
			fileset(dir:"${grailsHome}/src/war/WEB-INF/tld", includes:"spring.tld")
			fileset(dir:"${grailsHome}/src/war/WEB-INF/tld", includes:"grails.tld")			
		}	 
		copy(todir:"${basedir}/spring") {
			fileset(dir:"${grailsHome}/src/war/WEB-INF/spring") {
				include(name:"*.xml")
			}
		}  
		touch(file:"${basedir}/grails-app/i18n/messages.properties") 
	}
}

task("default": "Initializes a Grails application. Warning: This task will overwrite artifacts,use the 'upgrade' task for upgrades.") {
	depends( init )
}  

task ('createArtifact': "Creates a specific Grails artifact") {
	depends(promptForName)
	
	Ant.mkdir(dir:"${basedir}/${artifactPath}")
	
	className = GCU.getClassNameRepresentation(args)
	propertyName = GCU.getPropertyNameRepresentation(args)
	artifactFile = "${basedir}/${artifactPath}/${className}${typeName}.groovy"
	         
	if(new File(artifactFile).exists()) {
		Ant.input(addProperty:"${args}.${typeName}.overwrite", message:"${typeName} ${className}${typeName}.groovy already exists. Overwrite? [y/n]")
		if(Ant.antProject.properties."${args}.${typeName}.overwrite" == "n")
			return
	}
		
	Ant.copy(file:"${grailsHome}/src/grails/templates/artifacts/${artifactName}.groovy", 
			 tofile:artifactFile)
			
	Ant.replace(file:artifactFile, 
				token:"@artifact.name@", value:className )
				
	println "Created ${typeName} at ${artifactFile}"
}  

task(promptForName:"Prompts the user for the name of the Artifact if it isn't specified as an argument") {
	if(!args) {
		Ant.input(addProperty:"artifact.name", message:"${typeName} name not specified. Please enter:")
		args = Ant.antProject.properties."artifact.name"
	}          	
}

task(classpath:"Sets the Grails classpath") {
	Ant.path(id:"grails.classpath")  {
		pathelement(location:"${basedir}") 		
		pathelement(location:"${basedir}/grails-tests")		
		pathelement(location:"${basedir}/web-app")
		pathelement(location:"${basedir}/web-app/WEB-INF")
		pathelement(location:"${basedir}/web-app/classes")				
		fileset(dir:"${grailsHome}/lib")
		fileset(dir:"${grailsHome}/dist")
		fileset(dir:"lib")		
	}
}