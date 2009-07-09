package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class MappingInheritanceTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Parent {
    Boolean active
    static mapping = {
        active type: 'yes_no'
    }
}


class Child1 extends Parent {
    String someField
}

class Child2 extends Parent {
    boolean anotherBoolean

    static mapping = {
        anotherBoolean type:"yes_no"
    }
}
''')
    }

    void testMappingInheritance() {
        def Child1 = ga.getDomainClass("Child1").clazz
        def Child2 = ga.getDomainClass("Child2").clazz

        def c1 = Child1.newInstance(active:true, someField:"foo" ).save(flush:true)

        assertNotNull "should have saved Child1", c1

        def c2 = Child2.newInstance(active:false, anotherBoolean:true).save(flush:true)

        assertNotNull "should have saved Child1", c2

        def conn = session.connection()

        def rs = conn.prepareStatement("SELECT active, another_boolean FROM PARENT").executeQuery()

        rs.next()

        assertEquals "Y", rs.getString("active")

        rs.next()

        assertEquals "N", rs.getString("active")
        assertEquals "Y", rs.getString("another_boolean")



    }

}