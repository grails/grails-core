@SuppressWarnings('UnnecessarySemicolon')
package liquibase.ext.hibernate.database;

import groovy.transform.CompileStatic;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;

/**
 * Generic hibernate dialect used when an actual dialect cannot be determined.
 */
// TODO(groovyism-cleanup): semicolons kept to preserve rename-similarity against the original .java (see matrei's review on #16576). Removed by the stacked groovyism-cleanup PR.
@SuppressWarnings(['UnnecessarySemicolon'])
@CompileStatic
class HibernateGenericDialect extends Dialect {

    HibernateGenericDialect() {
        super(DatabaseVersion.make(7, 2));
    }

}
