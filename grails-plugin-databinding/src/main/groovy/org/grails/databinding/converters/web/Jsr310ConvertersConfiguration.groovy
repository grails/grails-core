package org.grails.databinding.converters.web

import grails.databinding.converters.FormattedValueConverter
import grails.databinding.converters.ValueConverter
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
}
