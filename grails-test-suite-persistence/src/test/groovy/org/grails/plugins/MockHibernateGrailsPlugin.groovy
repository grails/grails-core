package org.grails.plugins

import grails.util.GrailsUtil

import org.apache.tomcat.jdbc.pool.DataSource
import org.grails.core.artefact.AnnotationDomainClassArtefactHandler
import org.springframework.jdbc.datasource.DataSourceTransactionManager

class MockHibernateGrailsPlugin {

    def version = GrailsUtil.grailsVersion
    def dependsOn = [dataSource: version, i18n: version, core: version, domainClass: version]

    def artefacts = [new AnnotationDomainClassArtefactHandler()]
    def loadAfter = ['controllers']
    def doWithSpring = {
        dataSource(DataSource) {
            driverClassName = 'org.h2.Driver'
            url = 'jdbc:h2:mem:grailsDB;LOCK_TIMEOUT=10000'
            username = 'sa'
            password = ''
        }

        transactionManager(DataSourceTransactionManager, ref('dataSource'))
    }
}
