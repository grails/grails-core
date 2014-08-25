package org.codehaus.groovy.grails.web.servlet.view;

import grails.util.GrailsWebUtil

import javax.servlet.http.HttpServletRequest

import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.View
import org.springframework.web.servlet.view.InternalResourceView

import spock.lang.Specification


class GroovyPageViewResolverSpec extends Specification {
    def "should use namespace as part of cache key"() {
        given:
        GroovyPageViewResolver resolver = new GroovyPageViewResolver() {
            protected View createGrailsView(String viewName) throws Exception {
                return new InternalResourceView(viewName);
            }
        }
        resolver.templateEngine = new GroovyPagesTemplateEngine()
        def pageLocator = Mock(GrailsConventionGroovyPageLocator)
        pageLocator.resolveViewFormat(_) >> { String viewName -> viewName }
        resolver.groovyPageLocator = pageLocator
        resolver.allowGrailsViewCaching = true
        def webRequest = GrailsWebUtil.bindMockWebRequest()
        webRequest.currentRequest.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAMESPACE_ATTRIBUTE, 'a')
        when:
        def view = resolver.resolveViewName('/hello/hello',null)
        def view2 = resolver.resolveViewName('/hello/hello',null)
        then:
        view.is(view2)
        when:
        webRequest.currentRequest.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAMESPACE_ATTRIBUTE, 'b')
        def view3 = resolver.resolveViewName('/hello/hello',null)
        then:
        !view2.is(view3)
        when:
        webRequest.currentRequest.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAMESPACE_ATTRIBUTE, 'a')
        def view4 = resolver.resolveViewName('/hello/hello',null)
        then:
        view2.is(view4)
    }
    
    def "should use pluginContextPath as part of cache key"() {
        given:
        GroovyPageViewResolver resolver = new GroovyPageViewResolver() {
            protected View createGrailsView(String viewName) throws Exception {
                return new InternalResourceView(viewName);
            }
        }
        resolver.templateEngine = new GroovyPagesTemplateEngine()
        def pageLocator = Mock(GrailsConventionGroovyPageLocator)
        pageLocator.resolveViewFormat(_) >> { String viewName -> viewName }
        resolver.groovyPageLocator = pageLocator
        resolver.allowGrailsViewCaching = true
        def webRequest = GrailsWebUtil.bindMockWebRequest()
        def grailsAppAttributes = Mock(GrailsApplicationAttributes)
        webRequest.attributes = grailsAppAttributes
        grailsAppAttributes.getPluginContextPath(_ as HttpServletRequest) >> { HttpServletRequest request -> webRequest.currentRequest.getAttribute('currentPlugin') }
        
        webRequest.currentRequest.setAttribute('currentPlugin', 'a')
        when:
        def view = resolver.resolveViewName('/hello/hello',null)
        def view2 = resolver.resolveViewName('/hello/hello',null)
        then:
        view.is(view2)
        when:
        webRequest.currentRequest.setAttribute('currentPlugin', 'b')
        def view3 = resolver.resolveViewName('/hello/hello',null)
        then:
        !view2.is(view3)
        when:
        webRequest.currentRequest.setAttribute('currentPlugin', 'a')
        def view4 = resolver.resolveViewName('/hello/hello',null)
        then:
        view2.is(view4)
    }
    
    def "should use pluginContextPath and namespace as part of cache key"() {
        given:
        GroovyPageViewResolver resolver = new GroovyPageViewResolver() {
            protected View createGrailsView(String viewName) throws Exception {
                return new InternalResourceView(viewName);
            }
        }
        resolver.templateEngine = new GroovyPagesTemplateEngine()
        def pageLocator = Mock(GrailsConventionGroovyPageLocator)
        pageLocator.resolveViewFormat(_) >> { String viewName -> viewName }
        resolver.groovyPageLocator = pageLocator
        resolver.allowGrailsViewCaching = true
        def webRequest = GrailsWebUtil.bindMockWebRequest()
        def grailsAppAttributes = Mock(GrailsApplicationAttributes)
        webRequest.attributes = grailsAppAttributes
        grailsAppAttributes.getPluginContextPath(_ as HttpServletRequest) >> { HttpServletRequest request -> webRequest.currentRequest.getAttribute('currentPlugin') }
        
        webRequest.currentRequest.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAMESPACE_ATTRIBUTE, 'a')
        webRequest.currentRequest.setAttribute('currentPlugin', 'a')
        when:
        def view = resolver.resolveViewName('/hello/hello',null)
        def view2 = resolver.resolveViewName('/hello/hello',null)
        then:
        view.is(view2)
        when:
        webRequest.currentRequest.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAMESPACE_ATTRIBUTE, 'b')
        def view3a = resolver.resolveViewName('/hello/hello',null)
        then:
        !view2.is(view3a)
        when:
        webRequest.currentRequest.setAttribute('currentPlugin', 'b')
        def view3b = resolver.resolveViewName('/hello/hello',null)
        then:
        !view2.is(view3b)
        when:
        webRequest.currentRequest.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAMESPACE_ATTRIBUTE, 'a')
        webRequest.currentRequest.setAttribute('currentPlugin', 'a')
        def view4 = resolver.resolveViewName('/hello/hello',null)
        then:
        view2.is(view4)
    }

    
    def cleanup() {
        RequestContextHolder.resetRequestAttributes()
    }
}

