package org.codehaus.groovy.grails.orm.hibernate

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 7/5/11
 * Time: 10:37 AM
 * To change this template use File | Settings | File Templates.
 */
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
