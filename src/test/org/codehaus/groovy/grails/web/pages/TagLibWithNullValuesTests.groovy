package org.codehaus.groovy.grails.web.pages

import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Apr 23, 2009
 */

public class TagLibWithNullValuesTests extends AbstractGrailsTagTests{

    public void onSetUp() {
        gcl.parseClass('''
class MyTagLib {
  static namespace = 'my'

  def tag1 = { attrs ->
    out << out.getClass().name << ": [" << attrs.p1 << "] [" << attrs.p2 << "]"
  }

  def tag2 = { attrs ->
    out << my.tag1(p1: "abc")
  }
}
''')
    }


    void testNullValueHandling() {
        def template = '<p>This is tag1: <my:tag1 p1="abc"/></p>'

        assertOutputEquals '<p>This is tag1: org.codehaus.groovy.grails.web.pages.GSPResponseWriter: [abc] []</p>', template

        template = '<p>This is tag2: <my:tag2/></p>'

        assertOutputEquals '<p>This is tag2: org.codehaus.groovy.grails.web.taglib.GroovyPageTagWriter: [abc] []</p>', template
    }

}