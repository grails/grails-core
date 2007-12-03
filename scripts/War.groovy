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

import org.codehaus.groovy.grails.compiler.support.*

/**
 * Gant script that creates a WAR file from a Grails project
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */

Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    
                              

includeTargets << new File ( "${grailsHome}/scripts/Clean.groovy" ) 
includeTargets << new File ( "${grailsHome}/scripts/Package.groovy" )

task ('default':'''Creates a WAR archive for deployment onto a Java EE application server.

Examples: 
grails war
grails prod war
''') {
    depends( checkVersion)

	war()
} 

generateLog4jFile = true

target (war: "The implementation target") {
	depends( clean, packagePlugins, packageApp, generateWebXml )
	 
	try {
		Ant.mkdir(dir:"${basedir}/staging")

		Ant.copy(todir:"${basedir}/staging", overwrite:true) {
			fileset(dir:"${basedir}/web-app", includes:"**") 
		}       
		Ant.copy(todir:"${basedir}/staging/WEB-INF/grails-app", overwrite:true) {
			fileset(dir:"${basedir}/grails-app", includes:"views/**")
		}
		Ant.copy(todir:"${basedir}/staging/WEB-INF/classes") {
            fileset(dir:classesDirPath)
        }
		              
		scaffoldDir = "${basedir}/staging/WEB-INF/templates/scaffolding"
		packageTemplates()

		println "DEPENDENCIES = $config.grails.war.dependencies"

		Ant.copy(todir:"${basedir}/staging/WEB-INF/lib") {
			fileset(dir:"${grailsHome}/dist") {
					include(name:"grails-*.jar")
			}
			fileset(dir:"${basedir}/lib") {
					include(name:"*.jar")
			}
            fileset(dir:"${grailsHome}/lib") {
                for(d in config.grails.war.dependencies) {
                    include(name:d)
                }
                if(antProject.properties."ant.java.version" == "1.5") {
                    for(d in config.grails.war.java5.dependencies) {
                        include(name:d)
                    }
                }
            }
            if(antProject.properties."ant.java.version" == "1.4") {
                fileset(dir:"${basedir}/lib/endorsed") {
                        include(name:"*.jar")
                }
            }
		}                 
		Ant.copy(file:webXmlFile.absolutePath, tofile:"${basedir}/staging/WEB-INF/web.xml")
		Ant.copy(file:log4jFile.absolutePath, tofile:"${basedir}/staging/WEB-INF/log4j.properties")
        Ant.copy(todir:"${basedir}/staging/WEB-INF/lib", flatten:true, failonerror:false) {
			fileset(dir:"${basedir}/plugins") {
                include(name:"*/lib/*.jar")
            }
        }
		  
	    Ant.propertyfile(file:"${basedir}/staging/WEB-INF/classes/application.properties") {
	        entry(key:"grails.env", value:grailsEnv)
	    }		
		
		Ant.replace(file:"${basedir}/staging/WEB-INF/applicationContext.xml", 
				token:"classpath*:", value:"" ) 

		def fileName = grailsAppName
		def version = Ant.antProject.properties.'app.version'
		if (version) {
		    version = '-'+version
		} else {
		    version = ''
		}
		warName = "${basedir}/${fileName}${version}.war"
		warPlugins()		    
		createDescriptor()
		Ant.jar(destfile:warName, basedir:"${basedir}/staging")
		
	}   
	finally {
		cleanUpAfterWar()
	}
    event("StatusFinal", ["Created WAR ${warName}"])
}                                                                    
  
target(createDescriptor:"Creates the WEB-INF/grails.xml file used to load Grails classes in WAR mode") {
     def resourceList = GrailsResourceLoaderHolder.resourceLoader.getResources()
     def pluginList = resolveResources("file:${basedir}/plugins/*/*GrailsPlugin.groovy")
      
	 new File("${basedir}/staging/WEB-INF/grails.xml").withWriter { writer ->
		def xml = new groovy.xml.MarkupBuilder(writer)
		xml.grails {
			resources {
			   for(r in resourceList) {
				    def matcher = r.URL.toString() =~ /\S+?\/grails-app\/\S+?\/(\S+?).groovy/
					def name = matcher[0][1].replaceAll('/', /\./)
					resource(name)
			   } 
			}
			plugins {
                for(p in pluginList) {

                    def name = p.file.name - '.groovy'
                    plugin(name)
                }
            }
		}
	 }
	 
}

target(cleanUpAfterWar:"Cleans up after performing a WAR") {
	Ant.delete(dir:"${basedir}/staging", failonerror:true)
}

target(warPlugins:"Includes the plugins in the WAR") {
	Ant.sequential {
		mkdir(dir:"${basedir}/staging/WEB-INF/plugins")
		copy(todir:"${basedir}/staging/WEB-INF/plugins", failonerror:false) {
			fileset(dir:"${basedir}/plugins")  {    
				include(name:"**/*plugin.xml")
			}
		}
	}
}

