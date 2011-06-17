package grails.test.mixin

import grails.artefact.Artefact
import grails.test.mixin.web.GroovyPageUnitTestMixin

/**
 * @author Graeme Rocher
 */
@TestMixin(GroovyPageUnitTestMixin)
class GroovyPageUnitTestMixinTests extends GroovyTestCase {

    void testAssertOutputEquals() {
        mockTagLib(FooTagLib)
        assertOutputEquals 'tag contents good', '<foo:bar one="${one}"/>', [one:'good']
        
        shouldFail(AssertionError) {
            assertOutputEquals 'tag contents bad', '<foo:bar one="${one}"/>', [one:'good']
        }
    }
    
    void testAssertOutputMatches() {
        mockTagLib(FooTagLib)
        assertOutputMatches (/.*good.*/, '<foo:bar one="${one}"/>', [one:'good'])
        
        shouldFail(AssertionError) {
            assertOutputMatches (/.*bad.*/, '<foo:bar one="${one}"/>', [one:'good'])
        }
    }
    
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

@Artefact("TagLibrary")
class FooTagLib {
    static namespace = "foo"

    def bar = { attrs ->
        out << "tag contents ${attrs.one}"
    }
}
