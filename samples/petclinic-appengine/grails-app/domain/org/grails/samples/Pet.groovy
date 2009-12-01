package org.grails.samples;


import javax.persistence.*
import com.google.appengine.api.datastore.Key;
/**
 * Simple domain object representing a pet.
 *
 * @author Graeme Rocher
 */
@Entity
class Pet {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
 	Key id

	String name
	Date birthDate

	@OneToOne
	PetType type
	
	@ManyToOne
	Owner owner
	
	@OneToMany(mappedBy = "pet")	
	List<Visit> visits = new ArrayList<Visit>()
	
	static constraints = {
		name blank:false, validator: { val, obj ->
			if(!obj.id && obj.owner?.pets?.find { it.name == val } ) return "pet.duplicate"
		}
	}
}
