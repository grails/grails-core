package grails.config

import org.grails.config.NavigableMap
import spock.lang.Issue;
import spock.lang.Specification

class ConfigMapSpec extends Specification {

    def "should support merging ConfigObject maps"() {
        given:
        NavigableMap configMap = new NavigableMap()
        def config = new ConfigSlurper().parse('''
foo {
    bar = "good"
}
test.another = true
''')

        when:"a config object is merged"
        configMap.merge(config)

        then:"The merge is correct"
        configMap.size() == 4
        configMap['test'] instanceof NavigableMap
        configMap['test.another']  == true
    }

    def "should support merge correctly"() {
        given:
        NavigableMap configMap = new NavigableMap()
        when:
        configMap.merge(['foo.bar':'good1', bar:[foo:'good2']], true)
        then:
        configMap.foo.bar == 'good1'
        configMap.getProperty('foo.bar') == 'good1'
        configMap.bar.foo == 'good2'
        configMap.getProperty('bar.foo') == 'good2'

        when:
        configMap.merge(['foo.two':'good3', bar:[two:'good4']], true)
        configMap.merge(['grails.codegen.defaultPackage':"test"])
        configMap.merge([grails:[codegen:[defaultPackage:"test"]]])
        configMap.merge(['grails.codegen':[defaultPackage:"test"]], true)

        then:
        configMap.size() == 9
        configMap.containsKey('grails.codegen.defaultPackage')
        configMap.getProperty('grails.codegen.defaultPackage') == 'test'
        configMap.grails.codegen.defaultPackage == 'test'
        configMap.foo.bar == 'good1'
        configMap.getProperty('foo.bar') == 'good1'
        configMap.bar.foo == 'good2'
        configMap.getProperty('bar.foo') == 'good2'

        configMap.foo.two == 'good3'
        configMap.getProperty('foo.two') == 'good3'
        configMap.bar.two == 'good4'
        configMap.getProperty('bar.two') == 'good4'

    }
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
