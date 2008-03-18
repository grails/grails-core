package org.codehaus.groovy.grails.orm.hibernate

import javax.sql.DataSource

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Mar 18, 2008
 */
class TablePerSubclassIdentityMappingTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass('''
class StationEvent {
  Long id
  Long version
  String station

 static mapping = {
    id column: 'STATION_EVENT_ID'
    tablePerSubclass true
 }
}

class PlateEvent extends StationEvent {
    String plate
    static mapping = {
        id column: "STATION_EVENT_ID"
        version false
    }
}
''')
    }


    void testMappedIdentityForSubclass() {
        DataSource ds = applicationContext.getBean('dataSource')

         def con
         try {
             con = ds.getConnection()
             def statement = con.prepareStatement("select STATION_EVENT_ID from plate_event")
             statement.execute()
             statement = con.prepareStatement("select STATION_EVENT_ID from station_event")
             statement.execute()

         } finally {
             con.close()
         }
    }
}