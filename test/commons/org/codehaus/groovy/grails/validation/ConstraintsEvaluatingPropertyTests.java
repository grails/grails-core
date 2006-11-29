package org.codehaus.groovy.grails.validation;

import groovy.lang.GroovyClassLoader;
import junit.framework.TestCase;
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass;
import org.codehaus.groovy.grails.orm.hibernate.validation.ConstrainedPersistentProperty;
import org.codehaus.groovy.grails.validation.metaclass.ConstraintsEvaluatingDynamicProperty;

import java.util.Collection;
import java.util.Map;

public class ConstraintsEvaluatingPropertyTests extends TestCase {

    /*
      * Test method for 'org.codehaus.groovy.grails.validation.metaclass.ConstraintsDynamicProperty.get(Object)'
      */
    public void testGet() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class groovyClass = gcl.parseClass("package org.codehaus.groovy.grails.validation\n" +
                "class Test {\n" +
                " String name\n" +
                "}");

        ConstraintsEvaluatingDynamicProperty cp = new ConstraintsEvaluatingDynamicProperty();

        Map constraints = (Map)cp.get(groovyClass.newInstance());

        assertNotNull(constraints);
        assertFalse(constraints.isEmpty());
    }


    /**
     * Test that static constraints work
     */
    public void testStaticConstraints() throws Exception {
        String classSource = "package org.codehaus.groovy.grails.validation\n" +
                "class Test {\n" +
                "   Long id\n"+  // WE NEED this even though GORM 2 doesn't, as we're not a "domain" class within grails-app
                "   Long version\n"+ // WE NEED this even though GORM 2 doesn't, as we're not a "domain" class within grails-app
                "   String name\n" +
                "   static constraints = {\n" +
                "      name( nullable: false, validator : { 'called' } )\n" +
                "   }" +
                "}";
        ensureConstraintsPresent(new String[] { classSource }, 0, 2); // Must have nullable and validator
    }

    /**
     * Test that static constraints work
     */
    public void testInheritedConstraints() throws Exception {
        String classSource = "package org.codehaus.groovy.grails.validation\n" +
                "class Test {\n" +
                "   Long id\n"+  // WE NEED this even though GORM 2 doesn't, as we're not a "domain" class within grails-app
                "   Long version\n"+ // WE NEED this even though GORM 2 doesn't, as we're not a "domain" class within grails-app
                "   String name\n" +
                "   static constraints = {\n" +
                "      name( nullable: false, validator : { 'called' } )\n" +
                "   }" +
                "}";
        String descendentSource = "package org.codehaus.groovy.grails.validation\n" +
                "class TestB extends Test {\n" +
                "   static constraints = {\n" +
                "      name( length:5..20)\n" +
                "   }" +
                "}";
        ensureConstraintsPresent(new String[] { classSource, descendentSource}, 1, 3); // Must have nullable and validator
    }

    private void ensureConstraintsPresent(String[] classSource, int classIndexToTest, int constraintCount)
            throws Exception
    {
        // We need to do a real test here to make sure
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class[] classes = new Class[classSource.length];
        for (int i = 0; i < classSource.length; i++) {
            classes[i] = gcl.parseClass(classSource[i]);
        }

        DefaultGrailsDomainClass domainClass = new DefaultGrailsDomainClass(classes[classIndexToTest]);

        Map constraints = domainClass.getConstrainedProperties();

        ConstrainedPersistentProperty p = (ConstrainedPersistentProperty)constraints.get("name");
        Collection cons = p.getAppliedConstraints();
                       
        assertEquals( "Incorrect number of constraints extracted: " +constraints, constraintCount, cons.size());
    }

}
