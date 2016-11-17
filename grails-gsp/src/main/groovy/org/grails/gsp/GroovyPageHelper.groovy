/*
 * Copyright 2016 the original author or authors.
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

package org.grails.gsp

import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import org.grails.taglib.TagLibraryLookup
import org.grails.taglib.TagOutput
import org.grails.taglib.encoder.OutputContext

@CompileStatic
class GroovyPageHelper {
    private final TagLibraryLookup tagLibraryLookup
    private final OutputContext outputContext

    public GroovyPageHelper(TagLibraryLookup tagLibraryLookup, OutputContext outputContext) {
        this.tagLibraryLookup = tagLibraryLookup
        this.outputContext = outputContext
    }

    private Object invokeTag(String name, Map attrs, Object body = null) {
        TagOutput.captureTagOutput(tagLibraryLookup, 'g', name, attrs, body, outputContext)
    }

    private CharSequence invokeTagReturningChars(String name, Map attrs, Object body = null) {
        CharSequence.cast(invokeTag(name, attrs, body))
    }

    CharSequence message(Map attrs) {
        invokeTagReturningChars('message', attrs)
    }

    CharSequence resource(Map attrs) {
        invokeTagReturningChars('resource', attrs)
    }

    CharSequence external(Map attrs) {
        invokeTagReturningChars('external', attrs)
    }

    CharSequence img(Map attrs) {
        invokeTagReturningChars('img', attrs)
    }

    CharSequence link(Map attrs) {
        invokeTagReturningChars('link', attrs)
    }

    CharSequence createLink(Map attrs) {
        invokeTagReturningChars('createLink', attrs)
    }

    CharSequence meta(Map attrs) {
        invokeTagReturningChars('meta', attrs)
    }

    CharSequence render(Map attrs) {
        invokeTagReturningChars('render', attrs)
    }

    CharSequence fieldError(Map attrs) {
        invokeTagReturningChars('fieldError', attrs)
    }

    CharSequence formatValue(Map attrs) {
        invokeTagReturningChars('formatValue', attrs)
    }

    CharSequence formatNumber(Map attrs) {
        invokeTagReturningChars('formatNumber', attrs)
    }

    CharSequence formatDate(Map attrs) {
        invokeTagReturningChars('formatDate', attrs)
    }

    CharSequence formatBoolean(Map attrs) {
        invokeTagReturningChars('formatBoolean', attrs)
    }

    CharSequence encodeAs(Map attrs, Object body) {
        invokeTagReturningChars('encodeAs', attrs, body)
    }

    CharSequence hasErrors(Map attrs, Object body) {
        invokeTagReturningChars('hasErrors', attrs, body)
    }
}
