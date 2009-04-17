package org.grails.samples

import javax.jdo.annotations.*
import com.google.appengine.api.datastore.Key;
/**
 * Simple domain object representing a visit.
 *
 * @author Graeme Rocher
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION, detachable="true")
class Visit  {

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
 	Key key


	/** Holds value of property date. */
	@Persistent
	Date date = new Date()

	/** Holds value of property description. */
	@Persistent	
	String description

	/** Holds value of property pet. */
	@Persistent	
	Pet pet


	static constraints = {
		description blank:false
		key nullable:true
	}
}
