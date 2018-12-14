package grails.config

import org.grails.config.NavigableMap
import spock.lang.Specification
import spock.lang.Unroll

class NavigableMapSpec extends Specification {

    @Unroll
    def "#input "(Map input) {

        when:
        Map output = NavigableMap.collapseKeysWithSubscript(input)

        then:
        output.keySet() as List<String> == ['xml', 'grails.cors.mappings[/api/**]', 'js', 'json']
        output['js'] == 'text/javascript'
        output['json'] == ['application/json', 'text/json']
        output['xml'] == ['application/hal+xml', 'text/xml', 'application/xml']
        output['grails.cors.mappings[/api/**]'] == 'default'

        where:
        input << [
                [js: 'text/javascript', json: ['application/json', 'text/json'], xml: ['application/hal+xml', 'text/xml', 'application/xml'], 'grails.cors.mappings[/api/**]': 'default'],
                [js: 'text/javascript', 'json[0]': 'application/json', 'json[1]': 'text/json', 'xml[0]': 'application/hal+xml', 'xml[1]':'text/xml', 'xml[2]':'application/xml', 'grails.cors.mappings[/api/**]': 'default'],
                ]
    }

    @Unroll
    def "for #key keyWithoutSubscript => #expected "(Object key, Object expected) {
        expect:
        expected == NavigableMap.keyWithoutSubscript(key)

        where:
        key                             | expected
        'json'                          | 'json'
        'json[0]'                       | 'json'
        'json[10]'                      | 'json'
        'grails.cors.mappings[/api/**]' | 'grails.cors.mappings[/api/**]'
        2                               | 2
    }




}
