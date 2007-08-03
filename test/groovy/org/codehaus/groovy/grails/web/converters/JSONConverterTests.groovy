/**
 * Tests for the JSON converter.
 
 * @author Graeme Rocher
 * @since 0.4
  *
 * Created: Aug 3, 2007
 * Time: 9:10:19 PM
 * 
 */

package org.codehaus.groovy.grails.web.converters

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

class JSONConverterTests extends AbstractGrailsControllerTests {

       void testXMLConverter() {
           def c = ga.getControllerClass("RestController").newInstance()

           c.test()

           assertEquals( '''{"id":null,"class":"Book","title":"The Stand","author":"Stephen King"}''', response.contentAsString)
       }

    void onSetUp() {
        gcl.parseClass('''
import grails.converters.*

class RestController {
     def test = {
        def b = new Book(title:'The Stand', author:'Stephen King')
        render b as JSON
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