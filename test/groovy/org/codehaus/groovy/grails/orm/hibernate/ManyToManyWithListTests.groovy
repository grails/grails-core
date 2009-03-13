package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 22, 2009
 */

public class ManyToManyWithListTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Foo {
  static hasMany = [bars:Bar  ]
  List bars
}
@Entity
class Bar {
  static belongsTo = Foo
  static hasMany = [parents:Foo]
}
''')
    }


    void testManyToManyWithList() {
        def oneClass = ga.getDomainClass("Foo").clazz
        def twoClass= ga.getDomainClass("Bar").clazz


        def one = oneClass.newInstance()
        one.addToBars(twoClass.newInstance())

        assertNotNull "should have saved",one.save(flush:true)

        session.clear()

        one = oneClass.get(1)

        assertEquals 1, one.bars.size()
        assertNotNull one.bars[0]


        session.clear()

        def two = twoClass.get(1)

        assertEquals 1, two.parents.size()
        
    }

}