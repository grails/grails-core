package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 21, 2009
 */

public class EnumCollectionMappingTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''

import grails.persistence.*

enum VehicleStatus { OFF, IDLING, ACCELERATING, DECELARATING }

@Entity
class Truck {

    static hasMany = [statuses:VehicleStatus,moreStatuses:VehicleStatus]

    static mapping = {
        statuses joinTable:[name:'VEHICLE_STATUS_LOG', key:'TRUCK_ID', column:'THE_STATUS']
    }
}

''')
    }


    void testCollectionOfEnumMapping() {

        def conn = session.connection()

        conn.prepareStatement("SELECT TRUCK_ID, THE_STATUS FROM VEHICLE_STATUS_LOG").executeQuery()

        def VehicleStatus = ga.classLoader.loadClass("VehicleStatus")
        def Truck = ga.getDomainClass("Truck").clazz
        def truck = Truck.newInstance()

        truck.addToStatuses(VehicleStatus.OFF)
        truck.addToStatuses(VehicleStatus.IDLING)

        truck.save(flush:true)

        session.clear()

        truck = Truck.get(1)

        assertNotNull truck

        assertEquals 2, truck.statuses.size()
        assertTrue truck.statuses.contains(VehicleStatus.OFF)
        assertTrue truck.statuses.contains(VehicleStatus.IDLING)
    }

    void testDefaultCollectionOfEnumMapping() {

        def conn = session.connection()

        conn.prepareStatement("SELECT truck_id, vehicle_status FROM truck_more_statuses").executeQuery()

        def VehicleStatus = ga.classLoader.loadClass("VehicleStatus")
        def Truck = ga.getDomainClass("Truck").clazz
        def truck = Truck.newInstance()

        truck.addToMoreStatuses(VehicleStatus.OFF)
        truck.addToMoreStatuses(VehicleStatus.IDLING)

        truck.save(flush:true)

        session.clear()

        truck = Truck.get(1)

        assertNotNull truck

        assertEquals 2, truck.moreStatuses.size()
        assertTrue truck.moreStatuses.contains(VehicleStatus.OFF)
        assertTrue truck.moreStatuses.contains(VehicleStatus.IDLING)
    }
}