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
 * Gant script that creates a ZIP file creating just the artifacts from a project, for attaching to a JIRA issue
 *
 *
 * @author Marc Palmer
 *
 * @since 0.4
 */

Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )

artifactNames = [
    'conf',
    'controllers',
    'domain',
    'jobs',
    'i18n',
    'services',
    'taglib',
    'views' 
]

target ('default': "Creates a ZIP containing source artifacts for reporting bugs") {
    depends( checkVersion )
    
	def fileName = new File(basedir).name
	def date = new java.text.SimpleDateFormat("ddMMyyyy").format(new Date())
	def zipName = "${basedir}/${fileName}-bug-report-${date}.zip"

	Ant.zip(destfile:zipName) {
	    fileset(dir: "${basedir}", includes: "grails-app/**/*")
	    fileset(dir: "${basedir}", includes: "test/**/*")
	    fileset(dir: "${basedir}", includes: "scripts/**/*")
	    fileset(dir: "${basedir}", includes: "spring/**/*")
	    fileset(dir: "${basedir}", includes: "src/**/*")
	}

    event("StatusFinal", ["Created bug-report ZIP at ${zipName}"])
}

