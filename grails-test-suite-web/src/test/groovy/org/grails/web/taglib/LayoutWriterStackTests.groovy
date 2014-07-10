package org.grails.web.taglib

import grails.artefact.Artefact
import grails.test.mixin.TestFor
import org.junit.Test

@TestFor(TwoColumnTagLib)
class LayoutWriterStackTests {
    def template = """
    <g:twoColumn>
        <g:left>leftContent</g:left>
        <g:right>rightContent</g:right>
        bodyContent
    </g:twoColumn>"""

    @Test void testLayoutTag() {
        String result = applyTemplate(template)
        assertEqualsIgnoreWhiteSpace("""
        <div class='twoColumn'>
            left: <div class='left'>leftContent</div>,
            right: <div class='right'>rightContent</div>,
            body: bodyContent
        </div>""",
                result)
    }

    @Test void testNestedLayoutTags() {
        def nested = template.replaceAll("leftContent", template)
        String result = applyTemplate(nested)

        assertEqualsIgnoreWhiteSpace("""
        <div class='twoColumn'>
            left: <div class='left'>
                <div class='twoColumn'>
                    left: <div class='left'>leftContent</div>,
                    right: <div class='right'>rightContent</div>,
                    body: bodyContent
                </div>
            </div>,
            right: <div class='right'>rightContent</div>,
            body: bodyContent</div>""",
                result)
    }

    void assertEqualsIgnoreWhiteSpace(String s1, String s2) {
        assert s1.replaceAll(/\s/, '') == s2.replaceAll(/\s/, '')
    }
}

@Artefact("TagLib")
class TwoColumnTagLib {

    Closure twoColumn = {attrs, body ->
        def parts = LayoutWriterStack.writeParts(body)
        out << "<div class='twoColumn'>left: " << parts.left << ", right: " << parts.right << ", body: " << parts.body << "</div>"
    }
    Closure left = {attrs, body ->
        def w = LayoutWriterStack.currentWriter('left')
        w << "<div class='left'>" << body() << "</div>"
    }

    Closure right = {attrs, body ->
        def w = LayoutWriterStack.currentWriter('right')
        w << "<div class='right'>" << body() << "</div>"
    }
}
