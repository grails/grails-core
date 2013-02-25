package org.codehaus.groovy.grails.compiler.gorm

import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import org.grails.datastore.gorm.GormValidationApi
import org.grails.datastore.mapping.simple.SimpleMapDatastore

import spock.lang.Specification

class GormValidationTransformerSpec extends Specification {

    void "Test that the validate methods are available via an AST transformation"() {
        given:
              def gcl = new GrailsAwareClassLoader()
              def transformer = new GormValidationTransformer() {
                  boolean shouldInject(URL url) { true }
              }
              gcl.classInjectors = [transformer] as ClassInjector[]

          when:
              def cls = gcl.parseClass('''
class TestEntity {
    Long id

    String name
}
  ''')
              def obj = cls.newInstance()
              obj.validate()

          then:
             thrown IllegalStateException

          when:

             def ds = new SimpleMapDatastore()
             ds.mappingContext.addPersistentEntity(cls)
             cls.metaClass.static.currentGormValidationApi = {-> new GormValidationApi(cls, ds)}

          then:
             obj.validate() == true
    }
}