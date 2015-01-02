package org.grails.web.pages

import grails.artefact.Artefact
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@TestFor(NullValueTagLib)
class TagLibWithNullValuesTests extends Specification {

    void testNullValueHandling() {
        expect:
        applyTemplate('<p>This is tag1: <my:tag1 p1="abc"/></p>') == '<p>This is tag1: org.grails.taglib.encoder.OutputEncodingStack$OutputProxyWriter: [abc] []</p>'
        applyTemplate('<p>This is tag2: <my:tag2/></p>') == '<p>This is tag2: org.grails.taglib.encoder.OutputEncodingStack$OutputProxyWriter: [abc] []</p>'
    }
}

@Artefact('TagLib')
class NullValueTagLib {
    static namespace = 'my'

    Closure tag1 = { attrs ->
        out << out.getClass().name << ": [" << attrs.p1 << "] [" << attrs.p2 << "]"
    }

    Closure tag2 = { attrs ->
        out << my.tag1(p1: "abc")
    }
}
  
