/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.grails.orm.hibernate

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.Immutable
import groovy.transform.ToString

import grails.gorm.annotation.Entity
import grails.gorm.hibernate.HibernateEntity
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.orm.hibernate.cfg.Settings
import org.hibernate.HibernateException
import org.hibernate.MappingException
import org.hibernate.mapping.Column
import org.hibernate.mapping.Component
import org.hibernate.metamodel.spi.ValueAccess
import org.hibernate.usertype.CompositeUserType
import spock.lang.Specification

/**
 * A property mapped with a Hibernate {@link CompositeUserType} (issue #16688): the type spans several
 * columns, one per attribute of the embeddable class the user type names, in alphabetical attribute order.
 */
class CompositeUserTypeMappingSpec extends Specification {

    void "a composite user type binds one column per embeddable attribute in alphabetical order"() {
        given: 'a datastore with an entity that maps two properties with the composite user type'
        HibernateDatastore datastore = createDatastore('compositeUserTypeExplicit', {}, CompositeUserTypePayment)

        expect: 'each property is a component whose type is the composite user type'
        Component amount = componentOf(datastore, CompositeUserTypePayment, 'amount')
        amount.typeName == CompositeMoneyType.name
        amount.componentClassName == CompositeMoney.name

        and: 'the configured columns are assigned to the attributes in alphabetical attribute order'
        amount.properties*.name == ['amount', 'currency']
        columnNamesOf(amount) == ['amount', 'currency']
        columnNamesOf(componentOf(datastore, CompositeUserTypePayment, 'fee')) == ['fee_amount', 'fee_currency']

        and: 'the column configuration and the nullability of the property apply to the columns'
        Column currency = columnsOf(amount).last()
        currency.sqlType == 'char(3)'
        !currency.nullable
        columnsOf(componentOf(datastore, CompositeUserTypePayment, 'fee')).every { it.nullable }

        and: 'the table holds every column'
        tableColumnNames(datastore, CompositeUserTypePayment).containsAll(
                ['amount', 'currency', 'fee_amount', 'fee_currency', 'reference'])

        and: 'the schema reflects the column configuration'
        schemaColumn(datastore, 'COMPOSITE_USER_TYPE_PAYMENT', 'CURRENCY') == [type: 'CHARACTER', length: 3, nullable: 'NO']
        schemaColumn(datastore, 'COMPOSITE_USER_TYPE_PAYMENT', 'FEE_CURRENCY').nullable == 'YES'

        cleanup:
        datastore?.close()
    }

    void "a composite user type value is written to and read from its columns"() {
        given:
        HibernateDatastore datastore = createDatastore('compositeUserTypeRoundTrip', {}, CompositeUserTypePayment)

        when: 'an entity is saved'
        Long id = CompositeUserTypePayment.withTransaction {
            new CompositeUserTypePayment(
                    reference: 'order-1',
                    amount: new CompositeMoney(amount: 1999, currency: 'EUR'),
                    fee: new CompositeMoney(amount: 50, currency: 'EUR')
            ).save(flush: true, failOnError: true).id
        }

        then: 'the user type writes each attribute to its column'
        storedRow(datastore, id) == [amount: 1999L, currency: 'EUR', fee_amount: 50L, fee_currency: 'EUR']

        and: 'the user type reads the attributes back'
        CompositeUserTypePayment.withTransaction {
            CompositeUserTypePayment payment = CompositeUserTypePayment.get(id)
            payment.amount == new CompositeMoney(amount: 1999, currency: 'EUR') &&
                    payment.fee == new CompositeMoney(amount: 50, currency: 'EUR')
        }

        when: 'the values are replaced'
        CompositeUserTypePayment.withTransaction {
            CompositeUserTypePayment payment = CompositeUserTypePayment.get(id)
            payment.amount = new CompositeMoney(amount: 2499, currency: 'USD')
            payment.fee = null
            payment.save(flush: true, failOnError: true)
        }

        then: 'the change is written and a null value clears its columns'
        storedRow(datastore, id) == [amount: 2499L, currency: 'USD', fee_amount: null, fee_currency: null]
        CompositeUserTypePayment.withTransaction {
            CompositeUserTypePayment payment = CompositeUserTypePayment.get(id)
            payment.amount == new CompositeMoney(amount: 2499, currency: 'USD') && payment.fee == null
        }

        cleanup:
        datastore?.close()
    }

    void "a composite user type property is queried by value and by attribute"() {
        given:
        HibernateDatastore datastore = createDatastore('compositeUserTypeQuery', {}, CompositeUserTypePayment)
        CompositeMoney twenty = new CompositeMoney(amount: 2000, currency: 'EUR')
        CompositeUserTypePayment.withTransaction {
            new CompositeUserTypePayment(reference: 'ten', amount: new CompositeMoney(amount: 1000, currency: 'EUR'))
                    .save(failOnError: true)
            new CompositeUserTypePayment(reference: 'twenty', amount: twenty).save(failOnError: true)
            new CompositeUserTypePayment(reference: 'twenty-usd', amount: new CompositeMoney(amount: 2000, currency: 'USD'))
                    .save(flush: true, failOnError: true)
        }

        expect: 'HQL compares the whole value and reaches into its attributes'
        CompositeUserTypePayment.withTransaction {
            CompositeUserTypePayment.executeQuery(
                    'select p.reference from CompositeUserTypePayment p where p.amount = :amount', [amount: twenty]) == ['twenty'] &&
                    CompositeUserTypePayment.executeQuery(
                            'select p.reference from CompositeUserTypePayment p where p.amount.currency = :currency order by p.amount.amount',
                            [currency: 'EUR']) == ['ten', 'twenty']
        }

        and: 'criteria and dynamic finders compare the whole value'
        CompositeUserTypePayment.withTransaction {
            CompositeUserTypePayment.createCriteria().list { eq('amount', twenty) }*.reference == ['twenty'] &&
                    CompositeUserTypePayment.findAllByAmount(twenty)*.reference == ['twenty'] &&
                    CompositeUserTypePayment.countByAmount(twenty) == 1
        }

        cleanup:
        datastore?.close()
    }

    void "a composite user type without a column mapping names its columns after the property and the attribute"() {
        given:
        HibernateDatastore datastore = createDatastore('compositeUserTypeDefaultColumns', {}, CompositeUserTypeDefaultColumns)

        expect:
        columnNamesOf(componentOf(datastore, CompositeUserTypeDefaultColumns, 'amount')) == ['amount_amount', 'amount_currency']

        when:
        Long id = CompositeUserTypeDefaultColumns.withTransaction {
            new CompositeUserTypeDefaultColumns(amount: new CompositeMoney(amount: 5, currency: 'GBP'))
                    .save(flush: true, failOnError: true).id
        }

        then:
        CompositeUserTypeDefaultColumns.withTransaction {
            CompositeUserTypeDefaultColumns.get(id).amount == new CompositeMoney(amount: 5, currency: 'GBP')
        }

        cleanup:
        datastore?.close()
    }

    void "a composite user type registered as a user-type for its class applies without a per-property type"() {
        given: 'a datastore whose default mapping registers the composite user type for the value class'
        HibernateDatastore datastore = createDatastore('compositeUserTypeRegistered', {
            'user-type'(type: CompositeMoneyType, 'class': CompositeMoney)
        }, CompositeUserTypeRegistered)

        expect: 'the property is bound as a component with the columns of its own mapping'
        Component amount = componentOf(datastore, CompositeUserTypeRegistered, 'amount')
        amount.typeName == CompositeMoneyType.name
        columnNamesOf(amount) == ['money_value', 'money_currency']

        when:
        Long id = CompositeUserTypeRegistered.withTransaction {
            new CompositeUserTypeRegistered(amount: new CompositeMoney(amount: 7, currency: 'CHF'))
                    .save(flush: true, failOnError: true).id
        }

        then:
        CompositeUserTypeRegistered.withTransaction {
            CompositeUserTypeRegistered.get(id).amount == new CompositeMoney(amount: 7, currency: 'CHF')
        }

        cleanup:
        datastore?.close()
    }

    void "a composite user type takes its attributes from its embeddable class when that differs from the value class"() {
        given: 'a composite user type whose embeddable class is a record describing the value class'
        HibernateDatastore datastore = createDatastore('compositeUserTypeTemplate', {}, CompositeUserTypeTemplated)

        expect: 'the component is described by the embeddable class, not by the value class'
        CompositeMoneyTemplate.isRecord()
        Component amount = componentOf(datastore, CompositeUserTypeTemplated, 'amount')
        amount.componentClassName == CompositeMoneyTemplate.name
        amount.properties*.name == ['amount', 'currency']
        columnNamesOf(amount) == ['amount_amount', 'amount_currency']

        when:
        Long id = CompositeUserTypeTemplated.withTransaction {
            new CompositeUserTypeTemplated(amount: new CompositeMoney(amount: 11, currency: 'JPY'))
                    .save(flush: true, failOnError: true).id
        }

        then:
        CompositeUserTypeTemplated.withTransaction {
            CompositeUserTypeTemplated.get(id).amount == new CompositeMoney(amount: 11, currency: 'JPY')
        }

        cleanup:
        datastore?.close()
    }

    void "a composite user type takes its attributes from the fields and the readable properties of its embeddable class"() {
        given: 'an immutable value class with getters only, and an embeddable class with private fields only'
        HibernateDatastore datastore = createDatastore('compositeUserTypeImmutable', {},
                CompositeUserTypeImmutable, CompositeUserTypeFieldsTemplated)

        expect: 'the final fields of the immutable value class are its attributes, less the computed property it lists in transients'
        Component price = componentOf(datastore, CompositeUserTypeImmutable, 'price')
        price.componentClassName == CompositeImmutableMoney.name
        price.properties*.name == ['amount', 'currency']
        columnNamesOf(price) == ['price_amount', 'price_currency']

        and: 'the private fields of an embeddable class without accessors are its attributes'
        Component amount = componentOf(datastore, CompositeUserTypeFieldsTemplated, 'amount')
        amount.componentClassName == CompositeMoneyFields.name
        amount.properties*.name == ['amount', 'currency']
        columnNamesOf(amount) == ['amount_amount', 'amount_currency']

        when:
        Long priceId = CompositeUserTypeImmutable.withTransaction {
            new CompositeUserTypeImmutable(price: new CompositeImmutableMoney(1250, 'EUR'))
                    .save(flush: true, failOnError: true).id
        }
        Long amountId = CompositeUserTypeFieldsTemplated.withTransaction {
            new CompositeUserTypeFieldsTemplated(amount: new CompositeMoney(amount: 13, currency: 'NOK'))
                    .save(flush: true, failOnError: true).id
        }

        then:
        CompositeUserTypeImmutable.withTransaction {
            CompositeUserTypeImmutable.get(priceId).price == new CompositeImmutableMoney(1250, 'EUR')
        }
        CompositeUserTypeFieldsTemplated.withTransaction {
            CompositeUserTypeFieldsTemplated.get(amountId).amount == new CompositeMoney(amount: 13, currency: 'NOK')
        }

        cleanup:
        datastore?.close()
    }

    void "more columns than the embeddable class has attributes is a mapping error"() {
        when:
        createDatastore('compositeUserTypeTooManyColumns', {}, CompositeUserTypeTooManyColumns)

        then:
        Throwable thrown = thrown()
        MappingException cause = causeChain(thrown).find { it instanceof MappingException } as MappingException
        cause.message.contains(CompositeUserTypeTooManyColumns.name)
        cause.message.contains('amount')
        cause.message.contains('3')
        cause.message.contains('2')
    }

    private static HibernateDatastore createDatastore(String databaseName, Closure defaultMapping, Class... entities) {
        new HibernateDatastore(DatastoreUtils.createPropertyResolver(
                (Settings.SETTING_DB_CREATE): 'create-drop',
                'dataSource.url': "jdbc:h2:mem:${databaseName};LOCK_TIMEOUT=10000",
                'grails.gorm.default.mapping': defaultMapping
        ), entities)
    }

    private static Component componentOf(HibernateDatastore datastore, Class entity, String property) {
        datastore.metadata.getEntityBinding(entity.name).getProperty(property).value as Component
    }

    private static List<Column> columnsOf(Component component) {
        component.properties.collect { it.value.columns.first() as Column }
    }

    private static List<String> columnNamesOf(Component component) {
        columnsOf(component)*.name
    }

    private static List<String> tableColumnNames(HibernateDatastore datastore, Class entity) {
        datastore.metadata.getEntityBinding(entity.name).table.columns*.name
    }

    private static Map<String, Object> storedRow(HibernateDatastore datastore, Long id) {
        Map<String, Object> row = [:]
        datastore.dataSource.connection.withCloseable { conn ->
            conn.prepareStatement('select amount, currency, fee_amount, fee_currency from composite_user_type_payment where id = ?')
                    .withCloseable { stmt ->
                        stmt.setLong(1, id)
                        stmt.executeQuery().withCloseable { rs ->
                            assert rs.next()
                            ['amount', 'currency', 'fee_amount', 'fee_currency'].each { String column ->
                                Object value = rs.getObject(column)
                                row[column] = value instanceof Number ? ((Number) value).longValue() : value
                            }
                        }
                    }
        }
        row
    }

    private static Map<String, Object> schemaColumn(HibernateDatastore datastore, String table, String column) {
        Map<String, Object> description = [:]
        datastore.dataSource.connection.withCloseable { conn ->
            conn.prepareStatement('select data_type, character_maximum_length, is_nullable from information_schema.columns ' +
                    'where table_name = ? and column_name = ?').withCloseable { stmt ->
                stmt.setString(1, table)
                stmt.setString(2, column)
                stmt.executeQuery().withCloseable { rs ->
                    assert rs.next()
                    description.type = rs.getString('data_type')
                    description.length = rs.getObject('character_maximum_length')?.with { ((Number) it).intValue() }
                    description.nullable = rs.getString('is_nullable')
                }
            }
        }
        description
    }

    private static List<Throwable> causeChain(Throwable throwable) {
        List<Throwable> chain = []
        for (Throwable t = throwable; t != null && !chain.contains(t); t = t.cause) {
            chain << t
        }
        chain
    }
}

@Entity
class CompositeUserTypePayment implements HibernateEntity<CompositeUserTypePayment> {
    String reference
    CompositeMoney amount
    CompositeMoney fee

    static constraints = {
        amount nullable: false
        fee nullable: true
    }

    static mapping = {
        amount type: CompositeMoneyType, {
            column name: 'amount'
            column name: 'currency', sqlType: 'char(3)'
        }
        fee type: CompositeMoneyType, {
            column name: 'fee_amount'
            column name: 'fee_currency'
        }
    }
}

@Entity
class CompositeUserTypeDefaultColumns implements HibernateEntity<CompositeUserTypeDefaultColumns> {
    CompositeMoney amount

    static mapping = {
        amount type: CompositeMoneyType
    }
}

@Entity
class CompositeUserTypeRegistered implements HibernateEntity<CompositeUserTypeRegistered> {
    CompositeMoney amount

    static mapping = {
        amount {
            column name: 'money_value'
            column name: 'money_currency'
        }
    }
}

@Entity
class CompositeUserTypeTemplated implements HibernateEntity<CompositeUserTypeTemplated> {
    CompositeMoney amount

    static mapping = {
        amount type: CompositeMoneyTemplateType
    }
}

@Entity
class CompositeUserTypeImmutable implements HibernateEntity<CompositeUserTypeImmutable> {
    CompositeImmutableMoney price

    static mapping = {
        price type: CompositeImmutableMoneyType
    }
}

@Entity
class CompositeUserTypeFieldsTemplated implements HibernateEntity<CompositeUserTypeFieldsTemplated> {
    CompositeMoney amount

    static mapping = {
        amount type: CompositeMoneyFieldsTemplateType
    }
}

@Entity
class CompositeUserTypeTooManyColumns implements HibernateEntity<CompositeUserTypeTooManyColumns> {
    CompositeMoney amount

    static mapping = {
        amount type: CompositeMoneyType, {
            column name: 'first'
            column name: 'second'
            column name: 'third'
        }
    }
}

@CompileStatic
@EqualsAndHashCode
@ToString(includeNames = true)
class CompositeMoney implements Serializable {
    long amount
    String currency
}

/** The embeddable class of {@link CompositeMoneyTemplateType}: a source of attribute metadata only. */
record CompositeMoneyTemplate(long amount, String currency) {}

/**
 * The embeddable class of {@link CompositeMoneyFieldsTemplateType}: private fields without accessors, as in the
 * {@link CompositeUserType} javadoc.
 */
@CompileStatic
class CompositeMoneyFields {
    private long amount
    private String currency
}

/**
 * An immutable value class: final fields read through getters, without setters, and a computed property that
 * is listed in {@code transients} so that it is not a column.
 */
@CompileStatic
@Immutable
class CompositeImmutableMoney implements Serializable {
    long amount
    String currency

    static transients = ['display']

    String getDisplay() {
        "${amount} ${currency}"
    }
}

/**
 * Maps {@link CompositeMoney} to two columns. The attribute indexes follow the alphabetical order of
 * the embeddable class attributes: {@code amount} is 0 and {@code currency} is 1.
 */
@CompileStatic
class CompositeMoneyType implements CompositeUserType<CompositeMoney> {

    @Override
    Object getPropertyValue(CompositeMoney component, int property) {
        switch (property) {
            case 0:
                return component.amount
            case 1:
                return component.currency
            default:
                throw new HibernateException("Unknown property index ${property}")
        }
    }

    @Override
    CompositeMoney instantiate(ValueAccess values) {
        Long amount = values.getValue(0, Long)
        String currency = values.getValue(1, String)
        if (amount == null && currency == null) {
            return null
        }
        new CompositeMoney(amount: amount, currency: currency)
    }

    @Override
    Class<?> embeddable() {
        CompositeMoney
    }

    @Override
    Class<CompositeMoney> returnedClass() {
        CompositeMoney
    }

    @Override
    boolean equals(CompositeMoney x, CompositeMoney y) {
        x == y
    }

    @Override
    int hashCode(CompositeMoney x) {
        x == null ? 0 : x.hashCode()
    }

    @Override
    CompositeMoney deepCopy(CompositeMoney value) {
        value
    }

    @Override
    boolean isMutable() {
        false
    }

    @Override
    Serializable disassemble(CompositeMoney value) {
        value
    }

    @Override
    CompositeMoney assemble(Serializable cached, Object owner) {
        (CompositeMoney) cached
    }

    @Override
    CompositeMoney replace(CompositeMoney detached, CompositeMoney managed, Object owner) {
        detached
    }
}

/** The same mapping, described by a record rather than by the value class itself. */
@CompileStatic
class CompositeMoneyTemplateType extends CompositeMoneyType {

    @Override
    Class<?> embeddable() {
        CompositeMoneyTemplate
    }
}

/** The same mapping, described by a class with private fields and no accessors. */
@CompileStatic
class CompositeMoneyFieldsTemplateType extends CompositeMoneyType {

    @Override
    Class<?> embeddable() {
        CompositeMoneyFields
    }
}

/** Maps {@link CompositeImmutableMoney} to two columns: {@code amount} is index 0 and {@code currency} is 1. */
@CompileStatic
class CompositeImmutableMoneyType implements CompositeUserType<CompositeImmutableMoney> {

    @Override
    Object getPropertyValue(CompositeImmutableMoney component, int property) {
        switch (property) {
            case 0:
                return component.amount
            case 1:
                return component.currency
            default:
                throw new HibernateException("Unknown property index ${property}")
        }
    }

    @Override
    CompositeImmutableMoney instantiate(ValueAccess values) {
        Long amount = values.getValue(0, Long)
        String currency = values.getValue(1, String)
        if (amount == null && currency == null) {
            return null
        }
        new CompositeImmutableMoney(amount, currency)
    }

    @Override
    Class<?> embeddable() {
        CompositeImmutableMoney
    }

    @Override
    Class<CompositeImmutableMoney> returnedClass() {
        CompositeImmutableMoney
    }

    @Override
    boolean equals(CompositeImmutableMoney x, CompositeImmutableMoney y) {
        x == y
    }

    @Override
    int hashCode(CompositeImmutableMoney x) {
        x == null ? 0 : x.hashCode()
    }

    @Override
    CompositeImmutableMoney deepCopy(CompositeImmutableMoney value) {
        value
    }

    @Override
    boolean isMutable() {
        false
    }

    @Override
    Serializable disassemble(CompositeImmutableMoney value) {
        value
    }

    @Override
    CompositeImmutableMoney assemble(Serializable cached, Object owner) {
        (CompositeImmutableMoney) cached
    }

    @Override
    CompositeImmutableMoney replace(CompositeImmutableMoney detached, CompositeImmutableMoney managed, Object owner) {
        detached
    }
}
