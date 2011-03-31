package org.codehaus.groovy.grails.orm.hibernate

import java.sql.Connection

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Oct 27, 2008
 */
class VersionColumnTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

class VersionColumnBook {
    Long id
    Long version
    String title

    static mapping = {
        version 'v_number'
    }
}

@Entity
class DateVersion {
    String name
    Date version
}
@Entity
class LongVersion {
    String name
    Long version
}
'''
    }

    void testVersionColumnMapping() {
        // will fail if the column is not mapped correctly
        session.connection().prepareStatement("select v_number from version_column_book").execute()
    }

    void testLongVersion() {
        assertEquals Long, ga.getDomainClass('LongVersion').version.type
    }

    void testDateVersion() {
        assertEquals Date, ga.getDomainClass('DateVersion').version.type
    }
}
