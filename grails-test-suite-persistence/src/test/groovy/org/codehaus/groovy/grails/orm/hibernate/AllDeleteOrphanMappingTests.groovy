package org.codehaus.groovy.grails.orm.hibernate

class AllDeleteOrphanMappingTests extends AbstractGrailsHibernateTests {

    @Override
    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class A {
    static mapping = {
      bees cascade: "all-delete-orphan"
    }

    String val
    static hasMany = [bees: B]
    static belongsTo = [parent: D]
}

@Entity
class B {
    String val
    static belongsTo = [parent: A]
}

@Entity
class D {
    String val
    static hasMany = [ayes: A]
}
''')
    }

    // test for GRAILS-6734
    void testDeleteOrphanMapping() {
        def A = ga.getDomainClass("A").clazz
        def D = ga.getDomainClass("D").clazz
        def a1 = A.newInstance(val: "A1")
        def d1 = D.newInstance(val: "D1")
        d1.addToAyes(a1)
        d1.save()

        def a2 = A.newInstance(val: "A2")
        def d2 = D.newInstance(val: "D2")
        d2.addToAyes(a2)
        d2.save(flush: true)

        session.clear()

        d2 = D.get(2)

        // Initialize d2, then detach it
        d2.ayes.each { it.bees.iterator() }
        d2.discard()

        d2.addToAyes(A.get(1))
        d2 = d2.merge(flush:true)
    }
}
