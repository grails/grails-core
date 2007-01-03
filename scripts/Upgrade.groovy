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
			
		copy(todir:"${basedir}/grails-app") {
			fileset(dir:"${grailsHome}/src/grails/grails-app") {
                present(present:"srconly", targetdir:"${basedir}/grails-app")
            }
		}

		copy(todir:"${basedir}/grails-app/taglib") {
			fileset(dir:"${grailsHome}/src/grails/grails-app/taglib") {
                present(present:"srconly", targetdir:"${basedir}/grails-app/taglib")
            }
		}
						   	 
		copy(tofile:"${basedir}/web-app/WEB-INF/web.template.xml") {
		    fileset(file:"${grailsHome}/src/war/WEB-INF/web${servletVersion}.template.xml") {
                //present(present:"srconly", targetdir:"${basedir}/web-app/WEB-INF")
            }
        }

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

