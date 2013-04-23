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
 * @author Jeff Brown
 * @since 2.3
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
