package org.codehaus.groovy.grails.orm.hibernate

class CustomComponentMappingTests extends  AbstractGrailsHibernateTests {
    @Override
    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class CustomComponentMappingParent {
  CustomComponentMappingComponent component
  static embedded = ["component"]
}

//@Entity
class CustomComponentMappingComponent {
  BigInteger property
  static mapping = {
     property(column: "prop")
  }
}
''')
    }

    // Related to GRAILS-5447
    void testCustomEmbeddedComponentMapping() {
        def Parent = ga.getDomainClass("CustomComponentMappingParent").clazz
        def Component = ga.classLoader.loadClass("CustomComponentMappingComponent")

        def p = Parent.newInstance(component:Component.newInstance(property:10))

        assert p.save(flush:true) != null

        session.clear()

        p = Parent.get(p.id)

        assert p.component != null
        assert p.component.property == 10

        session.connection().prepareStatement("select prop from custom_component_mapping_parent").execute()
    }
}
