package grails.gorm.transactional

import spock.lang.Specification

/**
 * Created by graemerocher on 24/02/2017.
 */
class TransactionalIntegrationTestSpec extends Specification {

    void "test that @Integration tests are correctly transformed"() {
        when:
        Class testClass = new GroovyClassLoader().parseClass('''
import grails.test.mixin.integration.Integration
import grails.transaction.*

import spock.lang.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@Integration
@Rollback
class BookIntegrationSpec extends Specification {
    void "test something"() {
        expect:
        1 == 1
    }
}
''')
        then:"The test has transaction management wired in"
        testClass != null
        testClass.getMethod("getTransactionManager") != null
    }
}
