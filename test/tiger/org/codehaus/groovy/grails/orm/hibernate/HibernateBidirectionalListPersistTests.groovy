package org.codehaus.groovy.grails.orm.hibernate

import grails.spring.BeanBuilder
import org.apache.commons.dbcp.BasicDataSource
import org.hibernate.cfg.AnnotationConfiguration
import org.hibernate.SessionFactory
import org.hibernate.dialect.HSQLDialect
import org.hibernate.classic.Session

/**
* @author Graeme Rocher
* @since 1.0
*
* Created: Mar 12, 2008
*/
class HibernateBidirectionalListPersistTests extends GroovyTestCase{

    SessionFactory sessionFactory

    protected void setUp() {
        def bb = new BeanBuilder()

        bb.beans {
            dataSource(BasicDataSource) {
                url = "jdbc:hsqldb:mem:grailsDB"
                driverClassName = "org.hsqldb.jdbcDriver"
                username = "sa"
                password = ""
            }
            sessionFactory(ConfigurableLocalSessionFactoryBean) {
                dataSource = dataSource
                configLocation = "classpath:org/codehaus/groovy/grails/orm/hibernate/hibernate-bidirectional-list-mapping.xml"
                configClass = AnnotationConfiguration
                hibernateProperties = ["hibernate.hbm2ddl.auto":"create-drop"]
            }
        }

        def ctx = bb.createApplicationContext()

        sessionFactory = ctx.getBean("sessionFactory")
    }

    protected void tearDown() {
        sessionFactory = null
    }




    void testListPersisting() {
        assert sessionFactory

        def section = new FaqSection()

        section.title = "foo"
        def element = new FaqElement()
        element.question = "question 1"
        element.answer = "the answer"
        section.elements = [element]

        Session session = sessionFactory.openSession()

        session.save section

        session.flush()

        session.clear()

        section = session.get(FaqSection,1L)

        assert section
        assertEquals 1, section.elements.size()
    }
}