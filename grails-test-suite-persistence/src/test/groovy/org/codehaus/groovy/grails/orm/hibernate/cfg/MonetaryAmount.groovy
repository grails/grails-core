package org.codehaus.groovy.grails.orm.hibernate.cfg

/**
 * Example custom type used for testing.
 */
class MonetaryAmount implements Serializable {

    final BigDecimal value
    final Currency currency

    MonetaryAmount(value, Currency currency) {
        this.value = value?.toBigDecimal()
        this.currency = currency
    }

    boolean equals(o) {
        if (!(o instanceof MonetaryAmount)) return false
        o.value == value && o.currency == currency
    }

    int hashCode() {
        int result = 23468
        result += 37 * value.hashCode()
        result += 37 * currency.hashCode()
        result
    }
}
