package org.grails.web.mapping

import org.grails.web.util.WebUtils
import org.grails.web.mapping.DefaultUrlMappingsHolder

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class UrlMappingParameterTests extends AbstractGrailsMappingTests {

    def test1 = '''
class UrlMappings {
    static mappings = {

      "/$controller/$action?/$id?"{
          lang = "de"
          constraints {
             // apply constraints here
          }
      }
    }
}
'''

    def test2 = '''
class UrlMappings {
    static mappings = {
        "/news/$action?/$category" {
            controller = "blog"
            constraints {
                action(inList:['archive', 'latest'])
            }
        }
   }
}
'''

    def test3 = '''
class UrlMappings {
    static mappings = {
        "/showSomething/$key" {
            controller = "blog"
            constraints {
                key notEqual: 'bad'
            }
        }
   }
}
'''
    void testDontUseDispatchActionIfExceptionPresent() {
        Closure closure = new GroovyClassLoader().parseClass(test1).mappings
        def mappings = evaluator.evaluateMappings(closure)

        webRequest.currentRequest.addParameter("${WebUtils.DISPATCH_ACTION_PARAMETER}foo", "true")
        webRequest.currentRequest.setAttribute(WebUtils.EXCEPTION_ATTRIBUTE, new RuntimeException("bad"))
        def holder = new DefaultUrlMappingsHolder(mappings)
        def info = holder.match('/foo/list')

        assert info != null

        info.configure webRequest

        assert info.actionName == 'list'

    }
    void testUseDispatchAction() {
        Closure closure = new GroovyClassLoader().parseClass(test1).mappings
        def mappings = evaluator.evaluateMappings(closure)

        webRequest.currentRequest.addParameter("${WebUtils.DISPATCH_ACTION_PARAMETER}foo", "true")
        def holder = new DefaultUrlMappingsHolder(mappings)
        def info = holder.match('/foo/list')

        assert info != null

        info.configure webRequest

        assert info.actionName == 'foo'

        assertEquals "de", webRequest.params.lang
    }

    void testNotEqual() {
        Closure closure = new GroovyClassLoader().parseClass(test3).mappings
        def mappings = evaluator.evaluateMappings(closure)

        def holder = new DefaultUrlMappingsHolder(mappings)
        def info = holder.match('/showSomething/bad')

        assertNull 'url should not have matched', info

        info = holder.match('/showSomething/good')

        assertNotNull 'url should have matched', info
        info.configure webRequest
        assertEquals "good", webRequest.params.key
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
}
