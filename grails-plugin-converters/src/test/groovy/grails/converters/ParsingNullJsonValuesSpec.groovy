package grails.converters

import spock.lang.Specification

class ParsingNullJsonValuesSpec extends Specification {

    def "test parsing null value"() {
        expect:
        JSON.parse("{'myList':null}").myList == null
        JSON.parse("{'myList':null}").get('myList') == null
    }

    def "test the remove method returns null, not JSONObject.Null"() {
        when:
        def obj = JSON.parse("{'myList':null}")

        then:
        obj.remove('myList') == null
    }
}
