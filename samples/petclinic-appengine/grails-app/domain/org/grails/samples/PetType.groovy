package org.grails.samples;

import javax.persistence.*
import com.google.appengine.api.datastore.Key;
/**
 * @author Graeme Rocher
 */
@Entity
class PetType  {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
 	Key id

	String name
	
	static constraints = {
		name blank:false
	}
}
