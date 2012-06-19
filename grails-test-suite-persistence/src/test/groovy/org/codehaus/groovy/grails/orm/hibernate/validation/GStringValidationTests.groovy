package org.codehaus.groovy.grails.orm.hibernate.validation

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests

/**
 * GRAILS-2401
 *
 * @author Burt Beckwith
 */
class GStringValidationTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class WhizBang {
    Long id
    Long version
    String name
    int age

    static constraints = {
        age validator: { val, obj ->
            if (val > 500) {
                return "foo"
            }
        }

        name validator: { val, obj ->
            if (val.size() > 5) {
                return "foo${5}"
            }
        }
    }
}
'''
    }

    void testValidateString() {
        def WhizBang = ga.getDomainClass('WhizBang').clazz

        def instance = WhizBang.newInstance()
        instance.name = 'x'
        instance.age = 1000
        instance.validate()
        assertEquals 1, instance.errors.errorCount
        assertEquals 'foo', instance.errors.getFieldError('age').code
    }

    void testValidateGString() {
        def WhizBang = ga.getDomainClass('WhizBang').clazz

        def instance = WhizBang.newInstance()
        instance.name = 'xxxxxx'
        instance.age = 100
        instance.validate()
        assertEquals 1, instance.errors.errorCount
        assertEquals 'foo5', instance.errors.getFieldError('name').code
    }
}