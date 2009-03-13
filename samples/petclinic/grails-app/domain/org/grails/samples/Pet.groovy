package org.grails.samples;

/**
 * Simple domain object representing a pet.
 *
 * @author Graeme Rocher
 */
class Pet {

	String name
	Date birthDate
	PetType type
	Owner owner
	
	static hasMany = [visits:Visit]
	
	static constraints = {
		name blank:false, validator: { val, obj ->
			if(!obj.id && obj.owner?.pets?.find { it.name == val } ) return "pet.duplicate"
		}
	}
}
