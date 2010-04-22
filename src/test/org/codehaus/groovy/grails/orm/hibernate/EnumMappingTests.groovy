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
class SuperClass {Long id; Long version;}
class SubClassWithStringProperty extends SuperClass {
    Long id
    Long version
    String someProperty
}
class SubClassWithEnumProperty extends SuperClass {
	VehicleStatus vehicalStatus
}
class SubClassWithOptionalEnumProperty extends SuperClass {
    VehicleStatus optionalVehicalStatus
    static constraints = {
        optionalVehicalStatus nullable: true
    }
}
''')
    }

    void testEnumNullabilityWithTablePerHierarchy() {
        def vehicleEnum = ga.classLoader.loadClass("VehicleStatus")
    	def domainClassWithStringProperty = ga.getDomainClass('SubClassWithStringProperty').clazz
    	def domainClassWithEnumProperty = ga.getDomainClass('SubClassWithEnumProperty').clazz
    	def domainClassWithOptionalEnumProperty = ga.getDomainClass('SubClassWithOptionalEnumProperty').clazz

    	def domainObject = domainClassWithStringProperty.newInstance()
    	domainObject.someProperty = 'data'
        assertNotNull domainObject.save()

        domainObject = domainClassWithEnumProperty.newInstance()
        assertNull domainObject.save()
        domainObject.vehicalStatus = vehicleEnum.IDLING
        assertNotNull domainObject.save()

        domainObject = domainClassWithOptionalEnumProperty.newInstance()
        assertNotNull domainObject.save()
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