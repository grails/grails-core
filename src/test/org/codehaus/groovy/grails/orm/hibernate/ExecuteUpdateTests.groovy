/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 28, 2007
 */
package org.codehaus.groovy.grails.orm.hibernate
class ExecuteUpdateTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class Customer {
    Long id
    Long version
    String name
}
'''
    }

    void testExecuteUpdate() {
        def custClass = ga.getDomainClass("Customer").clazz

        assert custClass.newInstance(name:"Fred").save()
        assert custClass.newInstance(name:"Bob").save()
        assert custClass.newInstance(name:"Ginger").save()

        assertEquals 3, custClass.count()

        custClass.executeUpdate("delete from Customer")

        assertEquals 0, custClass.count()
    }

    void testExecuteUpdatePositionalParams() {
      def custClass = ga.getDomainClass("Customer").clazz

        assert custClass.newInstance(name:"Fred").save()
        assert custClass.newInstance(name:"Bob").save()
        assert custClass.newInstance(name:"Ginger").save()

        assertEquals 3, custClass.count()

        custClass.executeUpdate("delete from Customer c where c.name=?", ['Fred'])

        assertEquals 2, custClass.count()
    }

    void testExecuteUpdateOrdinalParams() {
      def custClass = ga.getDomainClass("Customer").clazz

        assert custClass.newInstance(name:"Fred").save()
        assert custClass.newInstance(name:"Bob").save()
        assert custClass.newInstance(name:"Ginger").save()

        assertEquals 3, custClass.count()

        custClass.executeUpdate("delete from Customer c where c.name=:name", [name:'Fred'])

        assertEquals 2, custClass.count()
    }


}