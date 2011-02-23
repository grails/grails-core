package org.codehaus.groovy.grails.orm.hibernate.cfg

/**
 * Represents a Join table in Grails mapping. It has a name which represents the name of the table, a key
 * for the primary key and a column which is the other side of the join.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class JoinTable {
    String name
    ColumnConfig key
    ColumnConfig column
}
