package org.grails.samples;

import javax.jdo.annotations.*
import com.google.appengine.api.datastore.Key;
/**
 * @author Graeme Rocher
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
class Vet  {

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
 	Key key

	
	@Persistent	
	String firstName
	
	@Persistent	
	String lastName
	
	@Persistent(mappedBy = "vet")	
	List<Speciality> specialities = new ArrayList<Speciality>()
	
	static constraints = {
		key nullable:true
		firstName blank:false
		lastName blank:false
		
	}

}
