package grails.persistence

import grails.core.DefaultGrailsApplication
import grails.core.GrailsDomainClass
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.springframework.context.ApplicationContext
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class CircularBidirectionalMapBySpec extends Specification{

    void "Test mapping for circular bidirectional association"() {

        given:"A Grails application"
            def application = new DefaultGrailsApplication([Person] as Class[], getClass().classLoader)
            application.initialise()
            application.setApplicationContext(Stub(ApplicationContext) {
                getBean('grailsDomainClassMappingContext', MappingContext) >> {
                    def context = new KeyValueMappingContext("circular")
                    context.addPersistentEntities(Person, Person2)
                    context
                }
            })

        when:"The domain instance is obtained"
            GrailsDomainClass domainClass = application.getDomainClass(Person.name)

        then:"The property mappings are correct"
            !domainClass.getPropertyByName('father').bidirectional
            !domainClass.getPropertyByName('friends').bidirectional
    }

    void "Test mapping for circular bidirectional association with 'none' as name"() {

        given:"A Grails application"
        def application = new DefaultGrailsApplication([Person2] as Class[], getClass().classLoader)
        application.initialise()
        application.setApplicationContext(Stub(ApplicationContext) {
            getBean('grailsDomainClassMappingContext', MappingContext) >> {
                def context = new KeyValueMappingContext("circular")
                context.addPersistentEntities(Person, Person2)
                context
            }
        })

        when:"The domain instance is obtained"
        GrailsDomainClass domainClass = application.getDomainClass(Person2.name)

        then:"The property mappings are correct"
        !domainClass.getPropertyByName('father').bidirectional
        !domainClass.getPropertyByName('friends').bidirectional
    }
}

@Entity
class Person {

    String name
    Person father

    static hasMany = [
        friends: Person
    ]

    static mappedBy = [
        father: null, friends:null
    ]

    static constraints = {
        father (nullable: true)
    }

}


@Entity
class Person2 {

    String name
    Person father

    static hasMany = [
        friends: Person
    ]

    static mappedBy = [
        father: 'none', friends:'none'
    ]

    static constraints = {
        father (nullable: true)
    }

}
