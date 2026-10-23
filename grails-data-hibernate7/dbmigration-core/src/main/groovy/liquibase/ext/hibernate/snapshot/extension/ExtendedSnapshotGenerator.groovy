@SuppressWarnings('UnnecessarySemicolon')
package liquibase.ext.hibernate.snapshot.extension;

import groovy.transform.CompileStatic;

// TODO(groovyism-cleanup): semicolons kept to preserve rename-similarity against the original .java (see matrei's review on #16576). Removed by the stacked groovyism-cleanup PR.
@SuppressWarnings(['UnnecessarySemicolon'])
@CompileStatic
interface ExtendedSnapshotGenerator<T, U> {

    U snapshot(T object);

    boolean supports(T object);

}
