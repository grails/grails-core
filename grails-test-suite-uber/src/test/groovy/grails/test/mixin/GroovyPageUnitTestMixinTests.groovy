package grails.test.mixin

import grails.artefact.Artefact
import grails.testing.web.GrailsWebUnitTest
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
@Ignore('grails-gsp is not on jakarta.servlet yet')
class GroovyPageUnitTestMixinTests extends Specification implements GrailsWebUnitTest {

    void setupSpec() {
        mockTagLib(FooTagLib)
    }

    void testAssertOutputEquals() {
        expect:
        'tag contents good' == applyTemplate('<foo:bar one="${one}"/>', [one:'good'])
    }

    void testAssertOutputMatches() {
        expect:
        applyTemplate('<foo:bar one="${one}"/>', [one:'good']) =~ /.*good.*/
    }

    void testRenderTemplate() {
        given:
        views['/bar/_foo.gsp'] = 'Hello <g:createLink controller="foo" />'

        when:
        def result = render(template:"/bar/foo")

        then:
        result == 'Hello /foo'
    }

    void testRenderView() {
        given:
        views['/foo/bar.gsp'] = 'Hello <g:createLink controller="bar" />'

        when:
        def result = render(view:"/foo/bar")

        then:
        result == 'Hello /bar'
    }
    
    @Issue("GRAILS-10723")
    void testCreateLinkWithoutController() {
        given:
        views['/foo/bar.gsp'] = 'Hello <g:createLink action="bar" />'

        when:
        def result = render(view:"/foo/bar")

        then:
        result == 'Hello /test/bar'
    }

    @Issue("GRAILS-10723")
    void testCreateLinkWithSpecifiedController() {
        given:
        webRequest.controllerName = 'foo'
        views['/foo/bar.gsp'] = 'Hello <g:createLink action="bar" />'

        when:
        def result = render(view:"/foo/bar")

        then:
        result == 'Hello /foo/bar'
    }

    void testThatViewsAreClearedBetweenTests() {
        when:
        def result = render(view:"/foo/bar")

        then:
        result == null
    }

    void testCanCallRenderMultipleTimesInOneTest() {
        given:
        views['/_h1.gsp'] = '<h1>${text}</h1>'
        views['/_h2.gsp'] = '<h2>${text}</h2>'

        expect:
        applyTemplate('<g:render template="/h1" model="[text: text]"/>', [text: 'A main heading']) == '<h1>A main heading</h1>'
        applyTemplate('<g:render template="/h2" model="[text: text]"/>', [text: 'A sub-heading']) == '<h2>A sub-heading</h2>'
    }

    void testMockTagLibrary() {
        when:
        def result = applyTemplate('<foo:bar one="${one}"/>', [one:'good'])

        then:
        result != null
        result == 'tag contents good'
    }
}

@Artefact("TagLibrary")
class FooTagLib {
    static namespace = "foo"

    def bar = { attrs ->
        out << "tag contents ${attrs.one}"
    }
}
