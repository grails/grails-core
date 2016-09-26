package org.grails.databinding.converters.web

import grails.databinding.TypedStructuredBindingEditor
import grails.databinding.converters.FormattedValueConverter
import grails.databinding.converters.ValueConverter
import org.grails.databinding.converters.AbstractStructuredDateBindingEditor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Configuration
class Jsr310ConvertersConfiguration {

    List<String> formatStrings = []

    @Bean
    FormattedValueConverter offsetDateTimeConverter() {
        new FormattedValueConverter() {
            @Override
            Object convert(Object value, String format) {
                OffsetDateTime.parse((CharSequence)value, DateTimeFormatter.ofPattern(format))
            }

            @Override
            Class<?> getTargetType() {
                OffsetDateTime
            }
        }
    }

    @Bean
    ValueConverter offsetDateTimeValueConverter() {
        new Jsr310DateValueConverter<OffsetDateTime>() {
            @Override
            OffsetDateTime convert(Object value) {
                convert(value) { String format ->
                    OffsetDateTime.parse((CharSequence)value, DateTimeFormatter.ofPattern(format))
                }
            }

            @Override
            Class<?> getTargetType() {
                OffsetDateTime
            }
        }
    }

    @Bean
    TypedStructuredBindingEditor offsetDateTimeStructuredBindingEditor() {
        new CustomDateBindingEditor<OffsetDateTime>() {
            @Override
            OffsetDateTime getDate(Calendar c) {
                OffsetDateTime.ofInstant(c.toInstant(), c.timeZone.toZoneId())
            }

            @Override
            Class<?> getTargetType() {
                OffsetDateTime
            }
        }
    }

    @Bean
    FormattedValueConverter offsetTimeConverter() {
        new FormattedValueConverter() {
            @Override
            Object convert(Object value, String format) {
                OffsetTime.parse((CharSequence) value, DateTimeFormatter.ofPattern(format))
            }

            @Override
            Class<?> getTargetType() {
                OffsetTime
            }
        }
    }

    @Bean
    ValueConverter offsetTimeValueConverter() {
        new Jsr310DateValueConverter<OffsetTime>() {
            @Override
            OffsetTime convert(Object value) {
                convert(value) { String format ->
                    OffsetTime.parse((CharSequence)value, DateTimeFormatter.ofPattern(format))
                }
            }

            @Override
            Class<?> getTargetType() {
                OffsetTime
            }
        }
    }

    @Bean
    TypedStructuredBindingEditor offsetTimeStructuredBindingEditor() {
        new CustomDateBindingEditor<OffsetTime>() {
            @Override
            OffsetTime getDate(Calendar c) {
                OffsetTime.ofInstant(c.toInstant(), c.timeZone.toZoneId())
            }

            @Override
            Class<?> getTargetType() {
                OffsetTime
            }
        }
    }

    @Bean
    FormattedValueConverter localDateTimeConverter() {
        new FormattedValueConverter() {
            @Override
            Object convert(Object value, String format) {
                LocalDateTime.parse((CharSequence) value, DateTimeFormatter.ofPattern(format))
            }

            @Override
            Class<?> getTargetType() {
                LocalDateTime
            }
        }
    }

    @Bean
    ValueConverter localDateTimeValueConverter() {
        new Jsr310DateValueConverter<LocalDateTime>() {
            @Override
            LocalDateTime convert(Object value) {
                convert(value) { String format ->
                    LocalDateTime.parse((CharSequence)value, DateTimeFormatter.ofPattern(format))
                }
            }

            @Override
            Class<?> getTargetType() {
                LocalDateTime
            }
        }
    }

    @Bean
    TypedStructuredBindingEditor localDateTimeStructuredBindingEditor() {
        new CustomDateBindingEditor<LocalDateTime>() {
            @Override
            LocalDateTime getDate(Calendar c) {
                LocalDateTime.ofInstant(c.toInstant(), c.timeZone.toZoneId())
            }

            @Override
            Class<?> getTargetType() {
                LocalDateTime
            }
        }
    }

    @Bean
    FormattedValueConverter localDateConverter() {
        new FormattedValueConverter() {
            @Override
            Object convert(Object value, String format) {
                LocalDate.parse((CharSequence) value, DateTimeFormatter.ofPattern(format))
            }

            @Override
            Class<?> getTargetType() {
                LocalDate
            }
        }
    }

    @Bean
    ValueConverter localDateValueConverter() {
        new Jsr310DateValueConverter<LocalDate>() {
            @Override
            LocalDate convert(Object value) {
                convert(value) { String format ->
                    LocalDate.parse((CharSequence)value, DateTimeFormatter.ofPattern(format))
                }
            }

            @Override
            Class<?> getTargetType() {
                LocalDate
            }
        }
    }

    @Bean
    TypedStructuredBindingEditor localDateStructuredBindingEditor() {
        new CustomDateBindingEditor<LocalDate>() {
            @Override
            LocalDate getDate(Calendar c) {
                LocalDate.of(c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH))
            }

            @Override
            Class<?> getTargetType() {
                LocalDate
            }
        }
    }

    @Bean
    FormattedValueConverter localTimeConverter() {
        new FormattedValueConverter() {
            @Override
            Object convert(Object value, String format) {
                LocalTime.parse((CharSequence) value, DateTimeFormatter.ofPattern(format))
            }

            @Override
            Class<?> getTargetType() {
                LocalTime
            }
        }
    }

    @Bean
    ValueConverter localTimeValueConverter() {
        new Jsr310DateValueConverter<LocalTime>() {
            @Override
            LocalTime convert(Object value) {
                convert(value) { String format ->
                    LocalTime.parse((CharSequence)value, DateTimeFormatter.ofPattern(format))
                }
            }

            @Override
            Class<?> getTargetType() {
                LocalTime
            }
        }
    }

    @Bean
    TypedStructuredBindingEditor localTimeStructuredBindingEditor() {
        new CustomDateBindingEditor<LocalTime>() {
            @Override
            LocalTime getDate(Calendar c) {
                LocalTime.of(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND))
            }

            @Override
            Class<?> getTargetType() {
                LocalTime
            }
        }
    }

    @Bean
    FormattedValueConverter zonedDateTimeConverter() {
        new FormattedValueConverter() {
            @Override
            Object convert(Object value, String format) {
                ZonedDateTime.parse((CharSequence)value, DateTimeFormatter.ofPattern(format))
            }

            @Override
            Class<?> getTargetType() {
                ZonedDateTime
            }
        }
    }

    @Bean
    ValueConverter zonedDateTimeValueConverter() {
        new Jsr310DateValueConverter<ZonedDateTime>() {
            @Override
            ZonedDateTime convert(Object value) {
                convert(value) { String format ->
                    ZonedDateTime.parse((CharSequence)value, DateTimeFormatter.ofPattern(format))
                }
            }

            @Override
            Class<?> getTargetType() {
                ZonedDateTime
            }
        }
    }

    @Bean
    TypedStructuredBindingEditor zonedDateTimeStructuredBindingEditor() {
        new CustomDateBindingEditor<ZonedDateTime>() {
            @Override
            ZonedDateTime getDate(Calendar c) {
                ZonedDateTime.ofInstant(c.toInstant(), c.timeZone.toZoneId())
            }

            @Override
            Class<?> getTargetType() {
                ZonedDateTime
            }
        }
    }

    abstract class Jsr310DateValueConverter<T> implements ValueConverter {

        @Override
        boolean canConvert(Object value) {
            value instanceof String
        }

        T convert(Object value, Closure callable) {
            T dateValue
            if (value instanceof String) {
                if(!value) {
                    return null
                }
                def firstException
                formatStrings.each { String format ->
                    if (dateValue == null) {
                        try {
                            dateValue = (T)callable.call(format)
                        } catch (Exception e) {
                            firstException = firstException ?: e
                        }
                    }
                }
                if(dateValue == null && firstException) {
                    throw firstException
                }
            }
            dateValue
        }

        @Override
        abstract Class<?> getTargetType()
    }

    abstract class CustomDateBindingEditor<T> extends AbstractStructuredDateBindingEditor<T> implements TypedStructuredBindingEditor<T> {

    }
}
