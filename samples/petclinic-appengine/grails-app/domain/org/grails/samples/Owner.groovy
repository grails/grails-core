package org.grails.samples;


import javax.persistence.*
import com.google.appengine.api.datastore.Key;
/**
 * Simple domain object representing an owner.
 *
 * @author Graeme Rocher
 */
@Entity
class Owner  {


	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
 	Key id

	
	String firstName
	String lastName
	String address
	String city
	String telephone

	@OneToMany(mappedBy = "owner")
	List<Pet> pets = new ArrayList<Pet>()
	
	static constraints = {
		firstName blank:false
		lastName blank:false
		address blank:false
		city blank:false		
		telephone matches:/\d+/, blank:false
	}
}
