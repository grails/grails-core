package org.grails.samples


/**
 * Simple domain object representing a visit.
 *
 * @author Graeme Rocher
 */
class Visit  {

	/** Holds value of property date. */
	Date date = new Date()

	/** Holds value of property description. */
	String description

	/** Holds value of property pet. */
	Pet pet


	static constraints = {
		description blank:false
	}
}
