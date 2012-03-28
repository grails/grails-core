package org.codehaus.groovy.grails.orm.hibernate

enum TestEnum {

    Flurb(42), Skrabdle(123)

    final int value

    private TestEnum(int value) {
        this.value = value
    }

    static TestEnum byValue(Number value) {
        values().find { it.value == value.intValue() }
    }
}
