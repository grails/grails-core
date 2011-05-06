package org.codehaus.groovy.grails.orm.hibernate

/**
 * Test for GRAILS-2887.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class CriteriaNegatedAssociationCriterionTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class CNACPerson {
    String name
    static hasMany = [roles:CNACRole]
}

@Entity
class CNACRole {
    String name
}
'''
    }

    // test for GRAILS-2887
    void testNegatedAssociationCriterion() {
        def Person = ga.getDomainClass("CNACPerson").clazz

        assertNotNull Person.newInstance(name:"Bob")
                            .addToRoles(name:"Admin")
                            .save(flush:true)

        assertNotNull Person.newInstance(name:"Fred")
                            .addToRoles(name:"Admin")
                            .save(flush:true)

        assertNotNull Person.newInstance(name:"Joe")
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
