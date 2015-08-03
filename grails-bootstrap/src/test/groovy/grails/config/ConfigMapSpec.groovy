package grails.config

import org.grails.config.NavigableMap
import spock.lang.Issue;
import spock.lang.Specification

class ConfigMapSpec extends Specification {

    def "should support flattening keys"() {
        given:
        NavigableMap configMap = new NavigableMap()
        when:
        configMap.a.b.c = 1
        configMap.a.b.d = 2
        then:
        configMap.toFlatConfig() == ['a.b.c': 1, 'a.b.d': 2]
    }

    @Issue('#9146')
    def "should support hashCode()"() {
        given:
        NavigableMap configMap = new NavigableMap()
        when:
        configMap.a.b.c = 1
        configMap.a.b.d = 2
        then:"hasCode() doesn't cause a Stack Overflow error"
        configMap.hashCode() == configMap.hashCode()
    }

    def "should support flattening list values"() {
        given:
        NavigableMap configMap = new NavigableMap()
        when:
        configMap.a.b.c = [1, 2, 3]
        configMap.a.b.d = 2
        then:
        configMap.toFlatConfig() ==
                ['a.b.c': [1, 2, 3],
                 'a.b.c[0]': 1,
                 'a.b.c[1]': 2,
                 'a.b.c[2]': 3,
                 'a.b.d': 2]
    }
    
    def "should support flattening to properties"() {
        given:
        NavigableMap configMap = new NavigableMap()
        when:
        configMap.a.b.c = [1, 2, 3]
        configMap.a.b.d = 2
        then:
        configMap.toProperties() ==
                ['a.b.c': '1,2,3',
                 'a.b.c[0]': '1',
                 'a.b.c[1]': '2',
                 'a.b.c[2]': '3',
                 'a.b.d': '2']
    }
    
    def "should support cloning"() {
        given:
        NavigableMap configMap = new NavigableMap()
        configMap.a.b.c = [1, 2, 3]
        configMap.a.b.d = 2
        when: 
        NavigableMap cloned = configMap.clone()
        then:
        cloned.toFlatConfig() ==
                ['a.b.c': [1, 2, 3],
                 'a.b.c[0]': 1,
                 'a.b.c[1]': 2,
                 'a.b.c[2]': 3,
                 'a.b.d': 2]
        !cloned.is(configMap)
        cloned == configMap
    }
}
