package org.grails.samples;

/**
 * Simple domain object representing an person.
 *
 * @author Graeme Rocher
 */
class Person {

	String firstName
	String lastName
	
	static constraints = {
		firstName blank:false
		lastName blank:false
	}
}
