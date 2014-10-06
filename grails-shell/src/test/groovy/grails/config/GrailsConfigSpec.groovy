package grails.config

import spock.lang.Specification

class GrailsConfigSpec extends Specification{
    
    def "should merge sub-documents in yaml file to single config"() {
        given:
        File file = new File("src/test/resources/grails/config/application.yml")
        GrailsConfig config = new GrailsConfig()
        when:
        config.loadYml(file)
        then:
        config.configMap.a1 == [a2:3, b2:4, c2:[a3:3, b2:4, c3:1], d2: 1, e2: 2]
        config.configMap.grails.profile == 'web'
        config.configMap.grails.containsKey('somekey') == false
    }
    
    def "should support merging maps"() {
        given:
        GrailsConfig config = new GrailsConfig()
        when:
        config.mergeMap([a:1])
        then:
        config.configMap == [a:1]
        when:
        config.mergeMap([b:2])
        then:
        config.configMap == [a:1, b:2]
        when:
        config.mergeMap([a: [c:1]])
        then:
        config.configMap == [a: [c:1], b:2]
        when:
        config.mergeMap([a: [d: 1]])
        then:
        config.configMap == [a: [c:1, d:1], b:2]
        when:
        config.mergeMap([a: [c: 2]])
        then:
        config.configMap == [a: [c:2, d:1], b:2]
        when: 'key has null value'
        config.mergeMap([a: null])
        then: 'the key should be removed'
        config.configMap == [b:2]
    }
    
    def "should support basic type conversions"() {
        given:
        GrailsConfig config = new GrailsConfig()
        when:
        config.mergeMap([intValue:'123', doubleValue:'12.34', longValue:'12345678910111213', bigDecimalValue:'12345678910111213141516.12345678910111213141516', booleanValue: 'Yes', falseValue: 'off'])
        then:
        config.navigate(Integer, 'intValue') == 123
        config.navigate(Double, 'doubleValue') == 12.34d
        config.navigate(Long, 'longValue') == 12345678910111213L
        config.navigate(BigDecimal, 'bigDecimalValue') == new BigDecimal("12345678910111213141516.12345678910111213141516")
        config.navigate(Boolean, 'booleanValue') == true
        config.navigate(Boolean, 'falseValue') == false
    }
    
    def "should support null safe navigation for getting"() {
        given:
        GrailsConfig config = new GrailsConfig()
        expect:
        config.a.b.c as Boolean == false
        config.a.b.c as Map == null
    }

    def "should support null safe navigation for setting"() {
        given:
        GrailsConfig config = new GrailsConfig()
        when:
        config.a.b.c = 1
        then:
        config.configMap == [a: [b: [c: 1]]]
    }

    def "should support merging values when map is set"() {
        given:
        GrailsConfig config = new GrailsConfig()
        when:
        config.a.b.c = 1
        config.a = [d: 2]
        then:
        config.configMap == [a: [b: [c: 1], d: 2]]
        when:
        config.a.b = [e: 3]
        then:
        config.configMap == [a: [b: [c: 1, e: 3], d: 2]]
    }

    def "should support merging values when map already exists"() {
        given:
        GrailsConfig config = new GrailsConfig()
        when:
        config.a.b.c = 1
        config.a = [d: 2]
        then:
        config.configMap == [a: [b: [c: 1], d: 2]]
        when:
        config.a.b.e = 3
        then:
        config.configMap == [a: [b: [c: 1, e: 3], d: 2]]
    }
        
    def "should support setting map in null safe navigation"() {
        given:
        GrailsConfig config = new GrailsConfig()
        when:
        config.a.b.c = [d: 3, e: [f: 4]]
        then:
        config.configMap == [a: [b:[c:[d:3, e:[f:4]]]]]
    }
    
    def "should support cloning"() {
        given:
        GrailsConfig config = new GrailsConfig([a: [b:[c:[d:3, e:[f:4]]]]])
        GrailsConfig config2 = config.clone()
        expect:
        config == config2
        !config.is(config2)
        config.configMap == config2.configMap
        !config.configMap.is(config2.configMap)
        config2.configMap == [a: [b:[c:[d:3, e:[f:4]]]]]
        when:
        config.a.b.hello = 'world'
        then:
        config.a.b.hello == 'world'
        config.configMap == [a: [b:[c:[d:3, e:[f:4]], hello:'world']]]
    }
    
    def "should support removing values when key is set to null"() {
        given:
        GrailsConfig config = new GrailsConfig([a: [b: [c: [d: 1, e: 2]]]])
        when:
        config.a.b = null
        config.a.b.c = 1
        then:
        config.configMap == [a: [b: [c: 1]]]
    }
    
    def "should support casting to Map"() {
        given:
        GrailsConfig config = new GrailsConfig([a: [b: [c: [d: 1, e: 2]]]])
        expect:
        (config as Map) == [a: [b: [c: [d: 1, e: 2]]]]
    }
    
    def "should support casting to boolean"() {
        given:
        GrailsConfig config = new GrailsConfig()
        expect:
        config as boolean == false
        when:
        config.a = 1
        then:
        config as boolean == true
    }
    
    def "should support casting map to GrailsConfig"() {
        given:
        def config = [a: [b: [c: [d: 1, e: 2]]]] as GrailsConfig
        expect:
        config instanceof GrailsConfig
        config.configMap == [a: [b: [c: [d: 1, e: 2]]]]
    }
}
