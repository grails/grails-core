package org.codehaus.groovy.grails.plugins

import grails.persistence.Entity
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import spock.lang.Specification

class AddToSpec extends Specification {

    def "Test that the addTo method assigns the correct instance of collection"() {
        given:
            def application = new DefaultGrailsApplication([Project, Member] as Class[], getClass().classLoader)
            application.initialise()

            GrailsDomainClass projectClass = application.getDomainClass(Project.name)
            GrailsDomainClass memberClass = application.getDomainClass(Member.name)

            DomainClassGrailsPlugin.addRelationshipManagementMethods(projectClass, null)

            def project = projectClass.newInstance()
            def member = memberClass.newInstance()

        when:
            project.addToManagers(member).addToManagers(member)

        then:
            project.managers.size() == 1
            project.managers instanceof Set

        when:
            project.addToDevelopers(member).addToDevelopers(member)

        then:
            project.developers.size() == 1
            project.developers instanceof SortedSet

        when:
            project.addToTesters(member).addToTesters(member)

        then:
            project.testers.size() == 2
            project.testers instanceof List

        when:
            project.addToSales(member).addToSales(member)

        then:
            project.sales.size() == 2
            project.sales instanceof List
    }

}

@Entity
class Project {
    SortedSet developers
    List testers
    Collection sales

    static hasMany = [
        managers: Member,
        developers: Member,
        testers: Member,
        sales: Member
    ]
}

@Entity
class Member implements Comparable {
    String name

    @Override
    int compareTo(Object o) { name <=> o?.name }
}
