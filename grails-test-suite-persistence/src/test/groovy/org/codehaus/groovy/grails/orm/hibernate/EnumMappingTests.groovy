package org.codehaus.groovy.grails.orm.hibernate

import javax.sql.DataSource

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class EnumMappingTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import org.codehaus.groovy.grails.orm.hibernate.TestEnum
import org.codehaus.groovy.grails.orm.hibernate.TestEnumUserType

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

class TestEnumUser {
    Long id
    Long version
    TestEnum usesCustom
    TestEnum doesnt
    static mapping = {
        usesCustom type: TestEnumUserType
    }
}
'''
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
        def ps = con.prepareStatement("select * from vehicle")
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
        def ps = con.prepareStatement("select * from vehicle2")
        def rs = ps.executeQuery()
        rs.next()
        assertEquals 1,rs.getInt("status")

        con.close()
    }

    void testCustomTypeEnumMapping() {
        def TestEnumUser = ga.getDomainClass("TestEnumUser").clazz
        def instance = TestEnumUser.newInstance()

        instance.usesCustom = TestEnum.Flurb
        instance.doesnt = TestEnum.Skrabdle
        instance.save(flush:true)
        session.clear()

        instance = TestEnumUser.get(instance.id)
        assertEquals TestEnum.Flurb, instance.usesCustom
        assertEquals TestEnum.Skrabdle, instance.doesnt

        DataSource ds = applicationContext.dataSource
        def con = ds.getConnection()
        def ps = con.prepareStatement('select * from test_enum_user')
        def rs = ps.executeQuery()
        rs.next()

        assertEquals 4200, rs.getInt('uses_custom')
        assertEquals 'Skrabdle', rs.getString('doesnt')

        rs.close()
        ps.close()
        con.close()
    }
}
