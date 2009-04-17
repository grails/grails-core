package org.grails.samples;


import javax.jdo.annotations.*
import com.google.appengine.api.datastore.Key;
/**
 * Simple domain object representing an owner.
 *
 * @author Graeme Rocher
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION, detachable="true")
class Owner  {

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
 	Key key

	
	@Persistent	
	String firstName
	
	@Persistent	
	String lastName
	
	@Persistent	
	String address
	
	@Persistent		
	String city
	
	@Persistent		
	String telephone

	@Persistent(mappedBy = "owner")
	List<Pet> pets = new ArrayList<Pet>()
	
	static constraints = {
		key nullable:true
		firstName blank:false
		lastName blank:false
		address blank:false
		city blank:false		
		telephone matches:/\d+/, blank:false
	}
}
