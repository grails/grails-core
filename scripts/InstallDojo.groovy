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
 * Gant script that installs Dojo
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU

Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )  

task ('default': "Installs the Dojo toolkit. An advanced Javascript library.") {
    depends(checkVersion)

	dojoVersion = "0.4.3"
	
	Ant.sequential {
		mkdir(dir:"${grailsHome}/downloads")

	    event("StatusUpdate", ["Downloading Dojo ${dojoVersion}"])

		get(dest:"${grailsHome}/downloads/dojo-${dojoVersion}-ajax.zip",
			src:"http://download.dojotoolkit.org/release-${dojoVersion}/dojo-${dojoVersion}-ajax.zip",
			verbose:true,
			usetimestamp:true)
		unzip(dest:"${grailsHome}/downloads",
			  src:"${grailsHome}/downloads/dojo-${dojoVersion}-ajax.zip")	
		
		mkdir(dir:"${basedir}/web-app/js/dojo")
		mkdir(dir:"${basedir}/web-app/js/dojo/src")
		
		copy(file:"${grailsHome}/downloads/dojo-${dojoVersion}-ajax/dojo.js", 
			 tofile:"${basedir}/web-app/js/dojo/dojo.js")		
		copy(file:"${grailsHome}/downloads/dojo-${dojoVersion}-ajax/iframe_history.html", 
			 tofile:"${basedir}/web-app/js/dojo/iframe_history.html")		
			
		copy(todir:"${basedir}/web-app/js/dojo/src") {
			fileset(dir:"${grailsHome}/downloads/dojo-${dojoVersion}-ajax/src", includes:"**/**")
		}		 
	}            
	event("StatusFinal", ["Dojo ${dojoVersion} installed successfully"])
}