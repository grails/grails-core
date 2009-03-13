package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class TreeListAssociationTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Customer{

   Customer parent
   List children
   String description

   static belongsTo = [parent:Customer]
   static hasMany = [children:Customer]

   static constraints = {
       parent(nullable: true)
       children(nullable: true)
   }
}
''')
    }

    void testTreeListAssociation() {
        def Customer = ga.getDomainClass("Customer").clazz

        def root = Customer.newInstance(description:"root")

        assertNotNull "should have saved root",root.save(flush:true)

        root.addToChildren(description:"child1")
            .addToChildren(description:"child2")
            .save(flush:true)

        session.clear()

        root = Customer.get(1)

        assertEquals "child1",root.children[0].description
        assertEquals "child2",root.children[1].description

        def one = root.children[0]
        def two = root.children[1]

        root.children[0] = two
        root.children[1] = one
        root.save(flush:true)

        session.clear()

        root = Customer.get(1)

        assertEquals "child2",root.children[0].description
        assertEquals "child1",root.children[1].description

    }


}