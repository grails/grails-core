/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Dec 6, 2007
 */
package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.support.MockStringResourceLoader
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler

class NamespacedTagLibRenderMethodTests extends AbstractGrailsTagTests {

    void onInit() {
        def tagClass = gcl.parseClass( '''
class WithNamespaceTagLib {

    static namespace = "ns1"

    def tag1 = { attrs, body ->
        out << render(template: "/bug1/t1n")
    }
    def tag2 = { attrs, body ->
        out << render(template: "/bug1/t2n")
    }

}
''')
       def tagClass2 = gcl.parseClass('''
class NormalTagLib {

    def tag1 = { attrs, body ->
        out << render(template: "/bug1/t1")
    }
    def tag2 = { attrs, body ->
        out << render(template: "/bug1/t2")
    }

}
       ''')

        grailsApplication.addArtefact(TagLibArtefactHandler.TYPE,tagClass)
        grailsApplication.addArtefact(TagLibArtefactHandler.TYPE,tagClass2)
    }


    void testInvokeNamespacedTagLib() {

        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource("/bug1/_t1n.gsp", '''START TAG1|${ns1.tag2()}|STOP TAG1''')
        resourceLoader.registerMockResource("/bug1/_t2n.gsp", 'START TAG2|STOP TAG2')
        appCtx.groovyPagesTemplateEngine.resourceLoader = resourceLoader
        webRequest.controllerName = "foo"

        def template = '''<pre>START|${ns1.tag1()}|STOP</pre>'''

        assertOutputEquals('<pre>START|START TAG1|START TAG2|STOP TAG2|STOP TAG1|STOP</pre>', template)
    }

    void testInvokeNormalTagLib() {

        def resourceLoader = new MockStringResourceLoader()
        resourceLoader.registerMockResource("/bug1/_t1.gsp", 'START TAG1|${tag2()}|STOP TAG1')
        resourceLoader.registerMockResource("/bug1/_t2.gsp", 'START TAG2|STOP TAG2')
        appCtx.groovyPagesTemplateEngine.resourceLoader = resourceLoader
        webRequest.controllerName = "foo"

        def template = '''<pre>START|${tag1()}|STOP</pre>'''

        assertOutputEquals('<pre>START|START TAG1|START TAG2|STOP TAG2|STOP TAG1|STOP</pre>', template)
    }
}