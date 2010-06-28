package org.codehaus.groovy.grails.web.taglib

class GroovyEachTagTests extends GroovyTestCase {

    void testEachWithSafeDereference() {
        def sw = new StringWriter()

        def tag = new GroovyEachTag()
        tag.init(out: new PrintWriter(sw))

        shouldFail {
            tag.doStartTag()
        }

        tag.setAttributes('"in"': 'test?')

        tag.doStartTag()

        assertEquals("test?.each { "+System.getProperty("line.separator"), sw.toString())
    }

    void testSimpleEach() {
        def sw = new StringWriter()
        def tag = new GroovyEachTag()
        tag.init(out: new PrintWriter(sw))

        shouldFail {
            tag.doStartTag()
        }

        tag.setAttributes('"in"': 'test')

        tag.doStartTag()

        assertEquals("test.each { "+ System.getProperty("line.separator"),sw.toString())
    }

    void testEachWithVar() {
        def sw = new StringWriter()

        def tag = new GroovyEachTag()
        tag.init(out: new PrintWriter(sw))
        tag.setAttributes('"in"': 'test', '"var"':"i")

        tag.doStartTag()

        assertEquals("test.each { i ->"+ System.getProperty("line.separator"),sw.toString())
    }

    void testEachWithStatusOnly() {
        def sw = new StringWriter()

        def tag = new GroovyEachTag()
        tag.init(out: new PrintWriter(sw))
        tag.setAttributes('"in"': 'test', '"status"':"i")
        shouldFail {
            tag.doStartTag()
        }
    }

    void testEachWithStatusAndVar() {
        def sw = new StringWriter()

        def tag = new GroovyEachTag()
        tag.init(out: new PrintWriter(sw))
        tag.setAttributes('"in"': 'test', '"status"':"i",'"var"':"i")

        shouldFail {
            tag.doStartTag()
        }
        tag.setAttributes('"var"':'j')
        tag.doStartTag()

        assert sw.toString() == "test.eachWithIndex { j,i ->"+System.getProperty("line.separator")
    }
}
