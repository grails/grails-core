package org.grails.databinding.converters

import java.text.SimpleDateFormat

class DateConversionHelper implements ValueConverter {

    public Object convert(Object value) {
        Date dateValue = null
        if(value instanceof String) {
            // TODO - fix this...
            def formatStrings = ['yyyy-MM-dd HH:mm:ss.S',"yyyy-MM-dd'T'hh:mm:ss'Z'"]
            def firstException
            formatStrings.each { format ->
                if(dateValue == null) {
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

}
