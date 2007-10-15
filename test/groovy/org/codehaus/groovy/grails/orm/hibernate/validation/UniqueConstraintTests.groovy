package org.codehaus.groovy.grails.orm.hibernate.validation;

import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.validation.*
import org.codehaus.groovy.grails.orm.hibernate.*

class UniqueConstraintTests extends AbstractGrailsHibernateTests {

    void testApplying() {
        assertTrue applicationContext.containsBean("UserValidator")
        def validator = applicationContext.getBean("UserValidator")
        assertNotNull(validator.domainClass)

        def userClass = ga.getDomainClass("User")
        ga.getDomainClass("User").constrainedProperties.each {key, value ->
            if (key == 'code') {
                value.appliedConstraints.each {
                    if (it.name == UniqueConstraint.UNIQUE_CONSTRAINT) assertTrue it.unique
                }
            } else if (key == 'login') {
                value.appliedConstraints.each {
                    if (it.name == UniqueConstraint.UNIQUE_CONSTRAINT) assertTrue it.unique
                }
            } else if (key == 'department') {
                value.appliedConstraints.each {
                    if (it.name == UniqueConstraint.UNIQUE_CONSTRAINT) assertTrue it.unique
                }
            } else if (key == 'organization') {
                value.appliedConstraints.each {
                    if (it.name == UniqueConstraint.UNIQUE_CONSTRAINT) assertFalse it.unique
                }
            }
        }
    }

    void testValidation() {
        def userClass = ga.getDomainClass("User").clazz

        // The first object shouldn't fire any unique constraints
        def user = userClass.newInstance()
        user.code = "123"
        user.login = "login1"
        user.grp = "group1"
        user.department = "department1"
        user.organization = "organization1"
        user.validate()
        assertFalse user.hasErrors()
        user.save(true)

        def id = user.id

        // instance with same id shouldn't fire unique constraint 
        user = userClass.newInstance()
        user.id = id
        user.code = "123"
        user.login = "login1"
        user.grp = "group1"
        user.department = "department1"
        user.organization = "organization1"
        user.validate()
        assertFalse user.hasErrors()

        // but instance with different id should 
        user.id = 123L
        user.validate()
        assertTrue user.hasErrors()

        // 'code' should fire unique constraint
        user = userClass.newInstance()
        user.code = "123"
        user.login = "login2"
        user.grp = "group2"
        user.department = "department2"
        user.organization = "organization2"
        user.validate()
        assertTrue user.hasErrors()

        // 'login' should fire unique constraint since it is in the same grp and same department
        user.code = "321"
        user.login = "login1"
        user.grp = "group1"
        user.department = "department1"
        user.organization = "organization2"
        user.validate()
        assertTrue user.hasErrors()

        // 'login' shouldn't fire unique constraint since it is in the same department but not in the same grp
        user.grp = "group2"
        user.validate()
        assertFalse user.hasErrors()

        // 'login' shouldn't fire unique constraint since it is in the same grp but not in the same department
        user.grp = "group1"
        user.department = "department2"
        user.validate()
        assertFalse user.hasErrors()

        // 'department' should fire unique constraint since it is in the same organization
        user.login = "login2"
        user.grp = "group2"
        user.department = "department1"
        user.organization = "organization1"
        user.validate()
        assertTrue user.hasErrors()

        // 'department' shouldn't fire unique constraint since it isn't in same organization as first object
        user.organization = "organization2"
        user.validate()
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
        } catch (Exception e) {
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
        } catch (Exception e) {
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
        } catch (Exception e) {
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
        } catch (Exception e) {
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
        } catch (Exception e) {
            fail("Exception isn't expected")
        }
    }

    void testRelationships() {
        def userClass = ga.getDomainClass("User").clazz
        def linkClass = ga.getDomainClass("Link1").clazz

        def user1 = userClass.newInstance()
        user1.code = "1"
        user1.login = "login1"
        user1.grp = "group1"
        user1.department = "department1"
        user1.organization = "organization1"
        user1.save(true)

        def user2 = userClass.newInstance()
        user2.code = "2"
        user2.login = "login2"
        user2.grp = "group2"
        user2.department = "department2"
        user2.organization = "organization2"
        user2.save(true)

        def user3 = userClass.newInstance()
        user3.code = "3"
        user3.login = "login3"
        user3.grp = "group3"
        user3.department = "department3"
        user3.organization = "organization3"
        user3.save(true)

        def link = linkClass.newInstance()
        link.u1 = user1
        link.u2 = user2
        link.validate()
        assertFalse link.hasErrors()
        link.save(true)

        link = linkClass.newInstance()
        link.u1 = user1
        link.u2 = user3
        link.validate()
        assertFalse link.hasErrors()

        link.u2 = user2
        link.validate()
        assertTrue link.hasErrors()
    }


    void onSetUp() {
        gcl.parseClass('''
            class User {
                Long id
                Long version

                String login
                String grp
                String department
                String organization
                String code

                static belongsTo = Link1

                static constraints = {
                    login(unique:['grp','department'])
                    department(unique:"organization")
                    code(unique:true)
                }
            }
            class Link1 {
                Long id
                Long version

                User u1
                User u2


                static constraints = {
                    u2(unique:'u1')
                }
            }
            '''
                )

    }

    void onTearDown() {

    }
}
