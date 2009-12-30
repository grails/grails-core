/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Nov 28, 2007
 */
package org.codehaus.groovy.grails.orm.hibernate

class ExecuteUpdateTests extends AbstractGrailsHibernateTests {

    private static final List<String> names = ['Fred', 'Bob', 'Ginger']

    private ids = []
    private custClass

    protected void onSetUp() {
        gcl.parseClass '''
class Customer {
    Long id
    Long version
    String name
}
'''


    }

    def init() {
        custClass = ga.getDomainClass("Customer").clazz

        for (name in names) {
            def customer = custClass.newInstance(name: name).save()
            assertNotNull customer
            ids << customer.id
        }
    }

    void testExecuteUpdate() {
        init()

        assertEquals 3, custClass.count()

        custClass.executeUpdate("delete from Customer")

        assertEquals 0, custClass.count()
    }

    void testExecuteUpdatePositionalParams() {
        init()

        assertEquals 3, custClass.count()

        custClass.executeUpdate("delete from Customer c where c.name=?", ['Fred'])

        assertEquals 2, custClass.count()
    }

    void testExecuteUpdateOrdinalParams() {
        init()

        assertEquals 3, custClass.count()

        custClass.executeUpdate("delete from Customer c where c.name=:name", [name:'Fred'])

        assertEquals 2, custClass.count()
    }

    void testExecuteUpdateListParams() {
        init()

        assertEquals 3, custClass.count()

        custClass.executeUpdate("delete from Customer c where c.id in (:ids)", [ids: ids[0..1]])

        assertEquals 1, custClass.count()
    }

    void testExecuteUpdateArrayParams() {
        init()

        assertEquals 3, custClass.count()

        Object[] deleteIds = [ids[0], ids[1]]
        custClass.executeUpdate("delete from Customer c where c.id in (:ids)", [ids: deleteIds])
        assertEquals 1, custClass.count()
    }
}
