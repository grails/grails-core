/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.domain

import org.grails.datastore.gorm.GormValidationApi
import org.springframework.datastore.mapping.core.Datastore
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.validation.Validator

 /**
 * Helper class used in the case where there is not GORM API installed in the application to provide
 * basic validation facility
 *
 * @author Graeme Rocher
 * @since 1.4
 */
class GormApiSupport {

    static GormValidationApi getGormValidationApi(Datastore datastore, Class cls, Validator validator) {
        def ctx = datastore.mappingContext
        PersistentEntity entity = ctx.getPersistentEntity(cls.getName())

        if (entity == null) {
            entity = ctx.addPersistentEntity(cls)
        }

        ctx.addEntityValidator(entity, validator)

        return new GormValidationApi(cls, datastore)
    }
}
