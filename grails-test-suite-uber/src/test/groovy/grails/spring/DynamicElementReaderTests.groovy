package grails.spring

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class DynamicElementReaderTests {

    @Test
    void testReadMethodToElement() {

        def elementReader = new DynamicElementReader("jee", [jee:"http://www.springframework.org/schema/jee"])

        try {
            elementReader.'jndi-lookup'(id:"dataSource", 'jndi-name':"jdbc/petstore")
        }
        catch (e) {
            assertEquals """Configuration problem: No namespace handler found for element <jee:jndi-lookup id='dataSource' jndi-name='jdbc/petstore' xmlns:jee='http://www.springframework.org/schema/jee'/>
Offending resource: Byte array resource [resource loaded from byte array]""", e.message
        }
    }
}
