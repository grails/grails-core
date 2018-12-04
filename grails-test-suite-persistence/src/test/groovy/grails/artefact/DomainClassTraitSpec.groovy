package grails.artefact

import grails.gorm.annotation.Entity
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification

class DomainClassTraitSpec extends Specification {

    void 'test that a class marked with @Artifact("Domain") is enhanced with the DomainClass trait'() {
        expect:
        DomainClass.isAssignableFrom SomeDomainClass
    }

    @Issue("https://github.com/grails/grails-core/issues/11187")
    @Ignore
    void 'test that a class marked with @Entity is enhanced with the DomainClass trait'() {
        expect:
        DomainClass.isAssignableFrom SomeOtherDomainClass
    }
}


@Artefact('Domain')
class SomeDomainClass{}

@Entity
class SomeOtherDomainClass{}

