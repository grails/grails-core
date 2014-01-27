package grails.test.runtime

import java.util.Map;

import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.pages.GroovyPageBinding
import org.codehaus.groovy.grails.web.pages.GroovyPageRequestBinding
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest

@CompileStatic
class WebFlowTestPlugin implements TestPlugin {
    String[] requiredFeatures = ['controller']
    String[] providedFeatures = ['webFlow']
    int ordinal = 0

    protected void clearWebFlowTestState(TestRuntime runtime) {
        runtime.removeValue("webFlowTestState")
    }
    
    public void onTestEvent(TestEvent event) {
        switch(event.name) {
            case 'afterClass':
                clearWebFlowTestState(event.runtime)
                break
        }
    }
    
    public void close(TestRuntime runtime) {
        
    }
}