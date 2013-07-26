/*
 * Copyright 2013 SpringSource
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
package org.codehaus.groovy.grails.project.creation

import grails.build.logging.GrailsConsole
import grails.util.BuildSettings
import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.cli.api.BaseSettingsApi
import org.codehaus.groovy.grails.cli.logging.GrailsConsoleAntBuilder
import org.codehaus.groovy.grails.cli.support.GrailsBuildEventListener

/**
 * Responsible for cleaning a Grails project
 *
 * @author Graeme Rocher
 * @since 2.3
 */

class GrailsProjectCleaner extends BaseSettingsApi {
    private static final GrailsConsole CONSOLE  = GrailsConsole.getInstance()

    private AntBuilder ant
    GrailsProjectCleaner(BuildSettings settings, GrailsBuildEventListener buildEventListener) {
        super(settings, buildEventListener, false)
    }

    @CompileStatic
    AntBuilder getAnt() {
        if (ant == null) {
            ant = new GrailsConsoleAntBuilder()
        }
        ant
    }

    /**
     *  Cleans a Grails project
     **/
    void cleanAll(boolean triggerEvents = true) {
        if (triggerEvents) {
            buildEventListener.triggerEvent("CleanAllStart")
        }
        clean()
        cleanTestReports()
        ant.delete(dir:new File(buildSettings.projectWorkDir, "scriptCache"), failonerror:false)
        ant.delete(dir:buildSettings.pluginBuildClassesDir, failonerror:false)

        CONSOLE.updateStatus "Application cleaned."
        if (triggerEvents) {
            buildEventListener.triggerEvent("CleanAllEnd")
        }
    }

    /**
     * Implementation of clean
     **/
    void clean(boolean triggerEvents = true) {
        if (triggerEvents) {
            buildEventListener.triggerEvent("CleanStart")
        }

        cleanCompiledSources()
        cleanWarFile()

        if (triggerEvents) {
            buildEventListener.triggerEvent("CleanEnd")
        }

    }

    void cleanWork() {
        ant.delete(dir: buildSettings.projectWorkDir, failonerror: false)
    }

    /**
     * Cleans compiled Java and Groovy sources
     **/
    void cleanCompiledSources(boolean triggerEvents = true)  {
        if (triggerEvents) {
            buildEventListener.triggerEvent("CleanCompiledSourcesStart")
        }

        def webInf = "${buildSettings.baseDir}/web-app/WEB-INF"
        ant.delete(dir:"${webInf}/classes")
        ant.delete(dir: new File(buildSettings.grailsWorkDir, ".slcache"), failonerror: false)
        ant.delete(file:buildSettings.webXmlLocation.absolutePath, failonerror:false)
        ant.delete(dir:"${buildSettings.projectWorkDir}/gspcompile", failonerror:false)
        ant.delete(dir:"${webInf}/lib")
        ant.delete(dir:"${buildSettings.baseDir}/web-app/plugins")
        ant.delete(dir:buildSettings.classesDir.absolutePath)
        ant.delete(dir:buildSettings.pluginClassesDir, failonerror:false)
        ant.delete(dir:buildSettings.pluginProvidedClassesDir, failonerror:false)

        ant.delete(dir:buildSettings.resourcesDir)
        ant.delete(dir:buildSettings.testClassesDir)
        ant.delete(failonerror:false, includeemptydirs: true) {
            fileset(dir:buildSettings.projectWorkDir) {
                include name:"*.resolve"
            }
        }

        if (triggerEvents) {
            buildEventListener.triggerEvent("CleanCompiledSourcesEnd")
        }
    }

    /**
     *  Cleans the test reports
     **/
    void cleanTestReports(boolean triggerEvents = true) {
        if (triggerEvents) {
            buildEventListener.triggerEvent("CleanTestReportsStart")
        }
        // Delete all reports *except* TEST-TestSuites.xml which we need
        // for the "--rerun" option to work.
        ant.delete(failonerror:false, includeemptydirs: true) {
            fileset(dir:buildSettings.testReportsDir.path) {
                include(name: "**/*")
                exclude(name: "TESTS-TestSuites.xml")
            }
        }
        if (triggerEvents) {
            buildEventListener.triggerEvent("CleanTestReportsEnd")
        }
    }

    /**
     * Cleans the deployable .war file
     */
    void cleanWarFile(boolean triggerEvents = true)  {
        if (triggerEvents) {
            buildEventListener.triggerEvent("CleanWarFileStart")
        }

        ant.delete(file:buildSettings.projectWarFile, failonerror:false)

        if (triggerEvents) {
            buildEventListener.triggerEvent("CleanWarFileEnd")
        }

    }
}


