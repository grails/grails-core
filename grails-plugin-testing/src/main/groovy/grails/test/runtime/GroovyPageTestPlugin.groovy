package grails.test.runtime

import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.pages.GroovyPageBinding
import org.codehaus.groovy.grails.web.pages.GroovyPageRequestBinding
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest

@CompileStatic
class GroovyPageTestPlugin implements TestPlugin {
    String[] requiredFeatures = ['controller']
    String[] providedFeatures = ['groovyPage']
    int ordinal = 0

    protected void bindPageScope(TestRuntime runtime) {
        GrailsWebRequest webRequest = (GrailsWebRequest) runtime.getValue("webRequest")
        GroovyPageBinding pageScope = new GroovyPageBinding(new GroovyPageRequestBinding(webRequest))
        webRequest.request.setAttribute(GrailsApplicationAttributes.PAGE_SCOPE, pageScope)
        runtime.putValue("pageScope", pageScope)
    }

    protected void clearPageScope(TestRuntime runtime) {
        runtime.removeValue("pageScope")
    }
    
    public void onTestEvent(TestEvent event) {
        switch(event.name) {
            case 'before':
                bindPageScope(event.runtime)
                break
            case 'after':
                clearPageScope(event.runtime)
                break
        }
    }
    
    public void close(TestRuntime runtime) {
        
    }
}