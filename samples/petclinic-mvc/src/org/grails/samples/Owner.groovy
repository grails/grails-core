package org.grails.samples

import grails.persistence.Entity;

/**
 * Simple domain object representing an owner.
 *
 * @author Graeme Rocher
 */
@Entity
class Owner extends Person {

	String address
	String city
	String telephone

    boolean isNew() { id == null }

	static hasMany = [pets:Pet]
    static transients = ['new']	
	static constraints = {
		address blank:false
		city blank:false		
		telephone matches:/\d+/, blank:false
	}

    static mapping = {
        pets fetch:'join'
    }
}
