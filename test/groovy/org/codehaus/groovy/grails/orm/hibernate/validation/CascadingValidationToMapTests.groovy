package org.codehaus.groovy.grails.orm.hibernate.validation

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests

/**
* @author Graeme Rocher
* @since 1.0
*
* Created: Jan 17, 2008
*/
class CascadingValidationToMapTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass '''
class Vehicle {
    Long id
    Long version
	String name
	Map doors = new HashMap()
	static hasMany = [doors:Door]
}
class Door {
    Long id
    Long version
	String make
	static belongsTo = [Vehicle]
}
'''
    }


	void testCascadeValidateOnMap() {
        def vehicleClass = ga.getDomainClass("Vehicle").clazz
        def doorClass = ga.getDomainClass("Door").clazz
        
        def vehicle = vehicleClass.newInstance(name:'One Door')
		def door = doorClass.newInstance()
		vehicle.doors['Front'] = door
		if (vehicle.validate()) {
			fail("Validating the vehicle did not cascade down to see that the door has no make")
		}

        assert vehicle.errors
        assertEquals 1, vehicle.errors.allErrors.size()

        assert vehicle.errors.getFieldError("make")

    }
}