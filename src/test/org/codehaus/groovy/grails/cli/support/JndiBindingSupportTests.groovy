package org.codehaus.groovy.grails.cli.support

import javax.naming.Context
import javax.naming.Name
import javax.naming.spi.ObjectFactory

/**
 * @author Graeme Rocher
 * @since 1.2.3
 */
class JndiBindingSupportTests extends GroovyTestCase {

    void testJndiBindingSupport() {
        def bindingSupport = new JndiBindingSupport(
            ["bean/MyBean": new MyBean(bar:"34"),
                "jdbc/EmployeeDB": [
                    type: "javax.sql.DataSource", //required
                    auth: "Container", // optional
                    description: "Data source for Foo", //optional
                    driverClassName: "org.hsql.jdbcDriver",
                    url: "jdbc:HypersonicSQL:database",
                    username: "dbusername",
                    password: "dbpassword",
                    maxActive: "8",
                    maxIdle: "4"],
                "bean/MyBean2": [
                    auth: "Container",
                    type: "org.codehaus.groovy.grails.cli.support.MyBean",
                    bar: "23"
                ],
            ])

        def context = bindingSupport.bind()
        assertNotNull "context should not be null", context

        def ds = context.lookup("jdbc/EmployeeDB")
        assertNotNull "should have a data source",ds
        assertEquals "dbusername", ds.username

        def b1 = context.lookup("bean/MyBean")
        assertNotNull "bean should not be null", b1
        assertEquals "34", b1.bar

        def b2 = context.lookup("bean/MyBean2")
        assertNotNull "bean should not be null", b2
        assertEquals "23", b2.bar
    }
}

class MyBean {
    String bar
}
