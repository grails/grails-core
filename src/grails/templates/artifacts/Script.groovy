import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU

Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )  

task ('default': "The description of the script goes here!") {
	doStuff()
}

task(doStuff: "The implementation task") {
	
}