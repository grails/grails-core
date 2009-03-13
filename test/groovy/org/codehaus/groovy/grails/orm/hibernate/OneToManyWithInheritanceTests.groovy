/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Sep 21, 2007
 */
package org.codehaus.groovy.grails.orm.hibernate

import org.springframework.util.Log4jConfigurer

class OneToManyWithInheritanceTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {        
        gcl.parseClass('''
class OwnerObject {
    Long id
    Long version
    String name

    Set class1
    Set class2
    static hasMany = [class1: SubClass1, class2: SubClass2]
}
class SubClass1 extends BaseClass {
    Long id
    Long version
    String name

    String toString() {
        return "SubClass1 - $name"
    }
}
class SubClass2 extends BaseClass {
    Long id
    Long version
    String otherField

    String toString() {
        return "SubClass2 - $otherField"
    }
}
class BaseClass {
    Long id
    Long version
    OwnerObject owner
    Date created = new Date()

    static belongsTo = OwnerObject
}       
        ''')
    }


    void testPersistentAndLoad() {
		def ownerClass = ga.getDomainClass("OwnerObject")
        def owner =  ownerClass.newInstance()
        owner.name = "The Owner"

        def s1 =  ga.getDomainClass("SubClass1").newInstance()
        s1.name = "An Object"
        s1.owner = owner

        def s2 =  ga.getDomainClass("SubClass2").newInstance()
        s2.otherField = "The Field"
        s2.owner = owner

		owner.addToClass1(s1)
		owner.addToClass2(s2)
		owner.save()

		session.flush()
		session.clear()

		owner = ownerClass.clazz.get(1)

		s1 = owner.class1.iterator().next()
		s2 = owner.class2.iterator().next()

		assert s1
		assert s2

		assertEquals "An Object", s1.name
		assertEquals "The Field", s2.otherField
    }


}