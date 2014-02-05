package sharedruntimetest

import org.codehaus.groovy.grails.commons.GrailsApplication;

class SharedRuntimeCheck {
    static GrailsApplication previousGrailsApplication
    static int configurerCounter = 0
    
    static void checkGrailsApplicationHasNotChanged(GrailsApplication grailsApplication) {
        assert configurerCounter == 0
        if(previousGrailsApplication != null) {
             assert previousGrailsApplication == grailsApplication
        } else {
            previousGrailsApplication = grailsApplication
        }
    }
}
