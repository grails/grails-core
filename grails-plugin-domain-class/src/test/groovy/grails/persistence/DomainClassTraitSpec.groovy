package grails.persistence

import grails.core.DefaultGrailsApplication
import grails.gorm.validation.PersistentEntityValidator
import grails.persistence.Entity
import grails.util.Holders
import grails.validation.ConstrainedProperty
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.springframework.context.ApplicationContext
import spock.lang.Issue
import spock.lang.Specification

import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * @author James Kleeh
 */
class DomainClassTraitSpec extends Specification {

    @Issue("GRAILS-9245")
    void "test getConstrainedProperties"() {
        given:
        def application = new DefaultGrailsApplication([Person] as Class[], getClass().classLoader)
        application.initialise()
        def field = PersistentEntityValidator.getDeclaredField('constrainedProperties')
        field.setAccessible(true)
        Field modifiersField = Field.getDeclaredField("modifiers")
        modifiersField.setAccessible(true)
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL)
        def validator = Mock(PersistentEntityValidator)

        field.set(validator, (Map)[name: [blank: false, inList: ['Joe']]])

        application.setApplicationContext(Stub(ApplicationContext) {
            getBean('grailsDomainClassMappingContext', MappingContext) >> {
                def context = new KeyValueMappingContext("domainclasstraitspec")
                context.addPersistentEntities(Person)
                context.addEntityValidator(context.getPersistentEntity(Person.class.name), validator)
                context
            }
        })
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

    class MockValidator extends PersistentEntityValidator {
        MockValidator() {
            def cp = Stub(ConstrainedProperty)
            cp.blank = false
            cp.inList = ['Joe']
            this.constrainedProperties = [name: cp]
        }
    }
}
