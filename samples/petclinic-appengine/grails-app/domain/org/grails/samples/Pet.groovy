package org.grails.samples;


import javax.jdo.annotations.*
import com.google.appengine.api.datastore.Key;
/**
 * Simple domain object representing a pet.
 *
 * @author Graeme Rocher
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION, detachable="true")
class Pet {

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
 	Key key

	@Persistent
	String name
	
	@Persistent	
	Date birthDate
	
	@Persistent
	PetType type
	
	@Persistent
	Owner owner
	
	@Persistent(mappedBy = "pet")	
	List<Visit> visits = new ArrayList<Visit>()
	
	static constraints = {
		key nullable:true		
		name blank:false, validator: { val, obj ->
			if(!obj.id && obj.owner?.pets?.find { it.name == val } ) return "pet.duplicate"
		}
	}
}
