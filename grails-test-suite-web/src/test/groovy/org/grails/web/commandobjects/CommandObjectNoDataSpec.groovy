package org.grails.web.commandobjects

import grails.testing.web.GrailsWebUnitTest
import spock.lang.Ignore
import spock.lang.Specification

class CommandObjectNoDataSpec extends Specification implements GrailsWebUnitTest {

    Closure doWithConfig() {{ config ->
        config['grails.gorm.default.constraints'] = {
            isProg inList: ['Emerson', 'Lake', 'Palmer']
        }
    }}

    @Ignore("grails.gorm.validation.exceptions.ValidationConfigurationException at CommandObjectNoDataSpec.groovy:19")
    void "test shared constraint"() {
        when:
        Artist artist = new Artist(name: "X")

        then:
        !artist.validate()
        artist.errors['name'].code == 'not.inList'
    }
}
