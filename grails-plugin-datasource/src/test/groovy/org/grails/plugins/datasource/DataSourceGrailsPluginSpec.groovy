package org.grails.plugins.datasource

import grails.core.GrailsApplication
import grails.plugins.GrailsPluginManager
import grails.spring.BeanBuilder
import groovy.sql.Sql
import org.grails.config.PropertySourcesConfig
import org.grails.datastore.mapping.core.DatastoreUtils
import org.springframework.context.ApplicationContext
import spock.lang.Specification

import javax.sql.DataSource

/**
 * Created by graemerocher on 19/01/2017.
 */
class DataSourceGrailsPluginSpec extends Specification {

    void "test data sources Grails plugin Spring configuration"() {
        when:
        DataSourceGrailsPlugin plugin = new DataSourceGrailsPlugin()
        plugin.setPluginManager(Mock(GrailsPluginManager))
        GrailsApplication application = Mock(GrailsApplication)
        application.getConfig() >> new PropertySourcesConfig('dataSource.pooled':true,'dataSource.url':'jdbc:h2:mem:devDb;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE')

        plugin.setGrailsApplication(application)

        BeanBuilder beanBuilder = new BeanBuilder()
        beanBuilder.beans plugin.doWithSpring()

        ApplicationContext ctx = beanBuilder.createApplicationContext()

        then:
        ctx.containsBean('dataSource')
        ctx.getBean('dataSource', DataSource)


        when:"A query is executed"
        DataSource ds = ctx.getBean('dataSource', DataSource)
        Sql sql = new Sql(ds)
        int result = sql.call('CREATE TABLE `user` (username VARCHAR(50), password VARCHAR(50)); select * from `user`')

        then:
        result == 0


    }
 }
