package org.codehaus.groovy.grails.orm.hibernate


/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class InheritedUniqueConstraintTests extends AbstractGrailsHibernateTests{

  protected void onSetUp() {
    gcl.parseClass('''
class Parent {

    Long id
    Long version
    String username

    static constraints = {
        username(nullable: false, unique:true)
    }

}

class Child extends Parent {
}
''')
  }


  void testInheritedUniqueConstraint() {
      def Parent = ga.getDomainClass("Parent").clazz
      def Child = ga.getDomainClass("Child").clazz

        def child1 = Child.newInstance(username:'mos')
        assertNotNull "should have saved unqiue child",child1.save(flush:true)

        def child2 = Child.newInstance(username:'mos')
        assertNull "should not have saved non-unqiue child",child2.save(flush:true)

        //// now with parent

        def parent1 = Parent.newInstance(username:'graeme')
        assertNotNull "should have saved unqiue parent",parent1.save(flush:true)

        def child = Child.newInstance(username:'graeme')
        assertNull "should not have saved non-unqiue child",child.save(flush:true)
  }
}