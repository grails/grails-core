package org.codehaus.groovy.grails.orm.hibernate.validation;

import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.validation.*
import org.codehaus.groovy.grails.orm.hibernate.*

class UniqueConstraintTests extends AbstractGrailsHibernateTests {

	void testUniqueConstraint() {

	    assertTrue applicationContext.containsBean("UserValidator")
	    def validator = applicationContext.getBean("UserValidator")
	    assertNotNull(validator.domainClass)
	    
	    
		def userClass = ga.getDomainClass("User").clazz

        def user = userClass.newInstance()

        user.login = "grails"
        user.email = "info@grails.org"
        user.save(true)


        assertFalse user.hasErrors()

        user.discard()

        user =  userClass.get(1)

        user.email = "rubbishemail"
        user.save(true)

        assertTrue user.hasErrors()

        user.discard()
        user =  userClass.get(1)

        assertEquals "info@grails.org", user.email

	}


	void onSetUp() {

		this.gcl.parseClass('''
class User {
    Long id
    Long version
    String login
    String email
    static constraints = {
        login(unique:true)
        email(email:true)
    }
}
'''
		)
	}

	void onTearDown() {

	}
}
