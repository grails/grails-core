package org.grails.databinding.converters;

public interface ConversionService {
    boolean canConvert(Class<?> source, Class<?> target);
    Object convert(Object objet, Class<?> targetType);
}
