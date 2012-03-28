package org.codehaus.groovy.grails.orm.hibernate.cfg

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

import org.codehaus.groovy.grails.orm.hibernate.AbstractUserType
import org.hibernate.usertype.ParameterizedType

/**
 * Example single-column Hibernate user type for use in testing.
 */
class MyUserType extends AbstractUserType implements ParameterizedType {

    private static final int[] SQL_TYPES = [Types.VARCHAR]

    /** Parameter for testing ParameterizedType. */
    private String param1
    /** Parameter for testing ParameterizedType. */
    private String param2

    int[] sqlTypes() { SQL_TYPES }
    Class returnedClass() { MyType }
    boolean equals(x, y) { x.name == y.name }
    int hashCode(x) { x.name.hashCode() }
    boolean isMutable() { true }

    Object nullSafeGet(ResultSet rs, String[] names, owner) throws SQLException {
        String name = rs.getString(names[0])
        rs.wasNull() ? null : new MyType(name: name)
    }

    void nullSafeSet(PreparedStatement ps, value, int index) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR)
        }
        else {
            ps.setString(index, value.name)
        }
    }

    void setParameterValues(Properties params) {
        param1 = params.param1
        param2 = params.param2
    }
}
