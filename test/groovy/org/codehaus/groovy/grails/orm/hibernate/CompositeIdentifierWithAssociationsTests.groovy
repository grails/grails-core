package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 22, 2009
 */

public class CompositeIdentifierWithAssociationsTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class CompositeIdentifierWithAssociationsParent implements Serializable {

    static hasMany = [ children : CompositeIdentifierWithAssociationsChild, children2: CompositeIdentifierWithAssociationsChild2]
    String col1
    String col2
    CompositeIdentifierWithAssociationsAddress address

    static mapping = { id composite:['col1', 'col2'] }
    static constraints = { address nullable:true }             
}

@Entity
class CompositeIdentifierWithAssociationsChild { static belongsTo = [ parent : CompositeIdentifierWithAssociationsParent ] }

@Entity
class CompositeIdentifierWithAssociationsChild2 {  }

@Entity
class CompositeIdentifierWithAssociationsAddress {
    String postCode
    static belongsTo = [parent:CompositeIdentifierWithAssociationsParent]
}

''')
    }

    /** TODO: Fix many-to-many with composite keys
    void testCompositeWithManyToMany() {
        def parentClass = ga.getDomainClass("Parent").clazz
        def childClass = ga.getDomainClass("Child3").clazz

        def parent = parentClass.newInstance()

        parent.col1 = "one"
        parent.col2 = "two"

        def child = childClass.newInstance()

        parent.addToChildren3(child)

        assertNotNull "should have saved", parent.save(flush:true)

        session.clear()

        parent = parentClass.get(parentClass.newInstance(col1:"one", col2:"two"))

        assertEquals 1, parent.children3.size()
    }*/

    void testCompositeWithBidirectionalOneToOne() {
        def parentClass = ga.getDomainClass("CompositeIdentifierWithAssociationsParent").clazz
        def addressClass = ga.getDomainClass("CompositeIdentifierWithAssociationsAddress").clazz

        def parent = parentClass.newInstance()

        parent.col1 = "one"
        parent.col2 = "two"
        parent.address = addressClass.newInstance(postCode:"32984739")

        assertNotNull "should have saved parent", parent.save(flush:true)

        session.clear()

        parent = parentClass.get(parentClass.newInstance(col1:"one", col2:"two"))

        assertEquals "32984739", parent.address.postCode
    }

    void testCompositeIdentiferWithBidirectionalOneToMany() {

        def parentClass = ga.getDomainClass("CompositeIdentifierWithAssociationsParent").clazz
        def childClass = ga.getDomainClass("CompositeIdentifierWithAssociationsChild").clazz

        def parent = parentClass.newInstance()

        parent.col1 = "one"
        parent.col2 = "two"

        def child = childClass.newInstance()

        parent.addToChildren(child)

        assertNotNull "should have saved", parent.save(flush:true)

        session.clear()

        parent = parentClass.get(parentClass.newInstance(col1:"one", col2:"two"))

        assertEquals 1, parent.children.size()

        session.clear()

        child = childClass.get(1)

        assertEquals "one", child.parent.col1
        assertEquals "two", child.parent.col2
        
    }


    void testCompositeIdentiferWithUnidirectionalOneToMany() {

        def parentClass = ga.getDomainClass("CompositeIdentifierWithAssociationsParent").clazz
        def childClass = ga.getDomainClass("CompositeIdentifierWithAssociationsChild2").clazz

        def parent = parentClass.newInstance()

        parent.col1 = "one"
        parent.col2 = "two"

        def child = childClass.newInstance()

        parent.addToChildren2(child)

        assertNotNull "should have saved", parent.save(flush:true)

        session.clear()

        parent = parentClass.get(parentClass.newInstance(col1:"one", col2:"two"))

        assertEquals 1, parent.children2.size()


    }

}