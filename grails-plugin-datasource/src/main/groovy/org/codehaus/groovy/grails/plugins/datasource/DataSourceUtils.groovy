package org.codehaus.groovy.grails.plugins.datasource

import groovy.transform.CompileStatic

import java.sql.Driver
import java.sql.DriverManager

/**
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class DataSourceUtils {

    static List<String> clearJdbcDriverRegistrations() {
        List<String> driverNames = []

        /*
         * DriverManager.getDrivers() has a nasty side-effect of registering
         * drivers that are visible to this class loader but haven't yet been
         * loaded. Therefore, the first call to this method a) gets the list
         * of originally loaded drivers and b) triggers the unwanted
         * side-effect. The second call gets the complete list of drivers
         * ensuring that both original drivers and any loaded as a result of the
         * side-effects are all de-registered.
         */
        def originalDrivers = new HashSet<Driver>()
        Enumeration<Driver> drivers = DriverManager.getDrivers()
        while (drivers.hasMoreElements()) {
            originalDrivers << drivers.nextElement()
        }
        drivers = DriverManager.getDrivers()
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement()
            // Only unload the drivers this web app loaded
            if (driver.getClass().classLoader !=
                DataSourceUtils.classLoader) {
                continue
            }
            // Only report drivers that were originally registered. Skip any
            // that were registered as a side-effect of this code.
            if (originalDrivers.contains(driver)) {
                driverNames.add(driver.getClass().canonicalName)
            }
            DriverManager.deregisterDriver(driver)
        }
        return driverNames
    }
}
