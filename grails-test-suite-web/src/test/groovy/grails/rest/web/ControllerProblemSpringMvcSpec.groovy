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
package grails.rest.web

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

import grails.artefact.Artefact
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

/**
 * respond writes a problem with the handler Spring MVC's handler adapter writes a {@code ResponseEntity} with, so the
 * application's {@code ResponseBodyAdvice} and {@code ErrorResponse.Interceptor} beans apply to it as to a handler method
 * of the controller.
 */
class ControllerProblemSpringMvcSpec extends Specification implements ControllerUnitTest<ProblemAdviceController> {

    Closure doWithConfig() {
        { config -> config['grails.web.rendering.json.spring'] = true }
    }

    @Configuration
    static class MvcConfiguration {

        @Bean
        ProblemBodyAdvice problemBodyAdvice() {
            new ProblemBodyAdvice()
        }

        @Bean
        OtherControllerAdvice otherControllerAdvice() {
            new OtherControllerAdvice()
        }

        @Bean
        RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
            new RequestMappingHandlerAdapter().tap {
                messageConverters = [new JacksonJsonHttpMessageConverter()] as List<HttpMessageConverter<?>>
                errorResponseInterceptors = [new RecordingInterceptor()] as List<ErrorResponse.Interceptor>
            }
        }
    }

    void 'the ResponseBodyAdvice of the application applies to a problem respond writes, as to a handler method of the controller'() {
        when:
        controller.respond(ProblemDetail.forStatus(HttpStatus.CONFLICT))

        then:
        response.status == 409
        response.contentType == 'application/problem+json'
        response.json.advised == ProblemAdviceController.name
    }

    void 'advice for other controllers does not apply to a problem respond writes'() {
        when:
        controller.respond(ProblemDetail.forStatus(HttpStatus.CONFLICT))

        then:
        !response.json.containsKey('other')
    }

    void 'the ErrorResponse interceptors of the handler adapter see a problem respond writes'() {
        when:
        controller.respond(ProblemDetail.forStatus(HttpStatus.CONFLICT))

        then:
        response.json.intercepted == true
    }

    void 'validation errors written as a problem pass through the same advice'() {
        given:
        response.format = 'json'
        def errors = new BeanPropertyBindingResult(new Object(), 'book')
        errors.reject('invalid', 'Book is invalid')

        when:
        controller.respond(errors)

        then:
        response.status == 422
        response.json.advised == ProblemAdviceController.name
        response.json.intercepted == true
    }
}

@Artefact('Controller')
class ProblemAdviceController {
}

@Artefact('Controller')
class OtherAdvisedController {
}

/** Advice for the controller under test, which adds the name of the class it is applied for. */
@ControllerAdvice(assignableTypes = ProblemAdviceController)
class ProblemBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        true
    }

    @Override
    Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                           Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
                           ServerHttpResponse response) {
        if (body instanceof ProblemDetail) {
            ((ProblemDetail) body).setProperty('advised', returnType.containingClass.name)
        }
        body
    }
}

/** Advice for another controller, which a problem the controller under test responds with does not get. */
@ControllerAdvice(assignableTypes = OtherAdvisedController)
class OtherControllerAdvice implements ResponseBodyAdvice<Object> {

    @Override
    boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        true
    }

    @Override
    Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                           Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
                           ServerHttpResponse response) {
        if (body instanceof ProblemDetail) {
            ((ProblemDetail) body).setProperty('other', true)
        }
        body
    }
}

class RecordingInterceptor implements ErrorResponse.Interceptor {

    @Override
    void handleError(ProblemDetail detail, ErrorResponse errorResponse) {
        detail.setProperty('intercepted', true)
    }
}
