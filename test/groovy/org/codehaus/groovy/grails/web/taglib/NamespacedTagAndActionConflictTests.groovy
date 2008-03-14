package org.codehaus.groovy.grails.web.taglib
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Mar 14, 2008
 */
class NamespacedTagAndActionConflictTests extends AbstractGrailsTagTests {

    public void onSetUp() {
        gcl.parseClass '''
class FeedsTagLib {
    static namespace = "feed"
    def rss = {
        out << "rss feed"
    }
}
class TestController  {
    def feed = {
        "foo"
    }
    def test = {
        // should favour local action of feed tag
        assert feed instanceof Closure
        render feed()
    }
}
'''
    }


    void testTagLibNamespaceAndActionConflict() {
        def controllerClass = ga.getControllerClass("TestController").clazz

        def controller = controllerClass.newInstance()

        controller.test()

        assertEquals "foo", response.contentAsString
        
    }


}