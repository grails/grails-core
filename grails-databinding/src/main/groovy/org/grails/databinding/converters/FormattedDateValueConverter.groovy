package org.grails.databinding.converters

import java.text.SimpleDateFormat

class FormattedDateValueConverter implements FormattedValueConverter {

    def convert(value, String format) {
        new SimpleDateFormat(format).parse((String)value)
    }

    Class<?> getTargetType() {
        Date
    }

}
