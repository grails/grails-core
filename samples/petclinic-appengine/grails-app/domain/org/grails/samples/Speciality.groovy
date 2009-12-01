package org.grails.samples;

import javax.persistence.*
import com.google.appengine.api.datastore.Key;


/**
 * @author Graeme Rocher
 */
@Entity
class Speciality {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
 	Key id
	
	String name
	
	@ManyToOne
	Vet vet
	
	static constraints = {
		name blank:false
	}
}
