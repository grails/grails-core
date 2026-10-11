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
package grails.gorm.tests.proxy.traitinitializer

import grails.gorm.annotation.Entity
import org.hibernate.Hibernate
import org.grails.orm.hibernate.HibernateDatastore
import spock.lang.AutoCleanup
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Specification

/**
 * Regression test for the construction of Hibernate proxies for Groovy entities whose trait
 * declares fields with default values.
 *
 * Groovy generates a static {@code $init$} helper for such traits and calls it from the entity
 * constructor. When Hibernate builds the lazy proxy, the proxy constructor runs while the
 * interceptor has not been assigned yet; the generated proxy intercepts {@code getMetaClass()}
 * (Groovy 3+ no longer marks it as synthetic, so Hibernate's synthetic-only filter does not
 * exclude it) and the trait initializer fails with a {@code NullPointerException}.
 *
 * {@code Domain.load()} must return a usable, uninitialized proxy for both entities.
 */
@Issue('https://github.com/apache/grails-core/issues/16600')
class TraitInitializerProxySpec extends Specification {

    @Shared @AutoCleanup HibernateDatastore datastore = new HibernateDatastore(getClass().getPackage())

    void "load() of an entity whose trait declares default values creates the proxy"() {
        given: "a persisted entity whose trait declares fields with default values"
        Long id = null
        InitializedTraitEntity.withTransaction {
            id = new InitializedTraitEntity(name: 'seed').save(flush: true, failOnError: true).id
        }

        when: "a lazy proxy is requested in a new session"
        InitializedTraitEntity proxy = null
        InitializedTraitEntity.withTransaction { proxy = InitializedTraitEntity.load(id) }

        then: "the proxy is built without triggering the trait initializer through the interceptor"
        noExceptionThrown()
        proxy != null
        !Hibernate.isInitialized(proxy)
    }

    void "control: load() of an entity whose trait declares no initializer creates the proxy"() {
        given: "a persisted entity whose trait declares a plain property"
        Long id = null
        PlainTraitEntity.withTransaction {
            id = new PlainTraitEntity(name: 'seed').save(flush: true, failOnError: true).id
        }

        when: "a lazy proxy is requested in a new session"
        PlainTraitEntity proxy = null
        PlainTraitEntity.withTransaction { proxy = PlainTraitEntity.load(id) }

        then: "the proxy is built"
        noExceptionThrown()
        proxy != null
        !Hibernate.isInitialized(proxy)
    }
}

trait InitializedTrait {
    String createdBy = 'N/A'
    String lastUpdatedBy = 'N/A'
}

@Entity
class InitializedTraitEntity implements InitializedTrait {
    String name
}

trait PlainTrait {
    String note
}

@Entity
class PlainTraitEntity implements PlainTrait {
    String name
}
