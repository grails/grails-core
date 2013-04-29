/*
 * Copyright 2011 SpringSource.
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
package grails.util

import org.codehaus.groovy.grails.cli.support.GrailsStarter

// First check for a system property called "grails.home". If that
// exists, we use its value as the location of Grails. Otherwise, we
// use the environment variable GRAILS_HOME.
def grailsHome = System.getProperty("grails.home")
if (!grailsHome) {
    grailsHome = System.getenv("GRAILS_HOME")

    if (!grailsHome) {
        println "Either the system property \"grails.home\" or the " +
                "environment variable GRAILS_HOME must be set to the " +
                "location of your Grails installation."
        return
    }

    System.setProperty("grails.home", grailsHome)
}

// Load the build properties so that we can read the Grails version.
def props = new Properties()
props.load(getClass().getClassLoader().getResourceAsStream("grails.build.properties"))

// We need JAVA_HOME so that we can get hold of the "tools.jar" file.
def javaHome = new File(System.getenv("JAVA_HOME"))

System.setProperty("grails.version", props["grails.version"])
System.setProperty("tools.jar", new File(javaHome, "lib/tools.jar").absolutePath)
GrailsStarter.main(["--conf", "${grailsHome}/conf/groovy-starter.conf", "--main", "org.codehaus.groovy.grails.cli.GrailsScriptRunner", "run-app"] as String[])
