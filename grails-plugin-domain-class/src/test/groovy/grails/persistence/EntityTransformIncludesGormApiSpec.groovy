package grails.persistence

import spock.lang.Specification


/**
 * Created by graemerocher on 20/06/2014.
 */
class EntityTransformIncludesGormApiSpec extends Specification{

    void "Test that with the presence of grails-datastore-gorm that the GORM API is added to compiled entities annotated with @Entity"() {


        when:"A entity annotated with @Entity is compiled"
            def cls = new GroovyClassLoader().parseClass('''
import grails.persistence.*

@Entity
class Book { String title }
''')

        then:"The class has the GORM APIs added to it"
            cls.getMethod('getErrors', null) != null
    }
}
