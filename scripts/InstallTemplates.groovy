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
 * Gant script that installs artifact and scaffolding templates
 * 
 * @author Marcel Overdijk
 *
 * @since 0.4
 */

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU

Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )  

target ('default': "Installs the artifact and scaffolding templates") {
    depends(checkVersion) 

	targetDir = "${basedir}/src/templates"
	overwrite = false

	// only if template dir already exists in, ask to overwrite templates
	if (new File(targetDir).exists()) {
		Ant.input(addProperty: "${args}.template.overwrite", message: "Overwrite existing templates? [y/n]")
		if (Ant.antProject.properties."${args}.template.overwrite" == "y")
			overwrite = true
	}
	else {
		Ant.mkdir(dir: targetDir)
	}

	Ant.copy(todir: targetDir, overwrite: overwrite) {
		// copy artifact and scaffolding templates
		fileset(dir: "${grailsHome}/src/grails/templates", includes: "artifacts/*, scaffolding/*")
	}    
	Ant.mkdir(dir:"${targetDir}/war")
	Ant.copy(tofile:"${targetDir}/war/web.xml", file:"${grailsHome}/src/war/WEB-INF/web${servletVersion}.template.xml")
	
    event("StatusUpdate", [ "Templates installed successfully"])
}