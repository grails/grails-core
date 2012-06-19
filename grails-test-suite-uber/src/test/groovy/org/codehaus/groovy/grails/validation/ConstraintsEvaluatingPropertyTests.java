package org.codehaus.groovy.grails.validation;

import groovy.lang.GroovyClassLoader;

import java.util.Collection;
import java.util.Map;

import junit.framework.TestCase;

import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.plugins.MockGrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerHolder;
import org.codehaus.groovy.grails.test.support.MockHibernatePluginHelper;

public class ConstraintsEvaluatingPropertyTests extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockGrailsPluginManager pluginManager = new MockGrailsPluginManager();
        PluginManagerHolder.setPluginManager(pluginManager);
        pluginManager.registerMockPlugin(MockHibernatePluginHelper.FAKE_HIBERNATE_PLUGIN);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        PluginManagerHolder.setPluginManager(null);
    }

    /*
     * Test method for 'org.codehaus.groovy.grails.validation.metaclass.ConstraintsDynamicProperty.get(Object)'
     */
    @SuppressWarnings("rawtypes")
    public void testGet() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class<?> groovyClass = gcl.parseClass("package org.codehaus.groovy.grails.validation\n" +
                "class Test {\n" +
                "   Long id\n"+  // WE NEED this even though GORM 2 doesn't, as we're not a "domain" class within grails-app
                "   Long version\n"+ // WE NEED this even though GORM 2 doesn't, as we're not a "domain" class within grails-app
                " String name\n" +
                "}");

        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(groovyClass);

        Map constraints = domainClass.getConstrainedProperties();

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
                "      name(nullable: false, validator : { 'called' })\n" +
                "   }" +
                "}";
        ensureConstraintsPresent(new String[] { classSource }, 0, 2); // Must have nullable and validator
    }

    /**
     * Test that static constraints work
     */
    @SuppressWarnings("rawtypes")
    public void testNullableConstraint() throws Exception {
        String bookClassSource =
                "package org.codehaus.groovy.grails.validation\n" +
                "class Book {\n" +
                "   Long id\n" +
                "   Long version\n" +
                "   String title\n" +
                "   String description\n" +
                "   Author author\n" +
                "   Author assistent\n" +
                "   Set chapters\n" +
                "   Map remarks\n" +
                "   static hasMany = [chapters:Chapter]\n" +
                "   static constraints = {\n" +
                "      description(nullable: true)\n" +
                "      assistent(nullable: true)\n" +
                "   }\n" +
                "}\n" +
                "class Author {\n" +
                "   Long id\n" +
                "   Long version\n" +
                "   String name\n" +
                "}\n" +
                "class Chapter {\n" +
                "   Long id\n" +
                "   Long version\n" +
                "   String text\n" +
                "}";

        GroovyClassLoader gcl = new GroovyClassLoader();

        DefaultGrailsDomainClass bookClass = new DefaultGrailsDomainClass(gcl.parseClass(bookClassSource, "Book"));

        Map constraints = bookClass.getConstrainedProperties();
        ConstrainedProperty p = (ConstrainedProperty)constraints.get("title");
        assertFalse("Title property should be required", p.isNullable());
        p = (ConstrainedProperty)constraints.get("description");
        assertTrue("Description property should be optional", p.isNullable());
        p = (ConstrainedProperty)constraints.get("author");
        assertFalse("Author property should be required", p.isNullable());
        p = (ConstrainedProperty)constraints.get("assistent");
        assertTrue("Assistent property should be optional", p.isNullable());
        // Test that Collections and Maps are nullable by default
        p = (ConstrainedProperty)constraints.get("chapters");
        assertTrue("Chapters property should be optional", p.isNullable());
        p = (ConstrainedProperty)constraints.get("remarks");
        assertTrue("Remarks property should be optional", p.isNullable());
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
                "      name(nullable: false, validator : { 'called' })\n" +
                "   }" +
                "}";
        String descendentSource = "package org.codehaus.groovy.grails.validation\n" +
                "class TestB extends Test {\n" +
                "   static constraints = {\n" +
                "      name(size:5..20)\n" +
                "   }" +
                "}";
        ensureConstraintsPresent(new String[] { classSource, descendentSource}, 1, 3); // Must have nullable and validator
    }

    @SuppressWarnings("rawtypes")
    private void ensureConstraintsPresent(String[] classSource, int classIndexToTest, int constraintCount)
            throws Exception {
        // We need to do a real test here to make sure
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class[] classes = new Class[classSource.length];
        for (int i = 0; i < classSource.length; i++) {
            classes[i] = gcl.parseClass(classSource[i]);
        }

        DefaultGrailsDomainClass domainClass = new DefaultGrailsDomainClass(classes[classIndexToTest]);

        Map constraints = domainClass.getConstrainedProperties();

        ConstrainedProperty p = (ConstrainedProperty)constraints.get("name");
        Collection cons = p.getAppliedConstraints();

        assertEquals("Incorrect number of constraints extracted: " +constraints, constraintCount, cons.size());
    }

}
