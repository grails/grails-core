/*
 * Copyright 2024 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.web.binding

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification


/**
 * @author Graeme Rocher
 * @since 1.1
 */
class BindToEnumTests extends Specification implements ControllerUnitTest<EnumBindingController>, DomainUnitTest<RoleHolder> {

    void testBindBlankValueToEnum() {
        params.role = ""

        when:
        def model = controller.save()

        then:
        model.holder.role == null
    }

    void testBindValueToEnum() {
        params.role = "USER"

        when:
        def model = controller.save()

        then:
        model.holder.role.toString() == "USER"
    }
}

@Entity
class RoleHolder {

    EnumRole role

    static constraints = {
        role nullable:true
    }
}

enum EnumRole {
    USER, ADMINISTRATOR, EDITOR
}

@Artefact('Controller')
class EnumBindingController {

    def save = {
        def h = new RoleHolder(params)
        [holder:h]
    }
}

