/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Sep 20, 2007
 */
package org.codehaus.groovy.grails.web.mapping

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.springframework.core.io.ByteArrayResource

class IdUrlMappingTests extends AbstractGrailsControllerTests{

    def mappingScript = '''
mappings {
		"/emailConfirmation/$id?" {
            controller = "emailConfirmation"
            action = "index"
		}
		"/$id?" {
		    controller = "content"
		    action = "index"
        }
}        
'''

    void onSetUp() {
        gcl.parseClass('''
class EmailConfirmationController {
	def index = {
		[result: "ID = " + params.id]
 	}
}
class ContentController {
	def index = {}
}
        ''')
    }
	void testIdInURL() {
           def res = new ByteArrayResource(mappingScript.bytes)

           def evaluator = new DefaultUrlMappingEvaluator()
           def mappings = evaluator.evaluateMappings(res)

           def holder = new DefaultUrlMappingsHolder(mappings)
           assert webRequest

           def infos = holder.matchAll("/emailConfirmation/foo")
           assert infos

           infos[0].configure(webRequest)

           def c = ga.getControllerClass("EmailConfirmationController").newInstance()

           assertEquals "foo",c.params.id
    }

    void testIdInParam() {

           def res = new ByteArrayResource(mappingScript.bytes)

           def evaluator = new DefaultUrlMappingEvaluator()
           def mappings = evaluator.evaluateMappings(res)

           def holder = new DefaultUrlMappingsHolder(mappings)
           assert webRequest

           request.addParameter("id", "foo")
            def infos = holder.matchAll("/emailConfirmation")
            assert infos

            infos[0].configure(webRequest)


           def c = ga.getControllerClass("EmailConfirmationController").newInstance()

           assertEquals "foo",c.params.id
    }

     void testMappingWithUrlEncodedCharsInId() {
          def res = new ByteArrayResource(mappingScript.bytes)

           def evaluator = new DefaultUrlMappingEvaluator()
           def mappings = evaluator.evaluateMappings(res)

           def holder = new DefaultUrlMappingsHolder(mappings)
           assert webRequest

           def infos = holder.matchAll("/emailConfirmation/my%20foo")
           assert infos

           infos[0].configure(webRequest)

           def c = ga.getControllerClass("EmailConfirmationController").newInstance()

           assertEquals "my foo",c.params.id

           infos = holder.matchAll("/emailConfirmation/my%2Ffoo")
           assert infos

           infos[0].configure(webRequest)

           c = ga.getControllerClass("EmailConfirmationController").newInstance()

           assertEquals "my/foo",c.params.id
    }
}