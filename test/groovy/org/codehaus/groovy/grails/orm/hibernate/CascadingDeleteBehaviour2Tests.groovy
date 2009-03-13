/**
 * Class description here.
 
 * @author Graeme Rocher
 * @since 0.4
  *
 * Created: Aug 9, 2007
 * Time: 4:19:37 PM
 * 
 */

package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.GrailsDomainClass

class CascadingDeleteBehaviour2Tests extends AbstractGrailsHibernateTests {


    void testCascadingDeleteFromChild() {
        def uClass = ga.getDomainClass("UserRecord")
        def iClass = ga.getDomainClass("Item")
        def irClass = ga.getDomainClass("ItemRating")

        def u = uClass.newInstance()
        u.name = "bob"
        u.save(flush:true)



        def i = iClass.newInstance()
        i.name = "stuff"
        i.save(flush:true)

        def ir = irClass.newInstance()

        ir.user = u
        ir.item = i
        ir.rating = 5
        ir.save(flush:true)

        session.clear()

        ir = irClass.clazz.get(1)
        ir.delete()
        session.flush()

        assertEquals 1, uClass.clazz.count()
        assertEquals 1, iClass.clazz.count()
        assertEquals 0, irClass.clazz.count()
    }

    void testDomainModel() {
        GrailsDomainClass ir = ga.getDomainClass("ItemRating")
        GrailsDomainClass uClass = ga.getDomainClass("UserRecord")
        GrailsDomainClass iClass = ga.getDomainClass("Item")

        assert ir.isOwningClass(uClass.clazz)
        assert ir.isOwningClass(iClass.clazz)
        assert !uClass.isOwningClass(ir.clazz)
        assert !iClass.isOwningClass(ir.clazz)
    }

//TODO find out why calling the class User instead of UserRecord clashes with UniqueConstraintTests,
//    there must be some static hibernate reference which is not being cleaned up between tests...
    void onSetUp() {
        gcl.parseClass('''

class UserRecord {
    Long id
    Long version
    static hasMany = [ratings:ItemRating]

    Set ratings
    String name
}

class Item {
    Long id
    Long version
    static hasMany = [ratings:ItemRating]

    Set ratings
    String name
}

class ItemRating {
    Long id
    Long version
    static belongsTo = [UserRecord,Item]

    UserRecord user
    Item item
    int rating
}
        ''')
    }
}