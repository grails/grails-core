package grails.util
/**
 * @author Graeme Rocher
 * @since 1.1
 * 
 * Created: Dec 12, 2008
 */

public class EnvironmentTests extends GroovyTestCase{

    protected void tearDown() {
        System.setProperty(Environment.KEY, "")
    }



    void testGetCurrent() {
        System.setProperty("grails.env", "prod")

        assertEquals Environment.PRODUCTION, Environment.getCurrent()

        System.setProperty("grails.env", "dev")

        assertEquals Environment.DEVELOPMENT, Environment.getCurrent()
        
    }

    void testGetEnvironment() {

        assertEquals Environment.DEVELOPMENT, Environment.getEnvironment("dev")
        assertEquals Environment.TEST, Environment.getEnvironment("test")
        assertEquals Environment.PRODUCTION, Environment.getEnvironment("prod")
        assertNull Environment.getEnvironment("doesntexist")
    }

    void testSystemPropertyOverridesMetadata() {
        Metadata.getInstance(new ByteArrayInputStream('''
grails.env=production
'''.bytes))

        assertEquals Environment.PRODUCTION, Environment.getCurrent()

        System.setProperty("grails.env", "dev")

        assertEquals Environment.DEVELOPMENT, Environment.getCurrent()

        System.setProperty("grails.env", "")

        assertEquals Environment.PRODUCTION, Environment.getCurrent()

        Metadata.getInstance(new ByteArrayInputStream(''.bytes))

        assertEquals Environment.DEVELOPMENT, Environment.getCurrent()

    }

}