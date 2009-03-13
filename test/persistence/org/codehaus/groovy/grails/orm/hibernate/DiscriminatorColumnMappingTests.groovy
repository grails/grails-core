package org.codehaus.groovy.grails.orm.hibernate

import java.sql.ResultSet

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class DiscriminatorColumnMappingTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Root {

    static mapping = {
        discriminator value:"1", column:[name:"tree",sqlType:"int"]
    }
}

@Entity
class Child1 extends Root{

    static mapping = {
       discriminator "2"
    }
}

@Entity
class Child2 extends Root {

    static mapping = {
       discriminator "3"
    }
}

''')
    }


    void testDiscriminatorMapping() {
        def Root = ga.getDomainClass("Root").clazz
        def Child1 = ga.getDomainClass("Child1").clazz
        def Child2 = ga.getDomainClass("Child2").clazz

        assertNotNull "should have saved root", Root.newInstance().save(flush:true)

        def conn = session.connection()

        ResultSet rs = conn.prepareStatement("select tree from root").executeQuery()
        rs.next()
        assertEquals 1, rs.getInt("tree")

        rs.close()

        assertNotNull "should have saved child1", Child1.newInstance().save(flush:true)

        rs = conn.prepareStatement("select tree from root").executeQuery()

        rs.next()
        assertEquals 1, rs.getInt("tree")

        rs.next()
        assertEquals 2, rs.getInt("tree")

    }

}