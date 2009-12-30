package org.grails.samples

import javax.persistence.*
import com.google.appengine.api.datastore.Key;
/**
 * Simple domain object representing a visit.
 *
 * @author Graeme Rocher
 */
@Entity
class Visit  {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
 	Key id


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
