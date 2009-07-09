package org.codehaus.groovy.grails.orm.hibernate

import javax.sql.DataSource

/**
* @author Graeme Rocher
* @since 1.0
*
* Created: May 28, 2008
*/
class EnumMappingTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
enum VehicleStatus { OFF, IDLING, ACCELERATING, DECELARATING }

class Vehicle {
    Long id
    Long version
    VehicleStatus status
}
class Vehicle2 {
    Long id
    Long version
    VehicleStatus status
    static mapping = {
        status enumType:'ordinal'
    }
}
''')
    }

    void testDefaultEnumMapping() {
        def vehicleClass = ga.getDomainClass("Vehicle").clazz
        def vehicleEnum = ga.classLoader.loadClass("VehicleStatus")
        def vehicle = vehicleClass.newInstance()

        vehicle.status = vehicleEnum.IDLING

        vehicle.save(flush:true)

        session.clear()

        vehicle = vehicleClass.get(1)

        assertEquals vehicleEnum.IDLING, vehicle.status


        DataSource ds = applicationContext.getBean("dataSource")

        def con = ds.getConnection()

        def ps= con.prepareStatement("select * from vehicle")
        def rs = ps.executeQuery()
        rs.next()
        assertEquals vehicleEnum.IDLING.toString(),rs.getString("status")
        
        con.close()
    }

    void testOrdinalEnumMapping() {
       def vehicleClass = ga.getDomainClass("Vehicle2").clazz
        def vehicleEnum = ga.classLoader.loadClass("VehicleStatus")
        def vehicle = vehicleClass.newInstance()

        vehicle.status = vehicleEnum.IDLING

        vehicle.save(flush:true)

        session.clear()

        vehicle = vehicleClass.get(1)

        assertEquals vehicleEnum.IDLING, vehicle.status


        DataSource ds = applicationContext.getBean("dataSource")

        def con = ds.getConnection()

        def ps= con.prepareStatement("select * from vehicle2")
        def rs = ps.executeQuery()
        rs.next()
        assertEquals 1,rs.getInt("status")

        con.close()
    }
}