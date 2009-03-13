package grails.util

import org.codehaus.groovy.grails.cli.support.GrailsStarter;

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
props.load(getClass().getClassLoader().getResourceAsStream("build.properties"))

// We need JAVA_HOME so that we can get hold of the "tools.jar" file.
def javaHome = new File(System.getenv("JAVA_HOME"))

System.setProperty("grails.version", props["grails.version"])
System.setProperty("tools.jar", new File(javaHome, "lib/tools.jar").absolutePath)
GrailsStarter.main(["--conf", "${grailsHome}/conf/groovy-starter.conf", "--main", "org.codehaus.groovy.grails.cli.GrailsScriptRunner", "run-app"] as String[])
