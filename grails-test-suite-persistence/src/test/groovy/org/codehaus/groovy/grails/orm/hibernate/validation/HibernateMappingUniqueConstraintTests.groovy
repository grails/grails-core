package org.codehaus.groovy.grails.orm.hibernate.validation

/**
 * Checks UniqueConstraint basing on Hibernate mapped classes.
 *
 * @author Alexey Sergeev
 */
class HibernateMappingUniqueConstraintTests extends AbstractUniqueConstraintTests {

    @Override
    protected void configureDataSource() {
        gcl.parseClass('''
dataSource {
    pooled = true
    driverClassName = "org.h2.Driver"
    username = "sa"
    password = ""
    dbCreate = "create-drop"
    url = "jdbc:h2:mem:grailsIntTestDB"
    properties {
      maxWait = 10000
    }
}
hibernate {
    cache.use_second_level_cache=true
    cache.use_query_cache=true
    cache.provider_class='net.sf.ehcache.hibernate.EhCacheProvider'
    config.location = ['classpath:/org/codehaus/groovy/grails/orm/hibernate/validation/hibernate.cfg.xml']
}
''', "DataSource")
    }

    @Override
    void onSetUp() {
        /**
         * Well, here we define usual classes mapped using annotations.
         * Yet constraints we also define right here as it is more convenient
         * for the test case instead of creating separate Constraint file.
         */
        gcl.parseClass '''
import javax.persistence.*

@Entity
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    String login
    String grp
    String department
    String organization
    String code

    static constraints = {
        login(unique:['grp','department'])
        department(unique:"organization")
        code(unique:true)
    }
}
'''

        gcl.parseClass '''
import javax.persistence.*

@Entity
class LinkedUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    @ManyToOne
    @JoinColumn(name = "user1_id", nullable = false)
    User user1
    @ManyToOne
    @JoinColumn(name = "user2_id", nullable = false)
    User user2

    static constraints = {
        user2(unique:'user1')
    }
}
'''
    }
}
