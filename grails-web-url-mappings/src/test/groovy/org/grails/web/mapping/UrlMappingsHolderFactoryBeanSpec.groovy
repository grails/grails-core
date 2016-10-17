package org.grails.web.mapping

import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import grails.plugins.DefaultGrailsPluginManager
import grails.plugins.GrailsPluginManager
import grails.web.mapping.UrlMappingsHolder
import org.springframework.context.support.GenericApplicationContext
import spock.lang.Specification

/**
 * Created by graemerocher on 17/10/16.
 */
class UrlMappingsHolderFactoryBeanSpec extends Specification {

    void "test url mappings holder factory bean excludes"() {
        given:
        UrlMappingsHolderFactoryBean factoryBean = new UrlMappingsHolderFactoryBean()
        def context = new GenericApplicationContext()
        context.refresh()
        def app = new DefaultGrailsApplication(ExcludeUrlMappings)
        def pm = new DefaultGrailsPluginManager(app)
        context.beanFactory.registerSingleton(GrailsApplication.APPLICATION_ID, app)
        context.beanFactory.registerSingleton(GrailsPluginManager.BEAN_NAME, pm)
        app.initialise()
        factoryBean.setApplicationContext(context)

        when:"The URL mappings holder is created"
        factoryBean.afterPropertiesSet()
        UrlMappingsHolder holder = factoryBean.getObject()

        then:"The excludes are correct"
        holder.excludePatterns == ["/stomp/", "/stomp/*", "/topic/*"]
        holder.matchAll("/stomp/foo").size() == 0
    }
}
class ExcludeUrlMappings {

    static excludes = ["/stomp/", "/stomp/*", "/topic/*"]

    static mappings = {
        '/**'(controller:"index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
