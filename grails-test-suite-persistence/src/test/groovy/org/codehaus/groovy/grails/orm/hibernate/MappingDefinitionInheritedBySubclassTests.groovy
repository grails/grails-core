package org.codehaus.groovy.grails.orm.hibernate

import java.sql.ResultSet

class MappingDefinitionInheritedBySubclassTests extends AbstractGrailsHibernateTests {

    @Override
    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
abstract class MappingDefinitionInheritedParent {
  Boolean active
  static mapping = {
    active type: 'yes_no'
  }
}

@Entity
class MappingDefinitionInheritedChild extends MappingDefinitionInheritedParent {
  String name

  static mapping = {
    sort 'name'
  }
}
''')
    }

    void testMappingInheritance() {
        def Child = ga.getDomainClass("MappingDefinitionInheritedChild").clazz

        Child.newInstance(name:"Fred", active:true).save()
        Child.newInstance(name:"Bob", active:false).save()
        Child.newInstance(name:"Eddie", active:true).save()

        session.clear()
        def results = Child.list()

        assert results.size() == 3
        assert results[0].name == "Bob"
        assert results[1].name == "Eddie"
        assert results[2].name == "Fred"

        ResultSet rs = session.connection().createStatement().executeQuery("select active from mapping_definition_inherited_parent where name = 'Bob'")

        assert rs.next() == true
        assert rs.getString("active") == "N"
    }
}
