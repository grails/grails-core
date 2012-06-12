package grails.orm

import org.codehaus.groovy.grails.orm.hibernate.GrailsHibernateTemplate
import org.hibernate.impl.CriteriaImpl

/**
 * @author Burt Beckwith
 */
class PagedResultListTests extends GroovyTestCase {

    void testSerialize() {

        def list = new TestPagedResultList()

        new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(list)

        assertTrue list.totalCountCalled
    }
}

class TestCriteria extends CriteriaImpl {
    TestCriteria() { super(null, null) }
    @Override
    List list() { /* do nothing */ }
}

class TestPagedResultList extends PagedResultList {

    private boolean totalCountCalled = false

    TestPagedResultList() {
        super(new GrailsHibernateTemplate(), new TestCriteria())
    }

    @Override
    int getTotalCount() {
        totalCountCalled = true
        42
    }
}
