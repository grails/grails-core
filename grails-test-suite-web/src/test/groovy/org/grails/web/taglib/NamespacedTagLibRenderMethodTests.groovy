package org.grails.web.taglib

import org.grails.core.io.MockStringResourceLoader
import org.grails.core.artefact.TagLibArtefactHandler

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class NamespacedTagLibRenderMethodTests extends AbstractGrailsTagTests {

    protected void onInit() {
        def tagClass = gcl.parseClass('''
import grails.gsp.*

@TagLib
class WithNamespaceTagLib {

    static namespace = "ns1"

    Closure tag1 = { attrs, body ->
        out << render(template: "/bug1/t1n")
    }
    Closure tag2 = { attrs, body ->
        out << render(template: "/bug1/t2n")
    }

}
''')
       def tagClass2 = gcl.parseClass('''
import grails.gsp.*

@TagLib
class NormalTagLib {

    Closure tag1 = { attrs, body ->
        out << render(template: "/bug1/t1")
    }
    Closure tag2 = { attrs, body ->
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
