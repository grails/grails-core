package grails.util;

import org.codehaus.groovy.tools.*

                                  
def ant = new AntBuilder()
ant.property(environment:"env")       
grailsHome = ant.antProject.properties."env.GRAILS_HOME"   

if(!grailsHome) {
	println "Environment variable GRAILS_HOME must be set to the location of your Grails install"
	return
}
System.setProperty("groovy.starter.conf", "${grailsHome}/conf/groovy-starter.conf")
System.setProperty("grails.home", grailsHome)
GroovyStarter.rootLoader(["--main", "org.codehaus.groovy.grails.cli.GrailsScriptRunner", "run-app"] as String[])	