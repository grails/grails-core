package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

class UndirectionalOneToManyConstructorCreationSpec extends GormSpec {

    void "Test that a unidirectional one-to-many association can be created with a single line using the constructor"() {
        when:"A undirectional one-to-many association is created"
            def trnmt = new Tournament (title:'Facebook Oscars',
                                        tags: [new Tag(name:"top commenter"),
                                               new Tag(name:"top liker") ]).save(flush:true)

        then:"The association is valid"
            trnmt != null
            trnmt.tags.size() == 2

        when:"The association is queried"
            session.clear()
            trnmt = Tournament.get(trnmt.id)

        then:"The association is valid"
            trnmt != null
            trnmt.tags.size() == 2
    }

    @Override
    List getDomainClasses() {
        [Tag, Tournament]
    }
}

@Entity
class Tag {
    String name
}

@Entity
class Tournament {
    String title
    List tags
    static hasMany = [tags: Tag]
}
