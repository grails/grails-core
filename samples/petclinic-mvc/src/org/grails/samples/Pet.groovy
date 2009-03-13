package org.grails.samples

import grails.persistence.Entity;

/**
 * Simple domain object representing a pet.
 *
 * @author Graeme Rocher
 */

@Entity
class Pet {

	String name
	Date birthDate
	PetType type
	Owner owner

    boolean isNew() { id == null }
	
	static hasMany = [visits:Visit]
	static transients = ['new']

	static constraints = {
		name blank:false, validator: { val, obj ->
			if(!obj.id &&  obj.owner?.pets?.find { it.name == val && !it.is(obj) }) return "pet.duplicate"
		}
	}


}
