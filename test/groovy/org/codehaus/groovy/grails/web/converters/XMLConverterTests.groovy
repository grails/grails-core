/**
 * Tests for the XML converter
 
 * @author Graeme Rocher
 * @since 0.6
  *
 * Created: Aug 3, 2007
 * Time: 6:50:09 PM
 * 
 */

package org.codehaus.groovy.grails.web.converters

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

class XMLConverterTests extends AbstractGrailsControllerTests {

       void testXMLConverter() {
           def c = ga.getControllerClass("RestController").newInstance()

           c.test()

           assertEquals( '''<?xml version="1.0" encoding="ISO-8859-1"?><book>
  <author>Stephen King</author>
  <title>The Stand</title>
</book>''', response.contentAsString)
       }

    void onSetUp() {
        gcl.parseClass('''
import grails.converters.*
        
class RestController {
     def test = {
        def b = new Book(title:'The Stand', author:'Stephen King')
        render b as XML
     }
}
class Book {
    Long id
    Long version
    String title
    String author

}

        ''')


    }


}