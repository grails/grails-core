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
package org.codehaus.groovy.grails.web.binding

import groovy.transform.CompileStatic

import org.grails.databinding.errors.BindingError
import org.grails.databinding.events.DataBindingListenerAdapter
import org.springframework.context.support.DefaultMessageSourceResolvable
import org.springframework.validation.BindingResult
import org.springframework.validation.Errors
import org.springframework.validation.FieldError

@CompileStatic
class GrailsWebDataBindingListener extends DataBindingListenerAdapter {
    private final BindingResult bindingResult

    GrailsWebDataBindingListener(BindingResult bindingResult) {
        this.bindingResult = bindingResult
    }

    @Override
    void bindingError(BindingError error) {
        Object[] o = getArgumentsForBindError(error.object?.getClass()?.getName(), error.getPropertyName())
        def codes = bindingResult.resolveMessageCodes('typeMismatch', error.getPropertyName())
        def cause = error.cause
        def defaultMessage = cause ? cause.message : 'Data Binding Failed'
        def fieldError = new FieldError(error.object?.getClass()?.getName(), error.getPropertyName(), error.getRejectedValue(), true, codes as String[], o, defaultMessage)
        bindingResult.addError(fieldError)
    }

    protected Object[] getArgumentsForBindError(String objectName, String field) {
        def codes = [
            objectName + Errors.NESTED_PATH_SEPARATOR + field,
            field] as String[]
        [new DefaultMessageSourceResolvable(codes, field)] as Object[]
    }
}
