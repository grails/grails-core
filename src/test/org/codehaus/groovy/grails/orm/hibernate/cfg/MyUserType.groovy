package org.codehaus.groovy.grails.orm.hibernate.cfg

import java.sql.*
import org.hibernate.*
import org.hibernate.usertype.UserType

/**
 * Example single-column Hibernate user type for use in testing.
 */
public class MyUserType implements UserType {

    private static final int[] SQL_TYPES = [ Types.VARCHAR ] as int[];

    public int[] sqlTypes() { return SQL_TYPES }
    public Class returnedClass() { return MyType }
    public boolean equals(Object x, Object y) { return x.name == y.name }
    public int hashCode(Object x) { return x.name.hashCode(); }
    public Object deepCopy(Object value) { return value; }
    public boolean isMutable() { return true }

    public Object nullSafeGet(ResultSet resultSet,
                              String[] names,
                              Object owner)
            throws HibernateException, SQLException {

      String name = resultSet.getString(names[0]);
      return resultSet.wasNull() ? null : new MyType(name: name)
    }

    public void nullSafeSet(PreparedStatement statement,
                            Object value,
                            int index)
            throws HibernateException, SQLException {

        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value.name);
        }
    }

    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    } 
}
