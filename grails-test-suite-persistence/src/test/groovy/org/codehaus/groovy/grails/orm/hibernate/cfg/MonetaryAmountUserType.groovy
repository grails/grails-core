package org.codehaus.groovy.grails.orm.hibernate.cfg

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

import org.codehaus.groovy.grails.orm.hibernate.AbstractUserType

/**
 * Example multi-column Hibernate user type for use in testing.
 */
class MonetaryAmountUserType extends AbstractUserType {

    private static final int[] SQL_TYPES = [Types.NUMERIC, Types.VARCHAR]

    int[] sqlTypes() { SQL_TYPES }
    Class returnedClass() { MonetaryAmount }

    Object nullSafeGet(ResultSet rs, String[] names, owner) throws SQLException {

        if (rs.wasNull()) return null

        BigDecimal value = rs.getBigDecimal(names[0])
        Currency currency = Currency.getInstance(rs.getString(names[1]))
        new MonetaryAmount(value, currency)
    }

    void nullSafeSet(PreparedStatement ps, amount, int index) throws SQLException {
        if (amount == null) {
            ps.setNull(index, SQL_TYPES[index])
            ps.setNull(index + 1, SQL_TYPES[index + 1])
        }
        else {
            String currencyCode = amount.currency.currencyCode
            ps.setBigDecimal(index, amount.value)
            ps.setString(index + 1, currencyCode)
        }
    }
}
