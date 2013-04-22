package org.grails.databinding.converters;

public interface FormattedValueConverter {
    Object convert(Object value, String format);
    Class<?> getTargetType();
}
