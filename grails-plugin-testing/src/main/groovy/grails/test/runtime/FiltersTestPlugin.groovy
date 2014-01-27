package grails.test.runtime;

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.web.filters.CompositeInterceptor

@CompileStatic
public class FiltersTestPlugin implements TestPlugin {
    String[] requiredFeatures = ['controller']
    String[] providedFeatures = ['filters']
    int ordinal = 0

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void registerBeans(TestRuntime runtime, GrailsApplication grailsApplicationParam) {
        defineBeans(runtime) {
            filterInterceptor(CompositeInterceptor)
        }
    }
    
    protected void clearFilters(TestRuntime runtime) {
        getCompositeInterceptor(runtime).handlers?.clear()
    }
    
    CompositeInterceptor getCompositeInterceptor(TestRuntime runtime) {
        return getGrailsApplication(runtime).mainContext.getBean("filterInterceptor", CompositeInterceptor)
    }

    void defineBeans(TestRuntime runtime, Closure closure) {
        runtime.publishEvent("defineBeans", [closure: closure])
    }
    
    GrailsApplication getGrailsApplication(TestRuntime runtime) {
        (GrailsApplication)runtime.getValue("grailsApplication")
    }
    
    public void onTestEvent(TestEvent event) {
        switch(event.name) {
            case 'after':
                clearFilters(event.runtime)
                break
            case 'registerBeans':
                registerBeans(event.runtime, (GrailsApplication)event.arguments.grailsApplication)
                break
        }
    }
    
    public void close(TestRuntime runtime) {
    
    }
}
