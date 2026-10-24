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
package grails.gorm.tests

import grails.gorm.annotation.Entity

/**
 * Two unidirectional hasMany associations that map to one join table with the same key column and a column of their
 * own for the element: the rows of an association are the rows that have its element column. Hibernate deletes all
 * the rows of a collection with one statement on its key, so clearing one association must not take the rows of the
 * other with it.
 */
class SharedJoinTableBulkDeleteSpec extends HibernateGormDatastoreSpec {

    void setupSpec() {
        manager.registerDomainClasses(SjtbdAccount, SjtbdRole)
    }

    void "clearing one association of a shared join table leaves the rows of the other"() {
        given: 'an account with a role in each association'
        Map<String, Long> ids = SjtbdAccount.withNewTransaction {
            SjtbdRole admin = new SjtbdRole(name: 'admin').save(flush: true)
            SjtbdRole guest = new SjtbdRole(name: 'guest').save(flush: true)
            SjtbdAccount account = new SjtbdAccount(name: 'account')
            account.addToRoles(admin)
            account.addToDenyRoles(guest)
            [account: account.save(flush: true).id]
        }

        when: 'the roles are cleared'
        SjtbdAccount.withNewSession {
            SjtbdAccount.withNewTransaction {
                SjtbdAccount account = SjtbdAccount.get(ids.account)
                account.roles.clear()
                account.save(flush: true)
            }
        }

        then: 'the denied roles are still there'
        SjtbdAccount.withNewSession {
            SjtbdAccount.withNewTransaction {
                SjtbdAccount account = SjtbdAccount.get(ids.account)
                account.roles.isEmpty() && account.denyRoles*.name == ['guest']
            }
        }

        when: 'the denied roles are cleared after a role is added back'
        SjtbdAccount.withNewSession {
            SjtbdAccount.withNewTransaction {
                SjtbdAccount account = SjtbdAccount.get(ids.account)
                account.addToRoles(SjtbdRole.findByName('admin'))
                account.denyRoles.clear()
                account.save(flush: true)
            }
        }

        then: 'the roles are still there'
        SjtbdAccount.withNewSession {
            SjtbdAccount.withNewTransaction {
                SjtbdAccount account = SjtbdAccount.get(ids.account)
                account.denyRoles.isEmpty() && account.roles*.name == ['admin']
            }
        }
    }
}

@Entity
class SjtbdAccount {
    Long id
    String name
    static hasMany = [roles: SjtbdRole, denyRoles: SjtbdRole]
    static mapping = {
        version false
        roles joinTable: [name: 'sjtbd_account_roles', key: 'account_id', column: 'role_id']
        denyRoles joinTable: [name: 'sjtbd_account_roles', key: 'account_id', column: 'deny_role_id']
    }
}

@Entity
class SjtbdRole {
    Long id
    String name
}
