package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.jdbc.ConnectionWrapper
import org.springframework.jdbc.datasource.ConnectionProxy

class DataSourceTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.Entity
@Entity
class Flanglurb {}
'''
    }

    void testDataBindingErrors() {
        def dc = ga.getDomainClass('Flanglurb').clazz
        dc.withTransaction { s ->

            def dataSource = appCtx.dataSource
            def dataSourceUnproxied = appCtx.dataSourceUnproxied

             // the session's connection will be a Hibernate wrapper around the Spring wrapper
            def sessionFactoryConnection = sessionFactory.currentSession.connection()

            assertTrue sessionFactoryConnection instanceof ConnectionWrapper
            sessionFactoryConnection = sessionFactoryConnection.getWrappedConnection()

            assertTrue sessionFactoryConnection instanceof ConnectionProxy
            sessionFactoryConnection = sessionFactoryConnection.getTargetConnection()

             // a connection from the datasource proxy will be a Spring wrapper around Hibernate's wrapper
            def dataSourceConnection = dataSource.connection

            assertTrue dataSourceConnection instanceof ConnectionProxy
            dataSourceConnection = dataSourceConnection.getTargetConnection()

            assertTrue dataSourceConnection instanceof ConnectionWrapper
            dataSourceConnection = dataSourceConnection.getWrappedConnection()

            assertTrue dataSourceConnection instanceof ConnectionProxy
            dataSourceConnection = dataSourceConnection.getTargetConnection()

            def unproxiedConnection = dataSourceUnproxied.connection
            assertFalse unproxiedConnection instanceof ConnectionProxy
            assertFalse unproxiedConnection instanceof ConnectionWrapper

            assertTrue sessionFactoryConnection.is(dataSourceConnection)
            assertFalse unproxiedConnection.is(dataSourceConnection)
        }
    }
}
