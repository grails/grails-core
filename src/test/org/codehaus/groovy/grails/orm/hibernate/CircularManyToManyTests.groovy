package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 22, 2009
 */

public class CircularManyToManyTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Organization {
    static hasMany = [children: Organization, relatedOrganizations: Organization]
    static mappedBy = [children: "parent", relatedOrganizations:"relatedOrganizations"]

    Organization parent
    String name
}

''')
    }

    void testCircularManyToManyMapping() {

        def conn = session.connection()
        conn.prepareStatement("SELECT organization_id, related_organizations__id FROM organization_related_organizations").executeQuery()

        def organizationClass = ga.getDomainClass("Organization").clazz

        def apple = organizationClass.newInstance(name:"apple")


        assertNotNull "should have saved", apple.save(flush:true)


        def ms = organizationClass.newInstance(name:"microsoft")
        apple.addToRelatedOrganizations(ms)

        apple.save(flush:true)

        session.clear()

        apple = organizationClass.get(1)

        assertEquals "apple", apple.name
        assertEquals 1, apple.relatedOrganizations.size()
        assertEquals 0, apple.children.size()

        apple.addToChildren(name:"filemaker")
        apple.save(flush:true)

        session.clear()

        apple = organizationClass.get(1)


        apple = organizationClass.get(1)

        assertEquals "apple", apple.name
        assertEquals 1, apple.relatedOrganizations.size()
        assertEquals 1, apple.children.size()

        def fm = apple.children.iterator().next()

        assertEquals "filemaker", fm.name
        assertEquals apple, fm.parent

        
    }



}