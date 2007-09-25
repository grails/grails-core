import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU

grailsHome = Ant.project.properties."environment.GRAILS_HOME"

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )  

task ('default': "The description of the script goes here!") {
    doStuff()
}

task(doStuff: "The implementation task") {

}