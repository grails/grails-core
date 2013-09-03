package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import spock.lang.Issue

/**
 * @author Graeme Rocher
 */
class OneToManyJoinTableSpec extends GormSpec {

    @Issue('GRAILS-10308')
    void "Test that a join table mapping works correctly with a one-to-many association"() {
        when:"A one-to-many with a custom join table mapping is persisted"
            def o = new Organization(name:"Foo Bar")
            o.addToQuestions(text:"Is this right?")
            o.save(flush:true)
            def conn = session.connection()

        then:"The table structure is correct"
            conn.prepareStatement("select organisationId from Organisation").execute()
            conn.prepareStatement("select questionId, organisationId from QuestionOrganisation").execute()

    }

    @Override
    List getDomainClasses() {
        return [Organization, Question]
    }
}

@Entity
class Organization {

    static mapping = {
        table 'Organisation'
        id column: 'organisationId'
        questions column: 'questionOrganisationId', joinTable: [ name: 'QuestionOrganisation', column: 'questionId', key: 'organisationId' ]
        version false
    }
    static constraints = {
    }
    String name

    static hasMany = [questions:Question]
}

@Entity
class Question {
    String text
}