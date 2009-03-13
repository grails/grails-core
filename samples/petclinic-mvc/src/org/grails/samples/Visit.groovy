package org.grails.samples

import grails.persistence.Entity


/**
 * Simple domain object representing a visit.
 *
 * @author Graeme Rocher
 */
@Entity
class Visit  {

	/** Holds value of property date. */
	Date date = new Date()

	/** Holds value of property description. */
	String description

	/** Holds value of property pet. */
	Pet pet


    boolean isNew() { id == null }

	static constraints = {
		description blank:false
	}
    
    static transients = ['new']
}
