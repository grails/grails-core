package org.codehaus.groovy.grails.commons;

import groovy.lang.GroovyClassLoader;
import junit.framework.TestCase;

/**
 * @author Graeme Rocher
 */
public class DefaultGrailsApplicationTests extends TestCase {

    public void testDataSourceEnvironments() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class devDS = gcl.parseClass("class DevelopmentDataSource {" +
                                    "String url = \"jdbc:hsqldb:mem:testDB\"\n" +
                                    "String driverClassName = \"org.hsqldb.jdbcDriver\"\n" +
                                    "String username = \"sa\"\n" +
                                    "String password = \"\"" +
                                    "}");
        Thread.sleep(1000);
        Class prodDS = gcl.parseClass("class ProductionDataSource {" +
                                    "String url = \"jdbc:hsqldb:mem:prodDB\"\n" +
                                    "String driverClassName = \"org.hsqldb.jdbcDriver\"\n" +
                                    "String username = \"sa\"\n" +
                                    "String password = \"\"" +
                                    "}");

        GrailsApplication ga = new DefaultGrailsApplication(new Class[]{devDS,prodDS}, gcl);

        assertEquals(devDS,ga.getGrailsDataSource().getClazz());

        System.setProperty(GrailsApplication.ENVIRONMENT,"production");

        assertEquals(prodDS,ga.getGrailsDataSource().getClazz());

        System.setProperty(GrailsApplication.ENVIRONMENT,"rubbish");

        try {
            GrailsDataSource ds = ga.getGrailsDataSource();
            assertNull(ds);
        }
        finally {
           System.setProperty(GrailsApplication.ENVIRONMENT,"");
        }

    }
}
