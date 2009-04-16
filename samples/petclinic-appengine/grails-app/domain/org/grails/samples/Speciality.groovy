package org.grails.samples;

import javax.jdo.annotations.*
import com.google.appengine.api.datastore.Key;


/**
 * @author Graeme Rocher
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
class Speciality {
	
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
 	Key key
	
   	@Persistent
	String name
	
	@Persistent
	Vet vet
	
	static constraints = {
		name blank:false
		key nullable:true
	}
}
