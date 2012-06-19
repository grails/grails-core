package org.codehaus.groovy.grails.orm.hibernate

class DefaultIndexDefinitionTests extends AbstractGrailsHibernateTests{

    @Override
    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class DefaultIndexDefinition {
    String name
    static mapping = {
        name index:true
    }
}
''')
    }

    void testDefaultIndex() {
        def DefaultIndexDefinition = ga.getDomainClass("DefaultIndexDefinition").clazz
        def did = DefaultIndexDefinition.newInstance(name:"Bob").save()
        assert did != null
    }
}
