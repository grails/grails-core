/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Dec 3, 2007
 */
package org.codehaus.groovy.grails.orm.hibernate
class UnidirectionalListOneToManyUpdateTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class Customer
{
    Long id
    Long version
    List orders
	static hasMany = [ orders : Order]
	String email
	String password
}
class  Order
{
    Long id
    Long version
     String number
     Date date = new Date()

    static mapping = {
        table "`order`"
    }
}
'''
    }



    void testAssociateOneToMany() {
        def custClass = ga.getDomainClass("Customer").clazz
        def orderClass = ga.getDomainClass("Order").clazz

        assert custClass.newInstance(email:"foo@bar.com", password:"letmein").save()
        assert orderClass.newInstance(number:"12345").save(flush:true)
        assert orderClass.newInstance(number:"12345234").save(flush:true)

        session.clear()

        def cust = custClass.get(1)
        def orders = orderClass.list()

        orders.each {
            cust.addToOrders(it)
        }


        cust.save(flush:true)

        session.clear()

        cust = custClass.get(1)

        assertEquals 2, cust.orders.size()
    }

}