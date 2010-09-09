package org.codehaus.groovy.grails.orm.hibernate

class JavaInheritanceTests extends AbstractGrailsHibernateTests{
    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class MyDomainClass extends org.codehaus.groovy.grails.orm.hibernate.AbstractJavaClass {
  String someProperty
}
'''
    }

    void testInheritingFromAbstractJavaClass() {
        def mdc = ga.getDomainClass("MyDomainClass")
        assertNotNull 'MyDomainClass is not considered a domain class', mdc
    }
}
