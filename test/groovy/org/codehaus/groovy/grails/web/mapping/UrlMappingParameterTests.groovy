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


    def topLevelMapping = '''
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
    void testDynamicMappingWithAdditionalParameter() {

        Closure closure = new GroovyClassLoader().parseClass(topLevelMapping).mappings
        def evaluator = new DefaultUrlMappingEvaluator()
        def mappings = evaluator.evaluateMappings(closure)

        def holder = new DefaultUrlMappingsHolder(mappings)

        def info = holder.match('/foo/list')

        info.configure webRequest

        assertEquals "de", webRequest.params.lang

    }

}