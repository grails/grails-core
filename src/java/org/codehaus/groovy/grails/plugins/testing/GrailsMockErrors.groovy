/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.plugins.testing

import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.springframework.validation.BeanPropertyBindingResult

/**
 * A simple Spring Errors implementation that adds support for Groovy-
 * style access to field errors. For example, if you have a mocked
 * domain instance "dc", then you can get the error code for a field
 * "name" by using this code:
 * <pre>
 *   dc.errors["name"]
 * </pre>
 */
class GrailsMockErrors extends BeanPropertyBindingResult {

    static final ERROR_CODE_TABLE = Collections.unmodifiableMap([
        "blank":               "blank",
        "creditCard.invalid":  "creditCard",
        "email.invalid":       "email",
        "matches.invalid":     "matches",
        "max.exceeded":        "max",
        "maxSize.exceeded":    "maxSize",
        "min.notmet":          "min",
        "minSize.notmet":      "minSize",
        "not.inList":          "inList",
        "notEqual":            "notEqual",
        "nullable":            "nullable",
        "range.toobig":        "range",
        "range.toosmall":      "range",
        "size.toobig":         "size",
        "size.toosmall":       "size",
        "url.invalid":         "url",
        "validator.invalid":   "validator" ])

    GrailsMockErrors(instance) {
        super(instance, instance.getClass().name)
    }

    def propertyMissing(String name) {
        def code = getFieldError(name)?.code
        return ERROR_CODE_TABLE[code] ?: code
    }

    def isEmpty() {
        !hasErrors()
    }
}
