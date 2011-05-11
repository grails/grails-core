package org.codehaus.groovy.grails.orm.hibernate.binding

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests

 /**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 11/05/2011
 * Time: 11:37
 * To change this template use File | Settings | File Templates.
 */
class DataBindingWithInverseListTests extends AbstractGrailsHibernateTests{
    @Override protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class Item {

  static hasMany = [ parts: Part  ]

  String  name
  List    parts

}

@Entity
class Part {

  static belongsTo = [ item: Item ]

  String name

  static constraints = {
     item nullable:true
  }

}


'''

    }



    // test for GRAILS-3783
    void testBindAndSaveWithInverseListCollection() {
        buildMockRequest()

        def Item = ga.getDomainClass("Item").clazz
        def Part = ga.getDomainClass("Part").clazz


        def item = Item.newInstance(name:"iMac")

        assert item.save(flush:true)  != null

        session.clear()

        def part = Part.newInstance(name:"Intel CPU", 'item.id': item.id)

        assert part.save(flush:true) != null
    }

    // test for GRAILS-6608
    void testCreateManySideWithNoParent() {
        def Part = ga.getDomainClass("Part").clazz

        def part = Part.newInstance(name:"Indel CPU")
        assert part.save(flush:true) != null
    }


    void testCreateWithoutSave() {

        buildMockRequest()

        def Item = ga.getDomainClass("Item").clazz
        def Part = ga.getDomainClass("Part").clazz


        def item = Item.newInstance(name:"iMac")

        assert item.save(flush:true)  != null

        session.clear()

        Part.newInstance('item.id': item.id)

        session.flush()
        session.clear()

        assert Part.count() == 0
    }
}
