package org.codehaus.groovy.grails.orm.hibernate

import javax.persistence.*
import java.security.acl.Owner

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class JpaMappedDomainTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import javax.persistence.*

@Entity
@Table(name = "animal")
class Animal {

  @Id @GeneratedValue
  int id
  String name
  @ManyToOne
  @JoinColumn
  Owner owner


  static constraints = {
        name blank:false
  }
}
@Entity
@Table(name = "owner")
class Owner {
   static changeMe = "one"
  def beforeInsert() {
      Owner.changeMe = "two"  
  }

  @Id @GeneratedValue
  int id
  String name
  @OneToMany
  @JoinColumn
  List<Animal> animals
}


''')
    }


    void testJpaMappedDomain() {
        def Animal = ga.getDomainClass("Animal").clazz
        def Owner = ga.getDomainClass("Owner").clazz

        assertEquals 0, Animal.count()

        def owner = Owner.newInstance(name:"Bob")
        assert owner.save(flush:true) : "should have saved owner"

        def animal = Animal.newInstance(name:"Dog", owner:owner)
        animal.save()

        assertEquals 1, Animal.count()
        assertEquals 1, Owner.count()
    }

    void testValidation() {
         def Animal = ga.getDomainClass("Animal").clazz

         def a = Animal.newInstance(name:"")

        assert !a.save(flush:true) : "should not have validated"

        assertEquals 0, Animal.count()
    }

    void testEvents() {
        def Owner = ga.getDomainClass("Owner").clazz

        assertEquals "one", Owner.changeMe

        def owner = Owner.newInstance(name:"Bob")
        assert owner.save(flush:true) : "should have saved owner"

        assertEquals "two", Owner.changeMe

    }

}

