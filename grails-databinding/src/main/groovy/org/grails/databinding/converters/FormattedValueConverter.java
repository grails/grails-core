package org.grails.databinding.converters;

import org.grails.databinding.BindingFormat;

/**
 * Classes which implement this interface may participate
 * in the data binding process as formatted value converters.
<pre>
import org.grails.databinding.converters.FormattedValueConverter

class FormattedStringValueConverter implements FormattedValueConverter {
    def convert(value, String format) {
        if('UPPERCASE' == format) {
            value = value.toUpperCase()
        } else if('LOWERCASE' == format) {
            value = value.toLowerCase()
        }
        value
    }

    Class getTargetType() {
        // specifies the type to which this converter may be applied
        String
    }
}
</pre>
 *
 * @see BindingFormat
 * @see org.grails.databinding.SimpleDataBinder
 * @see org.grails.databinding.SimpleDataBinder#registerFormattedValueConverter(FormattedValueConverter)
 */
public interface FormattedValueConverter {
    /**
     * Return a formatted value
     * 
     * @param value The value to be formatted
     * @param format The format String
     * @return the formatted value
     */
    Object convert(Object value, String format);
    Class<?> getTargetType();
}
