package org.codehaus.groovy.grails.compiler.web.converters

import grails.converters.XML

import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader

import spock.lang.Specification

class ConvertersDomainTransformerSpec extends Specification {

    void "Test transforming a @grails.persistence.Entity marked class doesn't generate duplication methods"() {
        given:
            def gcl = new GrailsAwareClassLoader()
            def convertersDomainTransformer = new ConvertersDomainTransformer() {
                @Override
                boolean shouldInject(URL url) { true }
            }
            gcl.classInjectors = [convertersDomainTransformer] as ClassInjector[]

        when:
            def cls = gcl.parseClass('''
@grails.persistence.Entity
class TestEntity {
    Long id
}
  ''')

        then:
            cls
    }

    void "Test domain type conversion methods added at compile time"() {
        given:
            def gcl = new GrailsAwareClassLoader()
            def transformer = new ConvertersDomainTransformer() {
                @Override
                boolean shouldInject(URL url) { true }
            }
            gcl.classInjectors = [transformer] as ClassInjector[]

        when:
            def cls = gcl.parseClass('''

class ConvertMe {
    String name
}

''')

            def xml = cls.newInstance(name:"Bob") as XML

        then:
            xml != null
            xml instanceof XML
    }
}
