package grails.config;

import spock.lang.Specification

class ConfigMapSpec extends Specification {

    def "should support flattening keys"() {
        given:
        ConfigMap configMap = new ConfigMap()
        when:
        configMap.a.b.c = 1
        configMap.a.b.d = 2
        then:
        configMap.toFlatConfig() == ['a.b.c': 1, 'a.b.d': 2]
    }

    def "should support flattening list values"() {
        given:
        ConfigMap configMap = new ConfigMap()
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
        ConfigMap configMap = new ConfigMap()
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
        ConfigMap configMap = new ConfigMap()
        configMap.a.b.c = [1, 2, 3]
        configMap.a.b.d = 2
        when: 
        ConfigMap cloned = configMap.clone()
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
