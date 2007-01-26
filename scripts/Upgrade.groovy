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
 * Gant script that handles upgrading of a Grails applications
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */    
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU

Ant.property(environment:"env")       
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"   

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )

task( upgrade: "main upgrade task") {
	depends( createStructure )    
   
	 Ant.input(message: """
	WARNING: This task will upgrade an older Grails application to ${grailsVersion}.
	However, tag libraries provided by earlier versions of Grails found in grails-app/taglib will be removed. 
	The task will not, however, delete tag libraries developed by yourself.
	Are you sure you want to continue? 
			   """,
			validargs:"y,n", 
			addproperty:"grails.upgrade.warning")

       def answer = Ant.antProject.properties."grails.upgrade.warning"        

	if(answer == "n") System.exit(0)
	createCorePlugin()
	
	def coreTaglibs = new File("${basedir}/plugins/core/grails-app/taglib")
	assert coreTaglibs.exists()   
	coreTaglibs.eachFile { f ->
		if(!f.isDirectory())
			Ant.delete(file:"${basedir}/grails-app/taglib/${f.name}")
	}                      
	def coreUtils = new File("${basedir}/plugins/core/grails-app/utils")	
	coreUtils.eachFile { f ->
		if(!f.isDirectory())
			Ant.delete(file:"${basedir}/grails-app/utils/${f.name}")
	}
	
	Ant.sequential {       
   	   		
        delete(dir:"${basedir}/tmp", failonerror:false)
		copy(todir:"${basedir}/web-app") {
			fileset(dir:"${grailsHome}/src/war") {
				include(name:"**/**")
				exclude(name:"WEB-INF/**")
                present(present:"srconly", targetdir:"${basedir}/web-app")
			} 
		}
		copy(file:"${grailsHome}/src/war/WEB-INF/sitemesh.xml",
			 tofile:"${basedir}/web-app/WEB-INF/sitemesh.xml", overwrite:true)
		copy(file:"${grailsHome}/src/war/WEB-INF/applicationContext.xml",
			 tofile:"${basedir}/web-app/WEB-INF/applicationContext.xml", overwrite:true)

		// These will still overwrite as the src name differs from target and the src name is not in target
		copy(tofile:"${basedir}/grails-app/conf/log4j.development.properties") {
		    fileset(file:"${grailsHome}/src/war/WEB-INF/log4j.properties") {
                present(present:"srconly", targetdir:"${basedir}/grails-app/conf")
            }
        }
		copy(tofile:"${basedir}/grails-app/conf/log4j.test.properties") {
		    fileset(file:"${grailsHome}/src/war/WEB-INF/log4j.properties") {
                present(present:"srconly", targetdir:"${basedir}/grails-app/conf")
            }
        }
		copy(tofile:"${basedir}/grails-app/conf/log4j.production.properties") {
		    fileset(file:"${grailsHome}/src/war/WEB-INF/log4j.properties") {
                present(present:"srconly", targetdir:"${basedir}/grails-app/conf")
            }
        }
			
		copy(file:"${grailsHome}/src/war/WEB-INF/web${servletVersion}.template.xml", 
				 tofile:"${basedir}/web-app/WEB-INF/web.template.xml",
				 overwrite:"true") 

	    def appKey = baseName.replaceAll( /\s/, '.' ).toLowerCase()

		replace(dir:"${basedir}/web-app/WEB-INF", includes:"**/*.*",
		    		token:"@grails.project.key@", value:"${appKey}" )				


		if(servletVersion != "2.3") {
			replace(file:"${basedir}/web-app/index.jsp", token:"http://java.sun.com/jstl/core",
					value:"http://java.sun.com/jsp/jstl/core")
		}  
		
		copy(todir:"${basedir}/web-app/WEB-INF/tld", overwrite:true) {
			fileset(dir:"${grailsHome}/src/war/WEB-INF/tld/${servletVersion}")	
			fileset(dir:"${grailsHome}/src/war/WEB-INF/tld", includes:"spring.tld")
			fileset(dir:"${grailsHome}/src/war/WEB-INF/tld", includes:"grails.tld")			
		}	 
		copy(todir:"${basedir}/spring") {
			fileset(dir:"${grailsHome}/src/war/WEB-INF/spring") {
				include(name:"*.xml")
				include(name:"*.groovy")    				
			}
		}  
		touch(file:"${basedir}/grails-app/i18n/messages.properties") 
	}
}

task("default": "Upgrades a Grails application from a previous version of Grails") {
	depends( upgrade )
}  

