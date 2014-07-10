package org.grails.web.taglib

class ReturnValueTagLibTests extends AbstractGrailsTagTests {

    void onSetUp() {
        gcl.parseClass('''
import grails.gsp.*

@TagLib
class ReturnValueTagLib {
        static returnObjectForTags = ['numberretval']

    Closure numberretval = { attrs ->
        // out shouldn't be used in returnObjectForTags tags, but we don't prevent it
        out << 'this output should be discarded in function call.'
        return 123
    }

    Closure outputnotused = { attrs ->
        return 'dontshowup'
    }

    Closure discardreturnvalue = { attrs ->
        out << 'hello'
        return 'dontshowup'
    }
}
''')
    }

    void testReturnValue() {
        def template = '${g.numberretval()}'
        assertOutputEquals '123', template

        template = '<g:numberretval />'
        assertOutputEquals 'this output should be discarded in function call.123', template

        template = '${(g.numberretval()==123 && g.numberretval() instanceof Integer)}<g:numberretval />'
        assertOutputEquals 'truethis output should be discarded in function call.123', template

        template = '${(numberretval()==123 && numberretval() instanceof Integer)}<g:numberretval />'
        assertOutputEquals 'truethis output should be discarded in function call.123', template
    }

    void testOutputNotUsed() {
        def template = '${g.outputnotused()}<g:outputnotused />'
        assertOutputEquals '', template

        template = '${outputnotused()}<g:outputnotused />'
        assertOutputEquals '', template
    }

    void testDiscardReturnValue() {
        def template = '${g.discardreturnvalue()}<g:discardreturnvalue />'
        assertOutputEquals 'hellohello', template

        template = '${discardreturnvalue()}<g:discardreturnvalue />'
        assertOutputEquals 'hellohello', template
    }
}
