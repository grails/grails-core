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
        System.setProperty(Environment.RELOAD_ENABLED, "")
        System.setProperty(Environment.RELOAD_LOCATION, "")

        Metadata.getCurrent().clear()
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

    void testReloadEnabled() {
        Metadata.getInstance(new ByteArrayInputStream('''
grails.env=production
'''.bytes))

        
        assertFalse "reload should be disabled by default in production", Environment.getCurrent().isReloadEnabled()


        System.setProperty("grails.env", "dev")

        assertFalse "reload should be disabled by default in development unless base.dir set", Environment.getCurrent().isReloadEnabled()

        System.setProperty("base.dir", ".")

        assertTrue "reload should be enabled by default in development if base.dir set", Environment.getCurrent().isReloadEnabled()

        System.setProperty("base.dir", "")

        System.setProperty("grails.env", "prod")

        assertFalse "reload should be disabled by default in production if base.dir set", Environment.getCurrent().isReloadEnabled()

        System.setProperty(Environment.RELOAD_ENABLED, "true")

        assertFalse "reload should be disabled by default in production if reload enabled set but not location", Environment.getCurrent().isReloadEnabled()

        System.setProperty(Environment.RELOAD_LOCATION, ".")

        assertTrue "reload should be enabled by default in production if reload enabled and location set", Environment.getCurrent().isReloadEnabled()


    }

}