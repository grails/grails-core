package org.codehaus.groovy.grails.orm.hibernate
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
''')
    }

    void testEnumMapping() {
        def vehicleClass = ga.getDomainClass("Vehicle").clazz
        def vehicleEnum = ga.classLoader.loadClass("VehicleStatus")
        def vehicle = vehicleClass.newInstance()

        vehicle.status = vehicleEnum.IDLING

        vehicle.save(flush:true)

        session.clear()

        vehicle = vehicleClass.get(1)

        assertEquals vehicleEnum.IDLING, vehicle.status

    }
}