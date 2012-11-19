package org.codehaus.groovy.grails.orm.hibernate.validation

import org.codehaus.groovy.grails.validation.exceptions.ConstraintException
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass

/**
 * Checks UniqueConstraint using GORM-s.
 *
 * @author Alexey Sergeev
 */
class GORMUniqueConstraintTests extends AbstractUniqueConstraintTests {
    
    void testValidatorBeanPresence() {
        assertTrue applicationContext.containsBean("UserValidator")
        def validator = applicationContext.getBean("UserValidator")
        assertNotNull(validator.domainClass)
        assertTrue applicationContext.containsBean("LinkedUserValidator")
        validator = applicationContext.getBean("LinkedUserValidator")
        assertNotNull(validator.domainClass)
    }

    void testWrongUniqueParams() {
        // Test argument with wrong type (Long)
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class userClass
        shouldFail(ConstraintException) {
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
            new DefaultGrailsDomainClass(userClass).constrainedProperties
        }

        // Test list argument with wrong type (Long)
        shouldFail(ConstraintException) {
            userClass = gcl.parseClass('''
            class User {
                Long id
                Long version

                String login
                String grp
                String department
                String code

                static constraints = {
                    login(unique:['grp',1L])
                }
            }
            ''')
            new DefaultGrailsDomainClass(userClass).constrainedProperties
        }

        // Test argument with non-existent property value
        shouldFail(ConstraintException) {
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
            new DefaultGrailsDomainClass(userClass).constrainedProperties
        }

        // Test list argument with non-existent property value
        shouldFail(ConstraintException) {
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
            new DefaultGrailsDomainClass(userClass).constrainedProperties
        }

        // Test that right syntax doesn't throws exception
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
        new DefaultGrailsDomainClass(userClass).constrainedProperties
    }

    @Override
    void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class User {
    String login
    String grp
    String department
    String organization
    String code

    static belongsTo = LinkedUser

    static constraints = {
        login(unique:['grp','department'])
        department(unique:"organization")
        code(unique:true)
    }
}

@Entity
class LinkedUser {

    User user1
    User user2

    static constraints = {
        user2(unique:'user1')
    }
}
'''
    }
}
