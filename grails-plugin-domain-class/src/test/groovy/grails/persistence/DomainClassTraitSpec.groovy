package grails.persistence

import grails.core.DefaultGrailsApplication
import grails.util.Holders
import org.grails.datastore.gorm.validation.constraints.registry.DefaultValidatorRegistry
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.springframework.context.ApplicationContext
import spock.lang.Issue
import spock.lang.Specification

/**
 * @author James Kleeh
 */
class DomainClassTraitSpec extends Specification {

    @Issue("GRAILS-9245")
    void "test getConstrainedProperties"() {
        given:
        def application = new DefaultGrailsApplication([Person] as Class[], getClass().classLoader)
        application.initialise()

        def context = new KeyValueMappingContext("domainclasstraitspec")
        context.addPersistentEntity(Person)
        context.setValidatorRegistry(new DefaultValidatorRegistry(context, new ConnectionSourceSettings()))
        application.setApplicationContext(Stub(ApplicationContext) {
            getBean('grailsDomainClassMappingContext', MappingContext) >> {
                context
            }
        })
        application.setMappingContext(context)
        Holders.grailsApplication = application

        expect:
        !Person.constrainedProperties.name.blank
        new Person().constrainedProperties.name.inList == ['Joe']
    }

    @Entity
    class Person {
        String name
        
        static constraints = {
            name blank: false, inList: ['Joe']
        }
    }

}
