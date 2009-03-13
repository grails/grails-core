package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.Hibernate

/**
 *
 * test for GRAILS-2923
 *
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 10, 2008
 */

public class MappingDslAndLazyFetchingTests  extends AbstractGrailsHibernateTests{

  protected void onSetUp() {
    gcl.parseClass('''
class Actor {

  Long id
  Long version

  static belongsTo = Film
  Set films
  static hasMany = [films:Film]

  String firstName
  String lastName

  static mapping = {
    id column:'actor_id'
    films column:'film_id'
  }
}

class Film {
  Long id
  Long version

  String name
  Set actors
  static hasMany = [actors:Actor]

  static mapping = {
     actors column:'actor_id'
  }
}

''')
  }


  // test for GRAILS-2923
  void testMappingDslAndLazyFetching() {
      def Actor = ga.getDomainClass("Actor").clazz
      def Film = ga.getDomainClass("Film").clazz

      def a = Actor.newInstance(firstName:"Edward", lastName:"Norton")

      def f = Film.newInstance(name:"American History X")
      f.addToActors(a)
      assert f.save(flush:true)


      session.clear()


      f = Film.get(1)

      assertFalse "lazy loading should be the default", Hibernate.isInitialized(f.actors)
  }

}