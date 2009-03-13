package org.grails.samples;


/**
 * Simple domain object representing a veterinarian.
 *
 * @author Graeme Rocher
 */
class Vet extends Person {

	static hasMany = [specialities:Speciality]
}
