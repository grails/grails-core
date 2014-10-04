package grails.config;

import spock.lang.Specification

class GrailsConfigSpec extends Specification{
    
    def "should merge sub-documents in yaml file to single config"() {
        given:
        File file = new File("src/test/resources/grails/config/application.yml")
        GrailsConfig config = new GrailsConfig()
        when:
        config.loadYml(file)
        then:
        config.config.a1 == [a2:3, b2:4, c2:[a3:3, b2:4, c3:1], d2: 1, e2: 2]
        config.config.grails.profile == 'web'
        config.config.grails.containsKey('somekey') == false
    }
    
    def "should support merging maps"() {
        given:
        GrailsConfig config = new GrailsConfig()
        when:
        config.mergeMap([a:1])
        then:
        config.config == [a:1]
        when:
        config.mergeMap([b:2])
        then:
        config.config == [a:1, b:2]
        when:
        config.mergeMap([a: [c:1]])
        then:
        config.config == [a: [c:1], b:2]
        when:
        config.mergeMap([a: [d: 1]])
        then:
        config.config == [a: [c:1, d:1], b:2]
        when:
        config.mergeMap([a: [c: 2]])
        then:
        config.config == [a: [c:2, d:1], b:2]
        when: 'key has null value'
        config.mergeMap([a: null])
        then: 'the key should be removed'
        config.config == [b:2]
    }
    
    def "should support basic type conversions"() {
        given:
        GrailsConfig config = new GrailsConfig()
        when:
        config.mergeMap([intValue:'123', doubleValue:'12.34', longValue:'12345678910111213', bigDecimalValue:'12345678910111213141516.12345678910111213141516', booleanValue: 'Yes', falseValue: 'off'])
        then:
        config.navigateConfigForType(Integer, 'intValue') == 123
        config.navigateConfigForType(Double, 'doubleValue') == 12.34d
        config.navigateConfigForType(Long, 'longValue') == 12345678910111213L
        config.navigateConfigForType(BigDecimal, 'bigDecimalValue') == new BigDecimal("12345678910111213141516.12345678910111213141516")
        config.navigateConfigForType(Boolean, 'booleanValue') == true
        config.navigateConfigForType(Boolean, 'falseValue') == false
    }

}
