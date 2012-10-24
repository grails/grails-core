package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.dialect.HSQLDialect

/**
 * @author Burt Beckwith
 */
class NamingTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class NamingTests1 {
    String name
}

@Entity
class NamingTests2 {
    String name
    static hasMany = [foos: NamingTests1]
}

@Entity
class NamingTests3 {
    String name
    static hasMany = [foos: NamingTests4]
}

@Entity
class NamingTests4 {
    String name
    static hasMany = [bars: NamingTests3]
    static belongsTo = NamingTests3
}

@Entity
class NamingTests5 {
    String name
    static hasMany = [foos: NamingTests6]
    static mapping = {
          table '`NamingTest5`'
    }
}

@Entity
class NamingTests6 {
    String name
    static hasMany = [bars: NamingTests5]
    static belongsTo = NamingTests5
}
'''
    }

    void testNames() {

        def hibernateConfig = appCtx.getBean('&sessionFactory').configuration
        def sql = hibernateConfig.generateSchemaCreationScript(new HSQLDialect()).sort()
        def tableNames = findTableNames(sql)

        assertEquals 9, tableNames.size()
        4.times { assertTrue "naming_tests${it + 1}}", tableNames.contains('naming_tests' + (it + 1)) }
        assertTrue tableNames.contains('"NamingTest5"')
        assertTrue tableNames.contains("naming_tests6")

        assertTrue tableNames.contains('naming_tests2_naming_tests1')
        assertTrue tableNames.contains('naming_tests3_foos')
        assertTrue tableNames.contains('naming_test5_foos')
    }

    private List findTableNames(sql) {
        def names = []
        sql.each { String ddl ->
            if (ddl.startsWith('create table ')) {
                ddl -= 'create table '
                names << ddl.substring(0, ddl.indexOf(' '))
            }
        }
        names
    }
}
