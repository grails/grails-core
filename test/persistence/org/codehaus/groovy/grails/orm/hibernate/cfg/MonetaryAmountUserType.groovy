package org.codehaus.groovy.grails.orm.hibernate.cfg

import java.sql.*
import org.hibernate.*
import org.hibernate.usertype.UserType

/**
 * Example multi-column Hibernate user type for use in testing.
 */
public class MonetaryAmountUserType implements UserType {

    private static final int[] SQL_TYPES = [ Types.NUMERIC, Types.VARCHAR ] as int[];

    public int[] sqlTypes() { return SQL_TYPES }
    public Class returnedClass() { return MonetaryAmount }
    public boolean equals(Object x, Object y) { return x == y }
    public int hashCode(Object x) { return x.hashCode() }
    public Object deepCopy(Object value) { return value }
    public boolean isMutable() { return false }

    public Object nullSafeGet(ResultSet resultSet,
                              String[] names,
                              Object owner)
            throws HibernateException, SQLException {
        if (resultSet.wasNull()) return null

        BigDecimal value = resultSet.getBigDecimal(names[0])
        Currency currency = Currency.getInstance(resultSet.getString(names[1]))
        return new MonetaryAmount(value, currency)
    }

    public void nullSafeSet(PreparedStatement statement,
                            Object amount,
                            int index)
            throws HibernateException, SQLException {

        if (value == null) {
            statement.setNull(index, SQL_TYPES[index]);
            statement.setNull(index + 1, SQL_TYPES[index + 1]);
        }
        else {
            String currencyCode = amount.currency.currencyCode
            statement.setBigDecimal(index, amount.value)
            statement.setString(index + 1, currencyCode)
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
