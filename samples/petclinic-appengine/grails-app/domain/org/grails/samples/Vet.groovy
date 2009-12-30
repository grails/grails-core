package org.grails.samples;

import javax.persistence.*
import com.google.appengine.api.datastore.Key;
/**
 * @author Graeme Rocher
 */
@Entity
class Vet  {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
 	Key id

	
	String firstName
	String lastName
	
	@OneToMany(mappedBy = "vet")	
	List<Speciality> specialities = new ArrayList<Speciality>()
	
	static constraints = {
		firstName blank:false
		lastName blank:false
		
	}

}
