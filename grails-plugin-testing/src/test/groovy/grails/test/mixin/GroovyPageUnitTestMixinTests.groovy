package grails.test.mixin

import grails.test.mixin.web.GroovyPageUnitTestMixin

/**
 * @author Graeme Rocher
 */
@TestMixin(GroovyPageUnitTestMixin)
class GroovyPageUnitTestMixinTests extends GroovyTestCase {

    void testRenderTemplate() {
        views['/bar/_foo.gsp'] = 'Hello <g:createLink controller="foo" />'

        def result = render(template:"/bar/foo")

        assert result == 'Hello /foo'

    }

    void testRenderView() {
       views['/foo/bar.gsp'] = 'Hello <g:createLink controller="bar" />'

        def result = render(view:"/foo/bar")

        assert result == 'Hello /bar'
    }

    void testMockTagLibrary() {
        mockTagLib(FooTagLib)

        def result = applyTemplate('<foo:bar one="${one}"/>', [one:'good'])

        assert result != null
        assert result == 'tag contents good'
    }
}

class FooTagLib {
    static namespace = "foo"

    def bar = {attrs ->
        out << "tag contents ${attrs.one}"
    }
}
