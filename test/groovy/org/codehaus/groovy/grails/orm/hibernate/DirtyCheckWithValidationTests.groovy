package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.FlushMode

/**
 * @author Graeme Rocher
 * @since 1.1
 * 
 * Created: Sep 19, 2008
 */
class DirtyCheckWithValidationTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
class DirtyCheckWithValidation {
    Long id
    Long version
    String name

    static constraints = {
        name blank:false
    }
}
''')
    }


    void testDataBindingAndValidationWithDirtyChecking() {
        session.setFlushMode FlushMode.AUTO

        def testClass = ga.getDomainClass("DirtyCheckWithValidation").clazz

        testClass.newInstance(name:"valid").save(flush:true)

        def test = testClass.get(1)
        test.properties = [name:'']
        assert !test.validate()
        session.flush()
        session.clear()
        test = testClass.get(1)

        assertEquals 'valid', test.name
    }

    void testRetrySaveWithDataBinding() {
        session.setFlushMode FlushMode.AUTO

        def testClass = ga.getDomainClass("DirtyCheckWithValidation").clazz

        testClass.newInstance(name:"valid").save(flush:true)

        def test = testClass.get(1)
        test.properties = [name:'']
        assert !test.validate()
        session.flush()
        session.clear()
        test = testClass.get(1)

        assertEquals 'valid', test.name

        test.name = ''
        assert !test.save(flush:true)

        test.name = 'thisisgood'
        assert test.save(flush:true)


        session.clear()


        test = testClass.get(1)

        assertEquals 'thisisgood', test.name
    }
}