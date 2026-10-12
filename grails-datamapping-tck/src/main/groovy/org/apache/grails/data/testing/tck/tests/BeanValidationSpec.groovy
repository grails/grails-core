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

package org.apache.grails.data.testing.tck.tests

import spock.lang.IgnoreIf

import grails.gorm.validation.ConstrainedEntity
import org.apache.grails.data.testing.tck.base.GrailsDataTckSpec
import org.apache.grails.data.testing.tck.domains.BeanValidatedArticle
import org.apache.grails.data.testing.tck.domains.BeanValidatedInvoice
import org.apache.grails.data.testing.tck.domains.BeanValidatedInvoiceLine
import org.apache.grails.data.testing.tck.domains.BeanValidatedPair
import org.apache.grails.data.testing.tck.domains.BeanValidatedParcel
import org.apache.grails.data.testing.tck.domains.BeanValidatedReview
import org.apache.grails.data.testing.tck.domains.BeanValidatedShipment

/**
 * A domain class annotated with Bean Validation constraints is validated against its annotations and its
 * constraints block, cascades like any other domain class, and exposes both as constraints.
 */
@IgnoreIf({ Boolean.getBoolean('simple.gorm.suite') }) // the simple datastore of the core suite registers no validator
class BeanValidationSpec extends GrailsDataTckSpec {

    void setupSpec() {
        manager.registerDomainClasses(BeanValidatedInvoice, BeanValidatedInvoiceLine, BeanValidatedShipment, BeanValidatedParcel,
                BeanValidatedReview, BeanValidatedPair, BeanValidatedArticle)
    }

    void 'the annotations and the constraints block are validated, each violation once'() {
        given: 'an instance violating an annotation, a null annotated property and the constraints block'
        def invoice = new BeanValidatedInvoice(code: 'toolong')

        expect:
        !invoice.validate()
        invoice.errors.errorCount == 3
        invoice.errors.getFieldError('reference').code == 'NotNull'
        invoice.errors.getFieldError('code').code == 'Size'
        invoice.errors.getFieldError('note').code == 'nullable'
        invoice.save() == null

        when: 'the instance is valid'
        invoice = new BeanValidatedInvoice(reference: 'INV-1', code: 'ok', note: 'paid')

        then:
        invoice.validate()
        invoice.save(flush: true) != null
        BeanValidatedInvoice.count() == 1
    }

    void 'the constraints of an annotated domain class describe its annotations and its constraints block'() {
        given:
        def entity = BeanValidatedInvoice.gormPersistentEntity
        def validator = entity.mappingContext.getEntityValidator(entity)

        expect:
        validator instanceof ConstrainedEntity
        def constraints = ((ConstrainedEntity) validator).constrainedProperties
        !constraints.reference.nullable
        constraints.code.maxSize == 5
        !constraints.note.nullable
    }

    void 'validation cascades to an annotated association, checking its annotations and its constraints block once'() {
        given: 'a valid invoice with a line violating an annotation and the constraints block'
        def invoice = new BeanValidatedInvoice(reference: 'INV-1', code: 'ok', note: 'paid')
        invoice.addToLines(new BeanValidatedInvoiceLine(label: '', sku: null))

        expect:
        !invoice.validate()
        invoice.errors.errorCount == 2
        invoice.errors.getFieldError('lines[0].label').code == 'NotBlank'
        invoice.errors.getFieldError('lines[0].sku').code == 'nullable'
        invoice.save() == null

        and: 'deepValidate: false leaves the line out'
        invoice.validate(deepValidate: false)
    }

    void 'a domain class without annotations cascades to an annotated association'() {
        given:
        def shipment = new BeanValidatedShipment(carrier: 'post')
        shipment.addToParcels(new BeanValidatedParcel(label: ''))

        expect:
        !shipment.validate()
        shipment.errors.errorCount == 1
        shipment.errors.getFieldError('parcels[0].label').code == 'NotBlank'
    }

    void 'an annotation of another group than the default one leaves the constraints block in force'() {
        given:
        def review = new BeanValidatedReview()
        def entity = BeanValidatedReview.gormPersistentEntity
        def constraints = ((ConstrainedEntity) entity.mappingContext.getEntityValidator(entity)).constrainedProperties

        expect: 'validate() does not run the group, so the block reports the null'
        !review.validate()
        review.errors.errorCount == 1
        review.errors.getFieldError('author').code == 'nullable'
        review.save() == null

        and: 'the constraints say what is enforced'
        !constraints.author.nullable
    }

    void 'an annotation of a group extended by a group of the sequence redefining the default group is validated and described'() {
        given:
        def article = new BeanValidatedArticle(slug: 'toolong')
        def entity = BeanValidatedArticle.gormPersistentEntity
        def constraints = ((ConstrainedEntity) entity.mappingContext.getEntityValidator(entity)).constrainedProperties

        expect: 'validate() runs the extended group, so the annotation alone reports the null'
        !article.validate()
        article.errors.errorCount == 2
        article.errors.getFieldError('title').code == 'NotNull'
        article.errors.getFieldError('slug').code == 'Size'
        article.save() == null

        and: 'the constraints describe the annotations'
        !constraints.title.nullable
        constraints.slug.maxSize == 5

        when: 'the instance is valid'
        article = new BeanValidatedArticle(title: 'Groups', slug: 'ok')

        then:
        article.validate()
        article.save(flush: true) != null
    }

    void 'a class level constraint is validated'() {
        given:
        def pair = new BeanValidatedPair(first: 'same', second: 'same')

        expect:
        !pair.validate()
        pair.errors.errorCount == 1
        pair.errors.globalErrors[0].code == 'DistinctLabels'
    }
}
