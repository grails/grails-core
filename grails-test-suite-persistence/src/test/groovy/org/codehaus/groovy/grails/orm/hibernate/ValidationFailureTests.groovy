package org.codehaus.groovy.grails.orm.hibernate

class ValidationFailureTests extends AbstractGrailsHibernateTests {

    void onSetUp() {
        gcl.parseClass """
class ValidationFailureBook {
  Long id
  Long version
  String title
}

class ValidationFailureAuthor {
    Long id
    Long version
    String name
    Set books
    static hasMany = [books: ValidationFailureBook]
    static constraints = {
        name(size:8..16)
    }
}
class ValidationOrder {
    Long id
    Long version
    String five
    String four
    String one
    String six
    String three
    String two

    static constraints = {
        one blank: false
        two blank: false
        three blank: false
        four blank: false
        five blank: false
        six blank: false
    }
}
"""
    }

    void testValidationFailure() {
        def authorClass = ga.getDomainClass("ValidationFailureAuthor")
        def bookClass = ga.getDomainClass("ValidationFailureBook")

        def a = authorClass.newInstance()
        a.name = "123456789"

        def b1 = bookClass.newInstance()
        b1.title = "foo"
        a.addToBooks(b1)
        def b2 = bookClass.newInstance()
        b2.title = "bar"
        a.addToBooks(b2)

        a.save(true)

        assertTrue session.contains(a)
        session.flush()

        session.evict(a)
        session.evict(b1)
        session.evict(b2)
        a = null
        b1 = null
        b2 = null

        a = authorClass.clazz.get(1)

        // now invalidate a
        a.name = "bad"
        a.save()

        session.flush()
        session.clear()

        a = authorClass.clazz.get(1)
        assertEquals "123456789", a.name
    }

    void testOrderOfErrors() {
        def orderClass = ga.getDomainClass('ValidationOrder')
        def order = orderClass.newInstance()
        assertFalse order.validate()
        def errors = order.errors
        assertNotNull errors
        assertEquals 6, errors.errorCount
        assertEquals 'one', errors.allErrors[0].field
        assertEquals 'two', errors.allErrors[1].field
        assertEquals 'three', errors.allErrors[2].field
        assertEquals 'four', errors.allErrors[3].field
        assertEquals 'five', errors.allErrors[4].field
        assertEquals 'six', errors.allErrors[5].field
    }
}
