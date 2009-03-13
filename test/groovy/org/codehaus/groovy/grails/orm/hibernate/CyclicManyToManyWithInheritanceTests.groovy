package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.GrailsDomainClass


/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Dec 4, 2008
 * Time: 12:03:41 PM
 * To change this template use File | Settings | File Templates.
 */

public class CyclicManyToManyWithInheritanceTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
class CyclicManyToManyWithInheritanceIndividual extends CyclicManyToManyWithInheritanceUser {
}
class CyclicManyToManyWithInheritanceUser {

    Long id
    Long version

    String name

    Set groups
    static hasMany = [ groups: CyclicManyToManyWithInheritanceUserGroup ]
}
class CyclicManyToManyWithInheritanceUserGroup extends CyclicManyToManyWithInheritanceUser {

    static belongsTo = [ CyclicManyToManyWithInheritanceIndividual, CyclicManyToManyWithInheritanceUser, CyclicManyToManyWithInheritanceUserGroup ]

    Set members
    static hasMany = [ members: CyclicManyToManyWithInheritanceUser ]

}

''')
    }


    void testDomain() {
        GrailsDomainClass individualDomain = ga.getDomainClass("CyclicManyToManyWithInheritanceIndividual")
        GrailsDomainClass userDomain = ga.getDomainClass("CyclicManyToManyWithInheritanceUser")
        def userGroupDomain = ga.getDomainClass("CyclicManyToManyWithInheritanceUserGroup")

        assertTrue "should be a many-to-many assocation",userGroupDomain.getPropertyByName("members").isManyToMany()
        assertTrue "should be a many-to-many assocation",userDomain.getPropertyByName("groups").isManyToMany()
        assertTrue "should be a many-to-many assocation",individualDomain.getPropertyByName("groups").isManyToMany()

    }

    void testCyclicManyToManyWithInheritance() {
        def Individual = ga.getDomainClass("CyclicManyToManyWithInheritanceIndividual").clazz
        def UserGroup = ga.getDomainClass("CyclicManyToManyWithInheritanceUserGroup").clazz

        def wallace = Individual.newInstance( name: "Wallace" )
        def gromit = Individual.newInstance( name: "Gromit" )
        def cheeseLovers = UserGroup.newInstance( name: "Cheese Lovers" )
        def cooker = Individual.newInstance( name: "Cooker" )
        def lunaphiles = UserGroup.newInstance( name: "Lunaphiles" )
        wallace.addToGroups(cheeseLovers)
        gromit.addToGroups(cheeseLovers)
        cooker.addToGroups(lunaphiles)
        // here's the line that causes the problem
        cheeseLovers.addToGroups(lunaphiles)
        wallace.save()
        gromit.save()
        cooker.save()
    }

}