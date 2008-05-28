package org.codehaus.groovy.grails.web.mapping

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.springframework.core.io.ByteArrayResource

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: May 28, 2008
 */
class UrlMappingParameterTests extends AbstractGrailsControllerTests{


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

    void testDynamicMappingWithAdditionalParameter() {

        Closure closure = new GroovyClassLoader().parseClass(test1).mappings
        def evaluator = new DefaultUrlMappingEvaluator()
        def mappings = evaluator.evaluateMappings(closure)

        def holder = new DefaultUrlMappingsHolder(mappings)

        def info = holder.match('/foo/list')

        info.configure webRequest

        assertEquals "de", webRequest.params.lang

    }

    void testDynamicMappingWithAdditionalParameterAndAppliedConstraints() {

        Closure closure = new GroovyClassLoader().parseClass(test2).mappings
        def evaluator = new DefaultUrlMappingEvaluator()
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