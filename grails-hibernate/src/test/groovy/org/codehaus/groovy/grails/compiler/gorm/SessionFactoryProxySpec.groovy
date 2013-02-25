package org.codehaus.groovy.grails.compiler.gorm

import grails.spring.BeanBuilder

import org.codehaus.groovy.grails.orm.hibernate.SessionFactoryHolder
import org.codehaus.groovy.grails.orm.hibernate.SessionFactoryProxy
import org.hibernate.dialect.H2Dialect
import org.springframework.context.ApplicationContext
import org.springframework.orm.hibernate3.LocalSessionFactoryBean

import spock.lang.Specification

 /**
 * Tests for the SessionFactoryProxy class
 */
class SessionFactoryProxySpec extends Specification{

    void "Ensure that SessionFactoryProxy patches SessionFactoryImpl with an appropriate SpringSessionContext"() {
        given:
           def ctx = applicationContext
        when:
           def sessionFactory = ctx.getBean("sessionFactoryHolder").sessionFactory
           def sessionFactoryProxy = ctx.getBean("sessionFactoryProxy")

        then:
           sessionFactory.@currentSessionContext.@sessionFactory == sessionFactoryProxy
    }

    void "Verify that we can access other properties of the SessionFactoryImpl via the proxy in Groovy code"() {
        given:
            def ctx = applicationContext
            def sessionFactoryProxy = ctx.getBean("sessionFactoryProxy")

        when:
            def eventListeners = sessionFactoryProxy.eventListeners

        then:
            eventListeners != null
    }

    ApplicationContext getApplicationContext() {
        BeanBuilder bb = new BeanBuilder()
        bb.beans {

            sessionFactoryHolder(SessionFactoryHolder) {
                sessionFactory =  bean(LocalSessionFactoryBean) {
                    hibernateProperties = ["hibernate.dialect":H2Dialect.name]
                }
            }

            sessionFactoryProxy(SessionFactoryProxy) {
                targetBean = "sessionFactoryHolder"
            }
        }
        bb.createApplicationContext()
    }
}
