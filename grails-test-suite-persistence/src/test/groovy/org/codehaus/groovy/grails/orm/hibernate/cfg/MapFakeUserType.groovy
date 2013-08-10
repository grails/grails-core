package org.codehaus.groovy.grails.orm.hibernate.cfg

import java.io.Serializable
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import java.util.HashMap
import java.util.Map

import org.hibernate.HibernateException
import org.hibernate.usertype.UserType

public class MapFakeUserType implements UserType {

    int[] sqlTypes() { Types.VARCHAR }
    Class returnedClass() { MapFakeUserType }

    public Object nullSafeGet(ResultSet rs, String[] names, owner) throws SQLException {
        String name = rs.getString(names[0])
        rs.wasNull() ? null : new MapFakeUserType(name: name)
    }

    public void nullSafeSet(PreparedStatement ps, value, int index) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR)
        }
        else {
            ps.setString(index, value.name)
        }
    }

    public boolean equals(Object x, Object y) throws HibernateException {
        return x == y;
    }

    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    public boolean isMutable() {
        return false;
    }

    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable)value;
    }

    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }
}
