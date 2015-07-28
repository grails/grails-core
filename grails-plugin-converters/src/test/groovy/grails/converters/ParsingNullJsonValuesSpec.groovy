package grails.converters

import spock.lang.Specification

class ParsingNullJsonValuesSpec extends Specification {

    def "test parsing null value"() {
        expect:
        JSON.parse("{'myList':null}").myList == null
        JSON.parse("{'myList':null}").get('myList') == null
    }
}
