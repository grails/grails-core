package org.grails.web.commandobjects

import grails.testing.web.GrailsWebUnitTest
import spock.lang.Specification

class CommandObjectNoDataSpec extends Specification implements GrailsWebUnitTest {

    Closure doWithConfig() {{ config ->
        config['grails.gorm.default.constraints'] = {
            isProg inList: ['Emerson', 'Lake', 'Palmer']
        }
    }}

    void "test shared constraint"() {
        when:
        Artist artist = new Artist(name: "X")

        then:
        !artist.validate()
        artist.errors['name'].code == 'not.inList'
    }
}
