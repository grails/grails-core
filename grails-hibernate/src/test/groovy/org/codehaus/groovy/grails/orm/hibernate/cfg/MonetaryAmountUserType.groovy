package org.codehaus.groovy.grails.orm.hibernate.cfg

import java.sql.*
import org.hibernate.*
import org.hibernate.usertype.UserType

/**
 * Example multi-column Hibernate user type for use in testing.
 */
class MonetaryAmountUserType implements UserType {

    private static final int[] SQL_TYPES = [ Types.NUMERIC, Types.VARCHAR ] as int[]

    int[] sqlTypes() { SQL_TYPES }
    Class returnedClass() { MonetaryAmount }
    boolean equals(Object x, Object y) { x == y }
    int hashCode(Object x) { x.hashCode() }
    Object deepCopy(Object value) { value }
    boolean isMutable() { false }

    Object nullSafeGet(ResultSet resultSet, String[] names, owner) throws SQLException {

        if (resultSet.wasNull()) return null

        BigDecimal value = resultSet.getBigDecimal(names[0])
        Currency currency = Currency.getInstance(resultSet.getString(names[1]))
        new MonetaryAmount(value, currency)
    }

    void nullSafeSet(PreparedStatement statement, amount, int index) throws SQLException {
        if (value == null) {
            statement.setNull(index, SQL_TYPES[index])
            statement.setNull(index + 1, SQL_TYPES[index + 1])
        }
        else {
            String currencyCode = amount.currency.currencyCode
            statement.setBigDecimal(index, amount.value)
            statement.setString(index + 1, currencyCode)
        }
    }

    Serializable disassemble(Object value) { value }

    Object assemble(Serializable cached, Object owner) { cached }

    Object replace(Object original, Object target, Object owner) { original }
}
