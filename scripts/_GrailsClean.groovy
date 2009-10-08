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
 * Gant script that cleans a Grails project
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

// No point doing this stuff more than once.
if (getBinding().variables.containsKey("_grails_clean_called")) return
_grails_clean_called = true

includeTargets << grailsScript("_GrailsEvents")

target ( cleanAll: "Cleans a Grails project" ) {
	clean()
	cleanTestReports()
}

target ( clean: "Implementation of clean" ) {
    depends(cleanCompiledSources, cleanWarFile)
}

target ( cleanCompiledSources: "Cleans compiled Java and Groovy sources" ) {
    def webInf = "${basedir}/web-app/WEB-INF"
    ant.delete(dir:"${webInf}/classes")
    ant.delete(file:webXmlFile.absolutePath, failonerror:false)
    ant.delete(file:"${projectWorkDir}/gspcompile", failonerror:false)
    ant.delete(dir:"${webInf}/lib")
    ant.delete(dir:"${basedir}/web-app/plugins")
    ant.delete(dir:classesDirPath)
    ant.delete(dir:resourcesDirPath)
    ant.delete(dir:testDirPath)
}

target ( cleanTestReports: "Cleans the test reports" ) {
    // Delete all reports *except* TEST-TestSuites.xml which we need
    // for the "--rerun" option to work.
    ant.delete(failonerror:false, includeemptydirs: true) {
        fileset(dir:grailsSettings.testReportsDir.path) {
            include(name: "**/*")
            exclude(name: "TESTS-TestSuites.xml")
        }
    }
}

target ( cleanWarFile: "Cleans the deployable .war file" ) {
    if (buildConfig.grails.war.destFile) {
        warName = buildConfig.grails.war.destFile
    }
    else {
        def fileName = grailsAppName
        def version = metadata.'app.version'
        if (version) {
            fileName += "-$version"
        }
        warName = "${basedir}/${fileName}.war"
    }

    ant.delete(file:warName, failonerror:false)
}
