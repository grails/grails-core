package org.codehaus.groovy.grails.plugins.i18n;

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*
import grails.util.Metadata

class I18nGrailsPluginTests extends AbstractGrailsMockTests {

    void testI18nPlugin() {

        ga.@applicationMeta = ['grails.war.deployed':'true', (Metadata.APPLICATION_NAME): getClass().name ] as Metadata
        ctx.registerMockResource("WEB-INF/grails-app/i18n/messages.properties")
        ctx.registerMockResource("WEB-INF/grails-app/i18n/messages-site_en.properties")
        ctx.registerMockResource("WEB-INF/grails-app/i18n/foo-site_en.properties")
        ctx.registerMockResource("WEB-INF/grails-app/i18n/project.properties")
        ctx.registerMockResource("WEB-INF/grails-app/i18n/project_nl.properties")
        ctx.registerMockResource("WEB-INF/grails-app/i18n/sub/dir_name/sub/foo-bar_en.properties")
        ctx.registerMockResource("WEB-INF/grails-app/i18n/nobundle")
        ctx.registerMockResource("WEB-INF/grails-app/i18n/nobundle.txt")
        ctx.registerMockResource("WEB-INF/grails-app/i18n/nobundle.xml")

        def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")

        def plugin = new DefaultGrailsPlugin(pluginClass, ga)

        def springConfig = new WebRuntimeSpringConfiguration(ctx)
        springConfig.servletContext = createMockServletContext()

        plugin.doWithRuntimeConfiguration(springConfig)

        def appCtx = springConfig.getApplicationContext()

        assert appCtx.containsBean("messageSource")
        assert appCtx.containsBean("localeChangeInterceptor")
        assert appCtx.containsBean("localeResolver")

        // nasty way of asserting/inspecting the basenames set in the messageSource
        // this is needed because messageSource has no API method for retrieving the basenames
        def messageSource = appCtx.getBean("messageSource")
        def field = messageSource.class.superclass.getDeclaredField("fallbackToSystemLocale")
        field.accessible=true
        assert !field.get(messageSource)

        def messageSourceString = messageSource?.toString()
        assert messageSourceString.contains( "messages")
        assert messageSourceString.contains( "messages-site")
        assert messageSourceString.contains( "foo-site")
        assert messageSourceString.contains( "foo-bar")
        assert messageSourceString.contains( "project")
        assert !messageSourceString.contains( "messages.properties")
        assert !messageSourceString.contains( "project.properties")
        assert !messageSourceString.contains( "project_nl.properties")
        //assers(messageSource, "n.contains(bundle")
        assert !messageSourceString.contains( "nobundle.txt")
        assert !messageSourceString.contains( "nobundle.xml")
    }
}
