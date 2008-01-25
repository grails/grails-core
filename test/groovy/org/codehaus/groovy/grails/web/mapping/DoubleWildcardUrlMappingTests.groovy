/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Oct 10, 2007
 */
package org.codehaus.groovy.grails.web.mapping

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.springframework.core.io.ByteArrayResource

class DoubleWildcardUrlMappingTests extends AbstractGrailsControllerTests {

    def mappingScript = '''
mappings {
    "/components/image/$path**?" {
        controller = "components"
        action = "image"
    }
    "/stuff/image/$path**" {
        controller = "components"
        action = "image"
    }

    "/cow/$controller/$action?/$id?/$path**?"()
}
    '''

    def mappingScript2 = '''
mappings  {
    "/$controller/$action?/$id?"()

    "/images/$image**.jpg" (controller: 'userImage', action: 'download')
}
'''

    public void onSetUp() {
        gcl.parseClass '''
class DoubleWildCardController {
    def index = { params.path }
}
'''
    }


    void testDoubleWildcardInParam() {

           def res = new ByteArrayResource(mappingScript.bytes)

           def evaluator = new DefaultUrlMappingEvaluator()
           def mappings = evaluator.evaluateMappings(res)

           def holder = new DefaultUrlMappingsHolder(mappings)
           assert webRequest

           request.addParameter("d", "1")
            def infos = holder.matchAll("/cow/wiki/show/2/doc/?d=1")
            assert infos

            infos[0].configure(webRequest)


           def c = ga.getControllerClass("DoubleWildCardController").newInstance()

           assertEquals "doc/",c.params.path
           assertEquals "1",c.params.d
    }
    
    void testDoubleWildCardMappingWithSuffix() {
        def res = new ByteArrayResource(mappingScript2.bytes)

        def evaluator = new DefaultUrlMappingEvaluator()
        def mappings = evaluator.evaluateMappings(res)


        def m = mappings[1]

        assert m

        def info = m.match("/images/foo.jpg")
        //assert !mappings[1].match("/stuff/image")
        assert info
        info.configure(webRequest)

        assertEquals "userImage", info.controllerName
        assertEquals "download", info.actionName
        assertEquals "foo", info.params.image

        info = m.match("/images/foo/bar.jpg")
        //assert !mappings[1].match("/stuff/image")
        assert info
        info.configure(webRequest)

        assertEquals "userImage", info.controllerName
        assertEquals "download", info.actionName
        assertEquals "foo/bar", info.params.image

    }

    void testDoubleWildCardMatching() {

             def res = new ByteArrayResource(mappingScript.bytes)

             def evaluator = new DefaultUrlMappingEvaluator()
             def mappings = evaluator.evaluateMappings(res)


             def m = mappings[0]
             def m2 = mappings[1]
             assert m

             def info = m.match("/components/image")
             //assert !mappings[1].match("/stuff/image")

             info.configure(webRequest)

             assertEquals "components", info.controllerName
             assertEquals "image", info.actionName
             assertNull webRequest.params.path

             info = m.match("/components/image/")
             info.configure(webRequest)

             assertEquals "components", info.controllerName
             assertEquals "image", info.actionName
             assertEquals '', webRequest.params.path

             info = m.match("/components/image/foo.bar")
             assert info
             info.configure(webRequest)

             assertEquals "components", info.controllerName
             assertEquals "image", info.actionName
             assertEquals 'foo.bar', webRequest.params.path

             info = m.match('/components/image/asdf/foo.bar')
             assert info
             info.configure(webRequest)

             assertEquals "components", info.controllerName
             assertEquals "image", info.actionName
             assertEquals 'asdf/foo.bar', webRequest.params.path

             assert !m2.match("/stuff/image")
             info = m2.match("/stuff/image/foo.bar")
             assert info
             info.configure(webRequest)

             assertEquals "components", info.controllerName
             assertEquals "image", info.actionName
             assertEquals 'foo.bar', webRequest.params.path

    }


}