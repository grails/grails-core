package org.codehaus.groovy.grails.orm.hibernate

class ReadOnlyTransactionTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class Gorpledanger {
    Long id
    Long version
    String name
}

class GorpledangerService {
    static transactional = false

    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    void doNotModify(thing) {
        thing.name += 'wahoo'
    }
}
'''
    }

    void testReadOnlyAnnotation() {
        def clazz = ga.getDomainClass('Gorpledanger').clazz
        def instance = clazz.newInstance()
        instance.name = 'foo'
        assertNotNull instance.save(flush: true)
        session.clear()

        instance = clazz.get(instance.id)
        def service = appCtx.gorpledangerService
        service.doNotModify instance
        session.clear()

        instance = clazz.get(instance.id)
        assertEquals 'foo', instance.name
    }
}
