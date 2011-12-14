package org.codehaus.groovy.grails.compiler.gorm

import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.GormValidationApi
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.springframework.validation.Errors

import spock.lang.Specification
import grails.persistence.Entity
import org.codehaus.groovy.grails.compiler.injection.DefaultGrailsDomainClassInjector

class GormTransformerSpec extends Specification {

    void "Test missing method thrown for uninitialized entity"() {
        given:
              def gcl = new GrailsAwareClassLoader()
              def gormTransformer = new GormTransformer() {
                  @Override
                  boolean shouldInject(URL url) { true }
              }
              gcl.classInjectors = [gormTransformer] as ClassInjector[]

          when:
              def cls = gcl.parseClass('''
@grails.persistence.Entity
class TestEntity {
    Long id
}
  ''')
             cls.load(1)
          then:
               def e = thrown(MissingMethodException)
               e.message.contains '''No signature of method: TestEntity.load() is applicable for argument types'''
    }

    void "Test that generic information is added to hasMany collections"() {
        given:
              def gcl = new GrailsAwareClassLoader()
              def gormTransformer = new GormTransformer() {
                  @Override
                  boolean shouldInject(URL url) { true }
              }
              def domainTransformer = new DefaultGrailsDomainClassInjector() {
                  @Override
                  boolean shouldInject(URL url) {
                      true
                  }

              }
              gcl.classInjectors = [gormTransformer,domainTransformer] as ClassInjector[]

          when:
              def cls = gcl.parseClass('''
@grails.persistence.Entity
class TestEntity {
    Long id

    static hasMany = [associated:Associated]
}

@grails.persistence.Entity
class Associated {
    Long id
}
  ''')

          then:
             cls.getAnnotation(Entity) != null
             cls.getDeclaredField("associated") != null
             cls.getDeclaredField("associated").genericType != null
             cls.getDeclaredField("associated").genericType.getActualTypeArguments()[0] == gcl.loadClass("Associated")
    }

    void "Test that only one annotation is added on already annotated entity"() {
        given:
              def gcl = new GrailsAwareClassLoader()
              def gormTransformer = new GormTransformer() {
                  @Override
                  boolean shouldInject(URL url) { true }
              }
              gcl.classInjectors = [gormTransformer] as ClassInjector[]

          when:
              def cls = gcl.parseClass('''
@grails.persistence.Entity
class TestEntity {
    Long id
}
  ''')

          then:
             cls.getAnnotation(Entity) != null
    }
    void "Test transforming a @grails.persistence.Entity marked class doesn't generate duplication methods"() {
        given:
              def gcl = new GrailsAwareClassLoader()
              def gormTransformer = new GormTransformer() {
                  @Override
                  boolean shouldInject(URL url) { true }
              }
              gcl.classInjectors = [gormTransformer] as ClassInjector[]

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

    void "Test that GORM static methods are available on transformation"() {
        given:
              def gcl = new GrailsAwareClassLoader()
              def transformer = new GormTransformer() {
                  @Override
                  boolean shouldInject(URL url) {
                      return true;
                  }

              }
              gcl.classInjectors = [transformer] as ClassInjector[]


          when:
              def cls = gcl.parseClass('''
class TestEntity {
    Long id
}
  ''')
              cls.count()

          then:
             thrown MissingMethodException

          when:
            cls.metaClass.static.currentGormStaticApi = {-> null}
            cls.count()

          then:
            thrown MissingMethodException


          when:
            def ds = new SimpleMapDatastore()
            ds.mappingContext.addPersistentEntity(cls)

            cls.metaClass.static.currentGormStaticApi = {-> new GormStaticApi(cls, ds, [])}

          then:
            cls.count() == 0

    }

    void "Test that the new Errors property is valid"() {
        given:
              def gcl = new GrailsAwareClassLoader()
              def transformer = new GormValidationTransformer() {
                  @Override
                  boolean shouldInject(URL url) {
                      return true;
                  }

              }
              gcl.classInjectors = [transformer] as ClassInjector[]


          when:
              def cls = gcl.parseClass('''
class TestEntity {
    Long id
    Long version
    String name
}
  ''')
              def dc = new DefaultGrailsDomainClass(cls)

          then:
              dc.persistentProperties.size() == 1

          when:
              def obj = dc.newInstance()

          then:
             obj != null
             obj.errors instanceof Errors

          when:
             def ds = new SimpleMapDatastore()

             cls.metaClass.static.currentGormValidationApi = {-> new GormValidationApi(cls, ds)}
             obj.clearErrors()

          then:
             obj.errors.hasErrors() == false
             obj.hasErrors() == false

          when:
             Errors errors = obj.errors
             errors.reject("bad")

          then:
             obj.hasErrors() == true
    }
}
