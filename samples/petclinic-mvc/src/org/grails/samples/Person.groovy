package org.grails.samples

import grails.persistence.Entity;

/**
 * Simple domain object representing an person.
 *
 * @author Graeme Rocher
 */
@Entity
class Person {

	String firstName
	String lastName
    
	static constraints = {
		firstName blank:false
		lastName blank:false
	}
}
