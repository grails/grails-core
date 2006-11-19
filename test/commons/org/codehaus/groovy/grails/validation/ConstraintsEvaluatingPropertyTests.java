package org.codehaus.groovy.grails.validation;

import groovy.lang.GroovyClassLoader;

import java.util.Map;
import java.util.Collection;

import junit.framework.TestCase;

import org.codehaus.groovy.grails.validation.metaclass.ConstraintsEvaluatingDynamicProperty;
import org.codehaus.groovy.grails.orm.hibernate.validation.ConstrainedPersistentProperty;
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass;

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
                "   static def constraints = {\n" +
                "      name( nullable: false, validator : { 'called' } )\n" +
                "   }" +
                "}";
        ensureConstraintsPresent(classSource, 2); // Must have nullable and validator
    }

    private void ensureConstraintsPresent(String classSource, int constraintCount)
            throws Exception
    {
        // We need to do a real test here to make sure
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class dc = gcl.parseClass(classSource);

        DefaultGrailsDomainClass domainClass = new DefaultGrailsDomainClass(dc);

        Map constraints = domainClass.getConstrainedProperties();

        ConstrainedPersistentProperty p = (ConstrainedPersistentProperty)constraints.get("name");
        Collection cons = p.getAppliedConstraints();
                       
        assertTrue( "Incorrect number of constraints extracted, found " + cons.size() + ": "+constraints, cons.size() == constraintCount);
    }

    /**
     * Test that non-static constraints work
     */
    public void testNonStaticConstraints() throws Exception {
        String classSource = "package org.codehaus.groovy.grails.validation\n" +
                "class Test {\n" +
                "   Long id\n"+  // WE NEED this even though GORM 2 doesn't, as we're not a "domain" class within grails-app
                "   Long version\n"+ // WE NEED this even though GORM 2 doesn't, as we're not a "domain" class within grails-app
                "   String name\n" +
                "   def constraints = {\n" +
                "      name( nullable: false, validator : { 'called' } )\n" +
                "   }" +
                "}";
        ensureConstraintsPresent(classSource, 2); // Must have nullable and validator
    }
}
