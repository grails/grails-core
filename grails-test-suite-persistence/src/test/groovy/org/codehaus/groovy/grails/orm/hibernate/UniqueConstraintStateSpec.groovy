package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity


class UniqueConstraintStateSpec extends GormSpec {

    void "Test that the domain validator state is correct for a unique constraint"() {
        when:"The domain is retrieved from the application"
            def domain = grailsApplication.getDomainClass(UniqueThing.name)

        then:"The domain class is valid"
            domain != null
            domain.validator != null

        when:"The unique thing is validated"
            def ut = new UniqueThing()
            ut.validate()
            domain = grailsApplication.getDomainClass(UniqueThing.name)
        then:"The domain class is valid"
            domain != null
            domain.validator != null

    }

    @Override
    List getDomainClasses() {
        [UniqueThing]
    }
}
@Entity
class UniqueThing {

    String name
    static constraints = {
        name unique:true
    }
}
