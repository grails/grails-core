@SuppressWarnings('UnnecessarySemicolon')
package liquibase.ext.hibernate.diff;

import groovy.transform.CompileStatic;
import liquibase.change.Change;
import liquibase.database.Database;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.ChangeGeneratorChain;
import liquibase.diff.output.changelog.core.MissingSequenceChangeGenerator;
import liquibase.ext.hibernate.database.HibernateDatabase;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Sequence;

// TODO(groovyism-cleanup): semicolons kept to preserve rename-similarity against the original .java (see matrei's review on #16576). Removed by the stacked groovyism-cleanup PR.
@SuppressWarnings(['UnnecessarySemicolon'])
@CompileStatic
class HibernateMissingSequenceChangeGenerator extends MissingSequenceChangeGenerator {

    @Override
    int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (Sequence.isAssignableFrom(objectType)) {
            return PRIORITY_ADDITIONAL;
        }
        return PRIORITY_NONE;
    }

    @Override
    Change[] fixMissing(
            DatabaseObject missingObject,
            DiffOutputControl control,
            Database referenceDatabase,
            Database comparisonDatabase,
            ChangeGeneratorChain chain) {
        if (referenceDatabase instanceof HibernateDatabase && !comparisonDatabase.supportsSequences()) {
            return new Change[0];
        } else if (comparisonDatabase instanceof HibernateDatabase && !referenceDatabase.supportsSequences()) {
            return new Change[0];
        } else {
            return super.fixMissing(missingObject, control, referenceDatabase, comparisonDatabase, chain);
        }
    }

}
