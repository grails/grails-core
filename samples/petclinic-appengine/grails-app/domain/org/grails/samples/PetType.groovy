package org.grails.samples;

import javax.jdo.annotations.*
import com.google.appengine.api.datastore.Key;
/**
 * @author Graeme Rocher
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
class PetType  {
	
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
 	Key key


	@Persistent
	String name
	
	static constraints = {
		name blank:false
		key nullable:true
	}
}
