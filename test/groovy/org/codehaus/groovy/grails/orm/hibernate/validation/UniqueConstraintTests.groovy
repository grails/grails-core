package org.codehaus.groovy.grails.orm.hibernate.validation;

import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.validation.*
import org.codehaus.groovy.grails.orm.hibernate.*

class UniqueConstraintTests extends AbstractGrailsHibernateTests {

	void testUniqueConstraint() {

	    assertTrue applicationContext.containsBean("UserValidator")
	    def validator = applicationContext.getBean("UserValidator")
	    assertNotNull(validator.domainClass)
	    
	    // login should be unique against group and code should be unique at all 
		def userClass = ga.getDomainClass("User").clazz

        def user = userClass.newInstance()
        user.code = "123"
        user.login = "grails"
        user.grp = "some-group"
        user.department = "department1"
        user.validate()
        assertFalse user.hasErrors()
        user.save(true)

        user = userClass.newInstance()
        user.code = "123"
        user.login = "another"
        user.grp = "another-group"
        user.department = "department2"
        user.validate()
        // code should fire unique constraint
        assertTrue user.hasErrors()
        
        user.code = "321"
        user.login = "grails"
        user.grp = "some-group"
        user.department = "department1"
        user.validate()
        // login should fire unique constraint since it is in the same grp and same department
        assertTrue user.hasErrors()
        
        user.grp = "another-group"
        user.validate()
        // login shouldn't fire unique constraint since it is in the same department but not in the same grp
        assertFalse user.hasErrors()

        user.grp = "some-group"
        user.department = "department2"
        user.validate()
        // login shouldn't fire unique constraint since it is in the same grp but not in the same department
        assertFalse user.hasErrors()

        user.login = "another-login"
        user.grp = "some-group"
        user.department = "department1"
        user.validate()
        // grp should fire unique constraint since it is in the same department
        assertTrue user.hasErrors()

        user.department = "department2"
        user.validate()
        // grp shouldn't fire unique constraint since it isn't in same department as first object
        assertFalse user.hasErrors()
	}
	
	void testWrongUniqueParams() {
		// Test argument with wrong type (Long)
		GroovyClassLoader gcl = new GroovyClassLoader();
		Class userClass
		try {
			userClass = gcl.parseClass('''
			class User {
			    Long id
			    Long version
			
			    String login
			    String grp
			    String department
			    String code
			
			    static constraints = {
			        login(unique:1L)
			    }
			}
			''')
			new DefaultGrailsDomainClass(userClass);
			fail("Exception expected")
		} catch( Exception e ) {
			// Greate
		}

		// Test list argument with wrong type (Long)
		try {
			userClass = gcl.parseClass('''
			class User {
			    Long id
			    Long version
			
			    String login
			    String grp
			    String department
			    String code
			
			    static constraints = {
			        login(unique:['grp',new Long(1)])
			    }
			}
			''')
			new DefaultGrailsDomainClass(userClass);
			fail("Exception expected")
		} catch( Exception e ) {
			// Greate
		}

		// Test argument with non-existent property value
		try {
			userClass = gcl.parseClass('''
				class User {
				    Long id
				    Long version
				
				    String login
				    String grp
				    String department
				    String code
				
				    static constraints = {
				        login(unique:'test')
				    }
				}
				''')
			new DefaultGrailsDomainClass(userClass);
			fail("Exception expected")
		} catch( Exception e ) {
			// Greate
		}

		// Test list argument with non-existent property value
		try {
			userClass = gcl.parseClass('''
				class User {
				    Long id
				    Long version
				
				    String login
				    String grp
				    String department
				    String code
				
				    static constraints = {
				        login(unique:['grp','test'])
				    }
				}
				''')
			new DefaultGrailsDomainClass(userClass);
			fail("Exception expected")
		} catch( Exception e ) {
			// Greate
		}

		// Test that right syntax doesn't throws exception
		try {
			userClass = gcl.parseClass('''
				class User {
				    Long id
				    Long version
				
				    String login
				    String grp
				    String department
				    String code
				
				    static constraints = {
				        login(unique:['grp'])
				    }
				}
				''')
				new DefaultGrailsDomainClass(userClass);
		} catch( Exception e ) {
			fail("Exception isn't expected")
		}
	}


	void onSetUp() {

		this.gcl.parseClass('''
class User {
    Long id
    Long version

    String login
    String grp
    String department
    String code

    static constraints = {
        login(unique:['grp','department'])
        grp(unique:"department")
        code(unique:true)
    }
}
'''
		)
	}

	void onTearDown() {

	}
}
