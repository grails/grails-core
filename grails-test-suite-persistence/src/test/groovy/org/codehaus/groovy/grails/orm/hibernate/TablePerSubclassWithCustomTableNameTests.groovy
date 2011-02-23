package org.codehaus.groovy.grails.orm.hibernate

import javax.sql.DataSource

/**
 * @author grocher
 */
class TablePerSubclassWithCustomTableNameTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class Animal {
      Long id
      Long version
      String name
      static mapping = {
        tablePerSubclass true
        table "myAnimals"
      }
}

class Dog extends Animal {
      String bark
      static mapping = {
        table "myDogs"
      }
}
class Cat extends Animal {
      String meow
      static mapping = {
        table "myCats"
      }
}
'''
    }

    void testGeneratedTables() {
        DataSource ds = applicationContext.getBean('dataSource')

        def con
        try {
            con = ds.getConnection()
            def statement = con.prepareStatement("select * from myDogs")
            statement.execute()
            statement = con.prepareStatement("select * from myCats")
            statement.execute()
        } finally {
            con.close()
        }
    }
}
