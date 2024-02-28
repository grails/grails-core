/*
 * Copyright 2024 the original author or authors.
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
package org.grails.databinding.converters

import grails.databinding.converters.ValueConverter
import groovy.transform.CompileStatic

import java.time.LocalDateTime

/**
 * This class is a {@link ValueConverter} for {@link LocalDateTime} target type.
 *
 * @author Emma Richardson
 * @since 6.2.0
 */
@CompileStatic
class LocalDateTimeConverter implements ValueConverter {
    @Override
    boolean canConvert(Object value) {
        return value instanceof String || value instanceof LocalDateTime
    }

    @Override
    Object convert(Object value) {
        if (value instanceof LocalDateTime) {
            return value
        } else if (value instanceof CharSequence) {
            String dateStr = value.toString()
            if (!dateStr) {
                return null
            } else {
                LocalDateTime.parse((String) value)
            }
        }
    }

    @Override
    Class<?> getTargetType() {
        return LocalDateTime
    }
}
