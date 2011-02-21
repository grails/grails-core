package org.codehaus.groovy.grails.orm.hibernate.cfg

import java.sql.*
import org.hibernate.*
import org.hibernate.usertype.UserType
import org.hibernate.usertype.ParameterizedType

/**
 * Example single-column Hibernate user type for use in testing.
 */
class MyUserType implements UserType, ParameterizedType {

    private static final int[] SQL_TYPES = [ Types.VARCHAR ] as int[]

    /** Parameter for testing ParameterizedType. */
    private String param1
    /** Parameter for testing ParameterizedType. */
    private String param2

    int[] sqlTypes() { SQL_TYPES }
    Class returnedClass() { MyType }
    boolean equals(Object x, Object y) { x.name == y.name }
    int hashCode(Object x) { x.name.hashCode() }
    Object deepCopy(Object value) { value }
    boolean isMutable() { true }

    Object nullSafeGet(ResultSet resultSet, String[] names, owner) throws SQLException {
        String name = resultSet.getString(names[0])
      resultSet.wasNull() ? null : new MyType(name: name)
    }

    void nullSafeSet(PreparedStatement statement, value, int index) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR)
        }
        else {
            statement.setString(index, value.name)
        }
    }

    Serializable disassemble(Object value) { value }

    Object assemble(Serializable cached, Object owner) { cached }

    Object replace(Object original, Object target, Object owner) { original }

    void setParameterValues(Properties params) {
        param1 = params.param1
        param2 = params.param2
    }
}
