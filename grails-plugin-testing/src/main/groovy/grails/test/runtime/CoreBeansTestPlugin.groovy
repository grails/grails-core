package grails.test.runtime;

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.InstanceFactoryBean
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import org.codehaus.groovy.grails.plugins.databinding.DataBindingGrailsPlugin
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAwareBeanPostProcessor
import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler
import org.codehaus.groovy.grails.validation.ConstraintsEvaluator
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import org.springframework.context.support.ConversionServiceFactoryBean
import org.springframework.context.support.StaticMessageSource

@CompileStatic
public class CoreBeansTestPlugin implements TestPlugin {
    String[] requiredFeatures = ['grailsApplication']
    String[] providedFeatures = ['coreBeans']
    int ordinal = 0

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void registerBeans(TestRuntime runtime, GrailsApplication grailsApplication) {
        defineBeans(runtime, new DataBindingGrailsPlugin().doWithSpring)

        defineBeans(runtime) {
            xmlns context:"http://www.springframework.org/schema/context"
            // adds AutowiredAnnotationBeanPostProcessor, CommonAnnotationBeanPostProcessor and others
            // see org.springframework.context.annotation.AnnotationConfigUtils.registerAnnotationConfigProcessors method
            context.'annotation-config'()

            proxyHandler(DefaultProxyHandler)
            grailsApplication(InstanceFactoryBean, grailsApplication, GrailsApplication)
            pluginManager(DefaultGrailsPluginManager, [] as Class[], grailsApplication)
            messageSource(StaticMessageSource)
            "${ConstraintsEvaluator.BEAN_NAME}"(DefaultConstraintEvaluator)
            conversionService(ConversionServiceFactoryBean)
            grailsApplicationPostProcessor(GrailsApplicationAwareBeanPostProcessor, grailsApplication)
        }
    }
    
    void defineBeans(TestRuntime runtime, Closure closure) {
        runtime.publishEvent("defineBeans", [closure: closure])
    }
    
    public void onTestEvent(TestEvent event) {
        switch(event.name) {
            case 'registerBeans':
                registerBeans(event.runtime, (GrailsApplication)event.arguments.grailsApplication)
                break
        }
    }
    
    public void close() {
    
    }
}
