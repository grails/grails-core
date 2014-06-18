package org.grails.databinding.converters

import grails.databinding.SimpleMapDataBindingSource

import grails.databinding.SimpleDataBinder

import spock.lang.Specification

class CurrencyConversionSpec extends Specification {

    void 'Test binding String to a Currency'() {
        given:
        def binder = new SimpleDataBinder()
        binder.registerConverter new CurrencyValueConverter()
        def bank = new Bank()

        when:
        binder.bind bank, [currency: 'USD'] as SimpleMapDataBindingSource

        then:
        bank.currency instanceof Currency
        'USD' == bank.currency.currencyCode
    }
}

class Bank {
    Currency currency
}
