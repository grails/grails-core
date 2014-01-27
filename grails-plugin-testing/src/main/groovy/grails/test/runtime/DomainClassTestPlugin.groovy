package grails.test.runtime

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.domain.GrailsDomainClassCleaner
import org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.codehaus.groovy.grails.validation.ConstraintsEvaluator
import org.codehaus.groovy.grails.validation.ConstraintsEvaluatorFactoryBean
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.gorm.validation.constraints.UniqueConstraintFactory
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.AbstractMappingContext
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.web.context.ConfigurableWebApplicationContext

@CompileStatic
class DomainClassTestPlugin implements TestPlugin {
    String[] requiredFeatures = ['grailsApplication', 'coreBeans']
    String[] providedFeatures = ['domainClass']
    int ordinal = 0

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void registerBeans(TestRuntime runtime, GrailsApplication grailsApplication) {
        defineBeans(runtime) {
            grailsDatastore(SimpleMapDatastore, grailsApplication.parentContext)
            transactionManager(DatastoreTransactionManager) {
                datastore = ref("grailsDatastore")
            }
            "${ConstraintsEvaluator.BEAN_NAME}"(ConstraintsEvaluatorFactoryBean) {
                defaultConstraints = DomainClassGrailsPlugin.getDefaultConstraints(grailsApplication.config)
            }
            grailsDomainClassCleaner(GrailsDomainClassCleaner, grailsApplication)
        }
    }
    
    protected void applicationInitialized(TestRuntime runtime, GrailsApplication grailsApplication) {
        initializeDatastoreImplementation(grailsApplication)
    }
    
    protected void initializeDatastoreImplementation(GrailsApplication grailsApplication) {
        // TODO: parentContext or mainContext?
        ConfigurableWebApplicationContext applicationContext = (ConfigurableWebApplicationContext)grailsApplication.parentContext
        SimpleMapDatastore simpleDatastore = applicationContext.getBean(SimpleMapDatastore)
        ((AbstractMappingContext)simpleDatastore.mappingContext).setCanInitializeEntities(false)
        applicationContext.addApplicationListener applicationContext.getBean(GrailsDomainClassCleaner)
        applicationContext.addApplicationListener new DomainEventListener(simpleDatastore)
        applicationContext.addApplicationListener new AutoTimestampEventListener(simpleDatastore)
        ConstrainedProperty.registerNewConstraint("unique", new UniqueConstraintFactory(simpleDatastore))
    }

    protected void cleanupDatastore() {
        ClassPropertyFetcher.clearCache()
        ConstrainedProperty.removeConstraint("unique")
    }

    protected void connectDatastore(TestRuntime runtime, GrailsApplication grailsApplication) {
        ConfigurableWebApplicationContext applicationContext = (ConfigurableWebApplicationContext)grailsApplication.parentContext
        SimpleMapDatastore simpleDatastore = applicationContext.getBean(SimpleMapDatastore)
        Session currentSession = DatastoreUtils.bindSession(simpleDatastore.connect())
        runtime.putValue("domainClassCurrentSession", currentSession)
    }

    protected void shutdownDatastoreImplementation(TestRuntime runtime, GrailsApplication grailsApplication) {
        Session currentSession = (Session)runtime.removeValue("domainClassCurrentSession")
        if (currentSession != null) {
            currentSession.disconnect()
            DatastoreUtils.unbindSession(currentSession)
        }
        ConfigurableWebApplicationContext applicationContext = (ConfigurableWebApplicationContext)grailsApplication.parentContext
        SimpleMapDatastore simpleDatastore = applicationContext.getBean(SimpleMapDatastore)
        simpleDatastore.clearData()
    }

    protected void before(TestRuntime runtime, GrailsApplication grailsApplication) {
        connectDatastore(runtime, grailsApplication)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void after(TestRuntime runtime, GrailsApplication grailsApplication) {
        shutdownDatastoreImplementation(runtime, grailsApplication)
    }
    
    void defineBeans(TestRuntime runtime, Closure closure) {
        runtime.publishEvent("defineBeans", [closure: closure])
    }
    
    GrailsApplication getGrailsApplication(TestEvent event) {
        (GrailsApplication)event.runtime.getValue("grailsApplication")
    }

    public void onTestEvent(TestEvent event) {
        switch(event.name) {
            case 'before':
                before(event.runtime, getGrailsApplication(event))
                break
            case 'after':
                after(event.runtime, getGrailsApplication(event))
                break
            case 'registerBeans':
                registerBeans(event.runtime, (GrailsApplication)event.arguments.grailsApplication)
                break
            case 'applicationInitialized':
                applicationInitialized(event.runtime, (GrailsApplication)event.arguments.grailsApplication)
                break
            case 'beforeClass':
                ClassPropertyFetcher.clearCache()
                break
            case 'afterClass':
                cleanupDatastore()
                break
        }
    }
    
    public void close(TestRuntime runtime) {
        
    }
}
