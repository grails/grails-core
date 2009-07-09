/**
 * @author Graeme Rocher
 */
package org.codehaus.groovy.grails.orm.hibernate
class InheritanceWithAssociationsTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass('''
class A {
    Long id
    Long version
    LinkToA link
}
class B {
    Long id
    Long version
    LinkToB link
}
class Link {
    Long id
    Long version

    static belongsTo = Root

    Root root

    static constraints = {
        root(nullable:true)
    }
}
class LinkToA extends Link {
    Long id
    Long version

    static belongsTo = A
    A a

}
class LinkToB {
    Long id
    Long version

    static belongsTo = B
    B b
}
class Root {
    Long id
    Long version

    Set links
    static hasMany = [links : Link]

} 
''')
    }


    void testMapping() {
         def rootClass = ga.getDomainClass("Root")
         def aClass = ga.getDomainClass("A")
         def linkToAClass = ga.getDomainClass("LinkToA")

         def root = rootClass.newInstance()
         def a = aClass.newInstance()
         def link = linkToAClass.newInstance()
         link.a = a
         link.root = root
         a.link = link
         
         a.save()

         root.addToLinks(link)
         root.save()
         session.flush()
         session.clear()

         root = rootClass.clazz.get(1)
         assert root
         assertEquals 1, root.links.size()

         link = root.links.iterator().next()
         assert link
         assert link.a

    }
}