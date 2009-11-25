package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class FindByLikeTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Street {
    String name = 'hello'
    static constraints = {
	    name(size: 2.. 50, blank: false, unique: true)
    }
}

''')
    }


    void testFindByLikeQuery() {
        def Street = ga.getDomainClass("Street").clazz
        def street = Street.newInstance(name: 'ab').save(flush: true)
        street = Street.newInstance(name: 'aab').save(flush: true)
        street = Street.newInstance(name: 'abc').save(flush: true)
        street = Street.newInstance(name: 'abcc').save(flush: true)
        street = Street.newInstance(name: 'abcc').save(flush: true)
        street = Street.newInstance(name: 'abcd').save(flush: true)
        street = Street.newInstance(name: 'abce').save(flush: true)
        street = Street.newInstance(name: 'abcc').save(flush: true)
        street = Street.newInstance(name: 'bcc').save(flush: true)
        street = Street.newInstance(name: 'dbcc').save(flush: true)

        session.clear()


	    assert Street.findAllByNameIlike('%ab%', [max: 5]) : "should have got some results from ilike query"

        assert Street.findByNameIlike('%ab%') : "should have got a result from ilike query"
    }

}