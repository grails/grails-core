package org.codehaus.groovy.grails.commons.cfg

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.plugins.CoreGrailsPlugin
import org.springframework.context.groovy.GroovyBeanDefinitionReader

import spock.lang.Issue
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class GrailsPlaceHolderConfigurerCorePluginRuntimeSpec extends Specification{

    @Issue('GRAILS-10130')
    void "Test that system properties are used to replace values at runtime with GrailsPlaceHolderConfigurer"() {
        given:"A configured application context"
            def parent = new GroovyBeanDefinitionReader()
            parent.beans {
                grailsApplication(DefaultGrailsApplication)
            }
            def bb = new GroovyBeanDefinitionReader(parent.createApplicationContext())

            final beanBinding = new Binding()
            beanBinding.setVariable('application', new DefaultGrailsApplication())
            bb.setBinding(beanBinding)
            bb.beans new CoreGrailsPlugin().doWithSpring
            bb.beans {
                testBean(ReplacePropertyBean) {
                    foo = '${foo.bar}'
                }
            }

        when:"A system property is used in a bean property"
            System.setProperty('foo.bar', "test")
            final appCtx = bb.createApplicationContext()
            def bean = appCtx.getBean("testBean")

        then:"The system property is ready"
            appCtx != null
            bean.foo == 'test'
    }

}
class ReplacePropertyBean {
    String foo
}

