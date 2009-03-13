package org.codehaus.groovy.grails.orm.hibernate

import java.sql.Connection

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Oct 27, 2008
 */
class VersionColumnTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''

class VersionColumnBook {
    Long id
    Long version
    String title

    static mapping = {
        version 'v_number'
    }

}

''')
    }


    void testVersionColumnMapping() {
        Connection c = session.connection()
        // will fail if the column is not mapped correctly
        def ps = c.prepareStatement("select v_number from version_column_book")
        ps.execute()
    }

}