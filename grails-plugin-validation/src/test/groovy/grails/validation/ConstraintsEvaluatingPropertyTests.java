package grails.validation;

import grails.core.DefaultGrailsApplication;
import grails.core.GrailsApplication;
import grails.core.GrailsDomainClass;
import grails.gorm.validation.*;
import grails.gorm.validation.Constrained;
import grails.util.Holders;
import groovy.lang.GroovyClassLoader;

import java.util.Collection;
import java.util.Map;

import junit.framework.TestCase;

import org.grails.core.artefact.DomainClassArtefactHandler;
import org.grails.plugins.MockGrailsPluginManager;

public class ConstraintsEvaluatingPropertyTests extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockGrailsPluginManager pluginManager = new MockGrailsPluginManager();
        Holders.setPluginManager(pluginManager);
        pluginManager.registerMockPlugin(MappingContextBuilder.FAKE_HIBERNATE_PLUGIN);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        Holders.setPluginManager(null);
    }

    /*
     * Test method for 'ConstraintsDynamicProperty.get(Object)'
     */
    @SuppressWarnings("rawtypes")
    public void testGet() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class<?> groovyClass = gcl.parseClass("package org.grails.validation\n" +
                "class Test {\n" +
                "   Long id\n"+  // WE NEED this even though GORM 2 doesn't, as we're not a "domain" class within grails-app
                "   Long version\n"+ // WE NEED this even though GORM 2 doesn't, as we're not a "domain" class within grails-app
                " String name\n" +
                "}");

        GrailsApplication ga = new DefaultGrailsApplication(groovyClass);
        ga.initialise();
        new MappingContextBuilder(ga).build(groovyClass);
        GrailsDomainClass domainClass = (GrailsDomainClass)ga.getArtefact(DomainClassArtefactHandler.TYPE, groovyClass.getName());

        Map constraints = domainClass.getConstrainedProperties();

        assertNotNull(constraints);
        assertFalse(constraints.isEmpty());
    }

    /**
     * Test that static constraints work
     */
    public void testStaticConstraints() throws Exception {
        String classSource = "package org.grails.validation\n" +
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
        GroovyClassLoader gcl = new GroovyClassLoader();
        gcl.parseClass(
                "package org.grails.validation\n" +
                "@grails.persistence.Entity class Book {\n" +
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
                "@grails.persistence.Entity class Author {\n" +
                "   Long id\n" +
                "   Long version\n" +
                "   String name\n" +
                "}\n" +
                "@grails.persistence.Entity class Chapter {\n" +
                "   Long id\n" +
                "   Long version\n" +
                "   String text\n" +
                "}");

        GrailsApplication ga = new DefaultGrailsApplication(gcl.getLoadedClasses());
        ga.initialise();
        new MappingContextBuilder(ga).build(gcl.getLoadedClasses());

        GrailsDomainClass bookClass = (GrailsDomainClass)ga.getArtefact(DomainClassArtefactHandler.TYPE, "org.grails.validation.Book");

        Map constraints = bookClass.getConstrainedProperties();
        Constrained p = (Constrained)constraints.get("title");
        assertFalse("Title property should be required", p.isNullable());
        p = (Constrained)constraints.get("description");
        assertTrue("Description property should be optional", p.isNullable());
        p = (Constrained)constraints.get("author");
        assertFalse("Author property should be required", p.isNullable());
        p = (Constrained)constraints.get("assistent");
        assertTrue("Assistent property should be optional", p.isNullable());
        // Test that Collections and Maps are nullable by default
        p = (Constrained)constraints.get("chapters");
        assertTrue("Chapters property should be optional", p.isNullable());
        p = (Constrained)constraints.get("remarks");
        assertTrue("Remarks property should be optional", p.isNullable());
    }

    /**
     * Test that static constraints work
     */
    public void testInheritedConstraints() throws Exception {
        String classSource = "package org.grails.validation\n" +
                "class Test {\n" +
                "   Long id\n"+  // WE NEED this even though GORM 2 doesn't, as we're not a "domain" class within grails-app
                "   Long version\n"+ // WE NEED this even though GORM 2 doesn't, as we're not a "domain" class within grails-app
                "   String name\n" +
                "   static constraints = {\n" +
                "      name(nullable: false, validator : { 'called' })\n" +
                "   }" +
                "}";
        String descendentSource = "package org.grails.validation\n" +
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

        GrailsApplication ga = new DefaultGrailsApplication(classes[classIndexToTest]);
        ga.initialise();
        new MappingContextBuilder(ga).build(classes[classIndexToTest]);
        GrailsDomainClass domainClass = (GrailsDomainClass)ga.getArtefact(DomainClassArtefactHandler.TYPE, classes[classIndexToTest].getName());

        Map constraints = domainClass.getConstrainedProperties();

        grails.gorm.validation.ConstrainedProperty p = (grails.gorm.validation.ConstrainedProperty)constraints.get("name");
        Collection cons = p.getAppliedConstraints();

        assertEquals("Incorrect number of constraints extracted: " +constraints, constraintCount, cons.size());
    }

}
