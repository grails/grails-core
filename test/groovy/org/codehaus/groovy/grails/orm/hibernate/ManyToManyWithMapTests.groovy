package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.GrailsDomainClass

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 22, 2009
 */

public class ManyToManyWithMapTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Foo {
  static hasMany = [bars:Bar]
  Map bars = new HashMap()
}
@Entity
class Bar {
  static belongsTo = Foo
  static hasMany = [parents:Foo]
}
''')
    }


    void testManyToManyWithMapDomain() {
        GrailsDomainClass fooClass = ga.getDomainClass("Foo")
        GrailsDomainClass barClass= ga.getDomainClass("Bar")

        assertTrue "should be many-to-many",fooClass.getPropertyByName("bars").isManyToMany()
        assertTrue "should be an association",fooClass.getPropertyByName("bars").isAssociation()

        assertTrue "should be many-to-many",barClass.getPropertyByName("parents").isManyToMany()
        assertTrue "should be an association",barClass.getPropertyByName("parents").isAssociation()

    }

    void testManyToManyWithMap() {
        def fooClass = ga.getDomainClass("Foo").clazz
        def barClass= ga.getDomainClass("Bar").clazz


        def foo = fooClass.newInstance()
        foo.bars['bar1'] = barClass.newInstance()

        assertNotNull "should have saved",foo.save(flush:true)

        session.clear()

        foo = fooClass.get(1)

        assertEquals 1, foo.bars.size()
        assertNotNull foo.bars['bar1']

        session.clear()

        def bar = barClass.get(1)

        assertEquals 1, bar.parents.size()

    }

}