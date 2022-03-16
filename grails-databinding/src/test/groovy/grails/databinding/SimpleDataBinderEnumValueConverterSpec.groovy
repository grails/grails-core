package grails.databinding

import grails.databinding.SimpleDataBinder;
import grails.databinding.SimpleMapDataBindingSource;
import grails.databinding.converters.ValueConverter;
import spock.lang.Issue
import spock.lang.Specification

class SimpleDataBinderEnumValueConverterSpec extends Specification {

    @Issue('GRAILS-10837')
    void 'Test ValueConverter for enum'() {
        given:
        def binder = new SimpleDataBinder()
        binder.registerConverter(new ColorConverter())
        def hat = new Hat()
        
        when:
        binder.bind hat, [hatColor: '2', hatSize: 'LARGE'] as SimpleMapDataBindingSource
        
        then:
        hat.hatColor == Color.GREEN
        hat.hatSize == Size.LARGE
    }
}

class Hat {
    Color hatColor
    Size hatSize
}

enum Size {
    MEDIUM, LARGE
}

enum Color {
    RED('1'),
    GREEN('2'),
    BLUE('3')

    String id

    Color(String id) {
        this.id = id
    }

    static getById(String id) {
        Color.find{ it.id == id }
    }
}

class ColorConverter implements ValueConverter {
    @Override
    boolean canConvert(Object value) {
        value instanceof String
    }

    @Override
    Object convert(Object value) {
        Color.getById(value)
    }

    @Override
    Class<?> getTargetType() {
        Color
    }
}
