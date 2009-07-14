/**
 * 
 */
package org.codehaus.groovy.grails.web.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;

import javax.servlet.ServletContextEvent;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

/**
 * Extends the Spring default ContextLoader to load GrailsApplicationContext
 * 
 * @author Graeme Rocher
 *
 */
public class GrailsContextLoaderListener extends ContextLoaderListener {

    private static final Log LOG = LogFactory.getLog(GrailsContextLoaderListener.class);

	protected ContextLoader createContextLoader() {
		return new GrailsContextLoader();
	}

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        deregisterJDBCDrivers();
        super.contextDestroyed(event);
    }

    private void deregisterJDBCDrivers() {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
            } catch (SQLException e) {
                LOG.error("Error deregisetring JDBC driver ["+driver+"]: " + e.getMessage(), e);
            }
        }
    }
}
