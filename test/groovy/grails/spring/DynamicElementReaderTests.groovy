package grails.spring

import org.w3c.dom.Element

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 15, 2009
 */

public class DynamicElementReaderTests extends GroovyTestCase{

    void testReadMethodToElement() {

        def elementReader = new DynamicElementReader("jee", [jee:"http://www.springframework.org/schema/jee"])

        try {
            elementReader.'jndi-lookup'(id:"dataSource", 'jndi-name':"jdbc/petstore")
        }
        catch (e) {
            assertEquals """Configuration problem: No namespace handler found for element <jee:jndi-lookup id='dataSource' jndi-name='jdbc/petstore' xmlns:jee='http://www.springframework.org/schema/jee'/>
Offending resource: resource loaded from byte array""", e.message            
        }

    }

}