/*
 * Copyright 2013 the original author or authors.
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

import java.text.SimpleDateFormat

/**
 * @author Jeff Brown
 * @since 2.3
 */
class DateConversionHelper implements ValueConverter {

    Object convert(value) {
        Date dateValue
        if (value instanceof String) {
            // TODO - fix this...
            def formatStrings = ['yyyy-MM-dd HH:mm:ss.S',"yyyy-MM-dd'T'hh:mm:ss'Z'"]
            def firstException
            formatStrings.each { format ->
                if (dateValue == null) {
                    def formatter = new SimpleDateFormat(format)
                    try {
                        dateValue = formatter.parse(value)
                    } catch (Exception e) {
                        firstException = firstException ?: e
                    }
                }
            }
            if(dateValue == null && firstException) {
                throw firstException
            }
        } else {
            dateValue = new Date(value)
        }
        dateValue
    }

    Class<?> getTargetType() {
        Date
    }

    public boolean canConvert(Object value) {
        value instanceof String
    }
}
