package org.grails.web.mapping

import grails.testing.web.UrlMappingsUnitTest
import org.grails.web.util.WebUtils
import org.grails.web.mapping.DefaultUrlMappingsHolder
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class UrlMappingParameterTests extends Specification implements UrlMappingsUnitTest<UrlMappings> {

    void testDontUseDispatchActionIfExceptionPresent() {
        when:
        webRequest.params.controller = 'foo'
        webRequest.currentRequest.addParameter("${WebUtils.DISPATCH_ACTION_PARAMETER}foo", "true")
        webRequest.currentRequest.setAttribute(WebUtils.EXCEPTION_ATTRIBUTE, new RuntimeException("bad"))
        def info = urlMappingsHolder.match('/foo/list')

        assert info != null

        info.configure webRequest

        then:
        info.actionName == 'list'

    }
    void testUseDispatchAction() {
        when:
        webRequest.params.controller = 'foo'
        webRequest.currentRequest.addParameter("${WebUtils.DISPATCH_ACTION_PARAMETER}foo", "true")
        def info = urlMappingsHolder.match('/foo/list')
        assert info != null
        info.configure webRequest

        then:
        info.actionName == 'foo'
        "de" == webRequest.params.lang
    }

    void testNotEqual() {
        when:
        webRequest.params.controller = 'foo'
        def info = urlMappingsHolder.match('/showSomething/bad')

        then:'url should not have matched'
        info.controllerName == 'foo'

        when:
        info = urlMappingsHolder.match('/showSomething/good')

        then:'url should have matched'
         info.controllerName == 'blog'

        when:
        info.configure webRequest

        then:
        "good" == webRequest.params.key
    }

    void testDynamicMappingWithAdditionalParameter() {
        Closure closure = new GroovyClassLoader().parseClass(test1).mappings
        def mappings = evaluator.evaluateMappings(closure)

        def holder = new DefaultUrlMappingsHolder(mappings)
        def info = holder.match('/foo/list')

        info.configure webRequest

        assertEquals "de", webRequest.params.lang
    }

    void testDynamicMappingWithAdditionalParameterAndAppliedConstraints() {
        Closure closure = new GroovyClassLoader().parseClass(test2).mappings
        def mappings = evaluator.evaluateMappings(closure)
        def holder = new DefaultUrlMappingsHolder(mappings)
        def info = holder.match('/news/latest/sport')

        info.configure webRequest

        assertEquals "blog", info.controllerName
        assertEquals "latest", info.actionName
        assertEquals "sport", info.parameters.category

        def urlCreator = holder.getReverseMapping("blog", "latest", [category:"sport"])
        assertEquals "/news/latest/sport",urlCreator.createURL("blog", "latest", [category:"sport"], "utf-8")
    }

    static class UrlMappings {
        static mappings = {
            "/$controller/$action?/$id?"{
                lang = "de"
                constraints {
                    // apply constraints here
                }
            }
            "/news/$action?/$category" {
                controller = "blog"
                constraints {
                    action(inList:['archive', 'latest'])
                }
            }
            "/showSomething/$key" {
                controller = "blog"
                constraints {
                    key notEqual: 'bad'
                }
            }
        }
    }
}
