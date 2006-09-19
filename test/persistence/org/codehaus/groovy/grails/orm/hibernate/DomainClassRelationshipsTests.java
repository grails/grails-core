package org.codehaus.groovy.grails.orm.hibernate;

import groovy.lang.GroovyObject;

import java.util.Set;

/**
 * Tests that check the relationship management capability of Grails domain classes
 *
 * @author Graeme Rocher
 */
public class DomainClassRelationshipsTests extends AbstractGrailsHibernateTests {


    public void testBidirectional() throws Exception {

        GroovyObject author = (GroovyObject)ga.getGrailsDomainClass("Author").newInstance();

        GroovyObject book1 = (GroovyObject)ga.getGrailsDomainClass("Book").newInstance();

        Object result = author.invokeMethod("addBook", new Object[]{book1});

        assertEquals(author,result);
        assertEquals(author,book1.getProperty("author"));

        Set books = (Set)author.getProperty("books");
        assertNotNull(books);
        assertEquals(1,books.size());
        assertTrue(books.contains(book1));
    }

    public void testUnidirectional() throws Exception {
        GroovyObject car = (GroovyObject)ga.getGrailsDomainClass("Car").newInstance();
        GroovyObject owner = (GroovyObject)ga.getGrailsDomainClass("Owner").newInstance();

        Object result = owner.invokeMethod("addCar", new Object[]{car});

        assertEquals(owner,result);

        Set cars = (Set)owner.getProperty("cars");
        assertNotNull(cars);
        assertEquals(1,cars.size());
        assertTrue(cars.contains(car));
    }

    protected void onSetUp() throws Exception {
        gcl.parseClass("class Book {\n" +
                                    "\tLong id\n" +
                                    "\tLong version\n" +
                                    "\n" +
                                    "\tAuthor author\n" +
                                    "}\n" +
                                    "class Author {\n" +
                                    "\tLong id\n" +
                                    "\tLong version\n" +
                                    "\tdef relatesToMany = [books:Book]\n" +
                                    "\tSet books\n" +
                                    "}");
        Thread.sleep(500);
        gcl.parseClass("class Car {\n" +
                "\tLong id\n" +
                "\tLong version\n" +
                "}\n" +
                "class Owner {\n" +
                "\tLong id\n" +
                "\tLong version\n" +
                "\tdef relatesToMany = [cars:Car]\n" +
                "\tSet cars\n" +
                "}");

    }

    protected void onTearDown() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
