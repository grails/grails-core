package org.codehaus.groovy.grails.orm.hibernate.binding

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests

class DataBindingWithInverseListTests extends AbstractGrailsHibernateTests{
    @Override protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class Item {

  static hasMany = [ parts: Part , nullableParts: NullablePart ]
  String  name
  List    parts
  List    nullableParts

}

@Entity
class Part {

  static belongsTo = [ item: Item ]

  String name


}

@Entity
class NullablePart {

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

        session.clear()

        item = Item.get(item.id)

        assert item.parts.size() == 1
        assert Part.count() == 1
        assert Part.get(part.id).item != null
    }

      // test for GRAILS-3783
    void testBindAndSaveWithNullableManySideAndInverseListCollection() {
        buildMockRequest()

        def Item = ga.getDomainClass("Item").clazz
        def Part = ga.getDomainClass("NullablePart").clazz


        def item = Item.newInstance(name:"iMac")

        assert item.save(flush:true)  != null

        session.clear()

        def part = Part.newInstance(name:"Intel CPU", 'item.id': item.id)

        assert part.save(flush:true) != null

        item = Item.get(item.id)
        item.addToNullableParts(part)
        item.save(flush:true)

        session.clear()

        item = Item.get(item.id)

        assert item.nullableParts.size() == 1
        assert Part.count() == 1
        assert Part.get(part.id).item != null
    }

    // test for GRAILS-6608
    void testCreateManySideWithNoParent() {
        def Part = ga.getDomainClass("NullablePart").clazz

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
