package grails.util
/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class BuildScopeTests extends GroovyTestCase{

    protected void tearDown() {
        System.setProperty(BuildScope.KEY, "")
    }

    void testEnableScope() {
        assertEquals BuildScope.ALL, BuildScope.getCurrent()

        BuildScope.WAR.enable()

        assertEquals BuildScope.WAR, BuildScope.getCurrent()
    }


    void testIsValid() {


        assertTrue BuildScope.isValid("war")
        assertTrue BuildScope.isValid("test")
        assertTrue BuildScope.isValid("war", "test")

        BuildScope.WAR.enable()

        assertTrue BuildScope.isValid("war")
        assertTrue BuildScope.isValid("war", "test")
        assertFalse BuildScope.isValid("test")

    }

}