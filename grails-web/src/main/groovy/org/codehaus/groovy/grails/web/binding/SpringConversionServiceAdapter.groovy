package org.codehaus.groovy.grails.web.binding

import org.grails.databinding.converters.ConversionService
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.core.convert.ConversionService as SpringConversionService

/**
 * This class implements org.grails.databinding.converters.ConversionService
 * and delegates to a org.springfraemwork core.convert.support.DefaultConversionService.
 *
 * @see org.grails.databinding.converters.ConversionService
 * @see org.springframework.core.convert.support.DefaultConversionService
 */
class SpringConversionServiceAdapter implements ConversionService {

    private SpringConversionService springConversionService = new DefaultConversionService()

    boolean canConvert(Class<?> source, Class<?> target) {
        springConversionService.canConvert source, target
    }

    def convert(Object object, Class<?> targetType) {
        springConversionService.convert object, targetType
    }
}
