package org.codehaus.groovy.grails.orm.hibernate

import javax.persistence.*

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class JpaMappedDomainTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        ctx.registerMockBean("animal", new Animal())
        ctx.registerMockBean("owner", new Owner())
    }


    void testJpaMappedDomain() {
        assertEquals 0, Animal.count()       
    }

}


/**
 * Created by IntelliJ IDEA.
 * User: sdmurphy
 * Date: Nov 7, 2009
 * Time: 3:12:27 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name = "animal")
class Animal {
  @Id @GeneratedValue
  int id
  String name
  @ManyToOne
  @JoinColumn
  Owner owner
}
@Entity
@Table(name = "owner")
class Owner {
  @Id @GeneratedValue
  int id
  String name
  @OneToMany
  @JoinColumn
  List<Animal> animals
}

