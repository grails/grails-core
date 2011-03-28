package org.codehaus.groovy.grails.compiler.gorm

import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import org.springframework.datastore.mapping.simple.SimpleMapDatastore
import spock.lang.Specification
import org.grails.datastore.gorm.GormStaticApi

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 28/03/2011
 * Time: 16:28
 * To change this template use File | Settings | File Templates.
 */
class GormTransformerSpec extends Specification {

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
             thrown IllegalStateException

          when:
            cls.metaClass.static.currentGormStaticApi = {-> null}
            cls.count()

          then:
            thrown NullPointerException


          when:
            def ds = new SimpleMapDatastore()
            ds.mappingContext.addPersistentEntity(cls)

            cls.metaClass.static.currentGormStaticApi = {-> new GormStaticApi(cls, ds)}

          then:
            cls.count() == 0

    }
}
