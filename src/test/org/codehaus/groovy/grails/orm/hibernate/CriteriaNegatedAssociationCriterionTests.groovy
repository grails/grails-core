package org.codehaus.groovy.grails.orm.hibernate
/**
 *
 * test for GRAILS-2887
 *
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 7, 2008
 */

public class CriteriaNegatedAssociationCriterionTests extends AbstractGrailsHibernateTests{

  protected void onSetUp() {
    gcl.parseClass('''
class CNACPerson {
    Long id
    Long version
    String name
    Set roles
    static hasMany = [roles:CNACRole]
}

class CNACRole {
  Long id
  Long version
  String name
}
''')
  }


  // test for GRAILS-2887
  void testNegatedAssociationCriterion() {
     def Person = ga.getDomainClass("CNACPerson").clazz

     assert Person.newInstance(name:"Bob")
           .addToRoles(name:"Admin")
            .save(flush:true)

    assert Person.newInstance(name:"Fred")
          .addToRoles(name:"Admin")
           .save(flush:true)

    assert Person.newInstance(name:"Joe")
          .addToRoles(name:"Lowlife")
           .save(flush:true)


    def results = Person.withCriteria {
      not {
        roles {
          eq('name', 'Admin')
        }
      }
      
    }


    assertEquals 1, results.size()
    assertEquals "Joe",  results[0].name

    results = Person.withCriteria {
      roles {
        eq('name', 'Admin')
      }
    }


    assertEquals 2, results.size()


    results = Person.withCriteria {
      roles {
        ne('name', 'Admin')
      }
    }

    assertEquals 1, results.size()
    assertEquals "Joe",  results[0].name

    
  }
}