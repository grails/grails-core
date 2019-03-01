/* Copyright 2013 the original author or authors.
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
package grails.databinding

import grails.databinding.converters.ValueConverter
import grails.databinding.errors.BindingError
import grails.databinding.events.DataBindingListenerAdapter
import org.grails.databinding.converters.DateConversionHelper

import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification

import java.text.SimpleDateFormat

class SimpleDataBinderSpec extends Specification {

    void 'Test binding to dynamically typed properties'() {
        given:
        def binder = new SimpleDataBinder()
        def w = new Widget()

        when:
        binder.bind(w, new SimpleMapDataBindingSource(alpha: 1, beta: 2))

        then:
        w.alpha == 1
        w.beta == 2
    }

    void 'Test binding to dynamically typed inherited properties'() {
        given:
        def binder = new SimpleDataBinder()
        def g = new Gadget()

        when:
        binder.bind(g, new SimpleMapDataBindingSource([alpha: 1, beta: 2, gamma: 3]))

        then:
        g.alpha == 1
        g.beta == 2
        g.gamma == 3
    }

    @Ignore
    void 'Test binding to nested properties with dotted path syntax'() {
        given:
        def binder = new SimpleDataBinder()
        def f = new Fidget()

        when:
        binder.bind(f, new SimpleMapDataBindingSource([name: 'Stuff', 'gadget.gamma': 42, 'gadget.alpha': 43]))

        then:
        f.name == 'Stuff'
        f.gadget.gamma == 42
        f.gadget.alpha == 43
        f.gadget.beta == null
    }

    void 'Test binding nested Maps'() {
        given:
        def binder = new SimpleDataBinder()
        def f = new Fidget()

        when:
        binder.bind f, new SimpleMapDataBindingSource([name: 'Stuff', gadget: [gamma: 42, alpha: 43]])

        then:
        f.name == 'Stuff'
        f.gadget.gamma == 42
        f.gadget.alpha == 43
        f.gadget.beta == null
    }

    void 'Test binding String to Integer'() {
        given:
        def binder = new SimpleDataBinder()
        def w = new Widget()

        when:
        binder.bind(w, new SimpleMapDataBindingSource([alpha: 1, beta: 2, delta: '42']))

        then:
        w.alpha == 1
        w.beta == 2
        w.delta == 42
    }

    void 'Test binding Integer to Number'() {
        given: def binder = new SimpleDataBinder()
        def w = new Widget()

        when:
        binder.bind(w, new SimpleMapDataBindingSource([epsilon: 42]))

        then:
        w.epsilon == 42
    }

    void 'Test binding array of String to a Set of Strings'() {
        given:
        def binder = new SimpleDataBinder()
        def obj = new Widget()

        when:
        binder.bind obj, new SimpleMapDataBindingSource([widgetChildren: ['Child 1', 'Child 2', 'Child 3'] as String[]])

        then:
        3 == obj.widgetChildren?.size()
        'Child 1' in obj.widgetChildren
        'Child 2' in obj.widgetChildren
        'Child 3' in obj.widgetChildren
    }

    void 'Test date binding'() {
        given:
        def binder = new SimpleDataBinder()
        def obj = new DateContainer()
        def nowUtilDate = new Date()
        def nowSqlDate = new java.sql.Date(nowUtilDate.time)
        def nowCalendar = Calendar.instance

        when:
        binder.bind(obj, new SimpleMapDataBindingSource([utilDate: nowUtilDate, sqlDate: nowSqlDate, calendar: nowCalendar]))

        then:
        obj.utilDate == nowUtilDate
        obj.sqlDate == nowSqlDate
        obj.calendar == nowCalendar
    }
    
    @Issue('GRAILS-10925')
    void 'Test binding a Date to a Date property marked with @BindingFormat'() {
        given:
        def binder = new SimpleDataBinder()
        def obj = new DateContainer()
        def nowDate = new Date()
        
        when:
        binder.bind obj, [formattedUtilDate: nowDate] as SimpleMapDataBindingSource
        
        then:
        obj.formattedUtilDate == nowDate
    }

    void 'Test listener is notified for properties in nested maps'() {
        given:
        def binder = new SimpleDataBinder()
        def obj = new DateContainer()
        def afterBindingEvents = []
        def listener = new DataBindingListenerAdapter() {
            void afterBinding(o, String propertyName, errors) {
                afterBindingEvents << [object: o, propertyName: propertyName]
            }
        }
        def f = new Fidget()

        when:
        binder.bind f, new SimpleMapDataBindingSource([name: 'Stuff', gadget: [gamma: 42, alpha: 43]]), listener

        then:
        f.name == 'Stuff'
        f.gadget.gamma == 42
        f.gadget.alpha == 43
        f.gadget.beta == null
        afterBindingEvents.size() == 4
        afterBindingEvents.find { it.object.is(f) && it.propertyName == 'name' }
        afterBindingEvents.find { it.object.is(f) && it.propertyName == 'gadget' }
        afterBindingEvents.find { it.object.is(f.gadget) && it.propertyName == 'alpha' }
        afterBindingEvents.find { it.object.is(f.gadget) && it.propertyName == 'gamma' }
    }

    void 'Test invalid date format'() {
        given:
        def binder = new SimpleDataBinder()
        def obj = new DateContainer()
        def bindingErrors = []
        def listener = new DataBindingListenerAdapter() {
            void bindingError(BindingError error, errors) {
                bindingErrors << error
            }
        }

        when:
        binder.bind obj, new SimpleMapDataBindingSource([formattedUtilDate: 'BAD']), listener

        then:
        obj.formattedUtilDate == null
        bindingErrors.size() == 1
        bindingErrors[0].rejectedValue == 'BAD'
        bindingErrors[0].cause.message == 'Unparseable date: "BAD"'
    }

    void 'Test binding string to date'() {
        given:
        def binder = new SimpleDataBinder()
        binder.registerConverter new DateConversionHelper(formatStrings: ['yyyy-MM-dd HH:mm:ss.S',"yyyy-MM-dd'T'HH:mm:ss'Z'","yyyy-MM-dd HH:mm:ss.S z","yyyy-MM-dd'T'HH:mm:ss.SSSX"])

        def obj = new DateContainer()

        when:
        binder.bind obj, new SimpleMapDataBindingSource([utilDate: '2013-04-15 21:26:31.973', formattedUtilDate: '11151969'])

        then:
        Calendar.APRIL == obj.utilDate.month
        15 == obj.utilDate.date
        113 == obj.utilDate.year
        21 == obj.utilDate.hours
        26 == obj.utilDate.minutes
        31 == obj.utilDate.seconds
        Calendar.NOVEMBER == obj.formattedUtilDate.month
        15 == obj.formattedUtilDate.date
        69 == obj.formattedUtilDate.year

        when:
        obj.utilDate = null
        binder.bind obj, new SimpleMapDataBindingSource([utilDate: "2011-03-12T09:24:22Z"])

        then:
        Calendar.MARCH == obj.utilDate.month
        12 == obj.utilDate.date
        111 == obj.utilDate.year
        9 == obj.utilDate.hours
        24 == obj.utilDate.minutes
        22 == obj.utilDate.seconds
    }

    void 'Test structured date binding'() {
        given:
        def binder = new SimpleDataBinder()
        def obj = new DateContainer()

        when:
        binder.bind(obj, new SimpleMapDataBindingSource([utilDate_month: '11',
            utilDate_day: '15',
            utilDate_year: '1969',
            calendar_month: '4',
            calendar_day: '21',
            calendar_year: '2049',
            sqlDate_month: '6',
            sqlDate_day: '14',
            sqlDate_year: '1937',
            sqlDate: 'struct',
            calendar: 'struct',
            utilDate: 'struct']))
        def utilDate = obj.utilDate
        def calendar = obj.calendar
        def sqlDate = obj.sqlDate

        then:
        Calendar.NOVEMBER == utilDate.month
        15 == utilDate.date
        69 == utilDate.year
        Calendar.JUNE == sqlDate.month
        14 == sqlDate.date
        37 == sqlDate.year
        Calendar.APRIL == calendar.get(Calendar.MONTH)
        21 == calendar.get(Calendar.DATE)
        2049 == calendar.get(Calendar.YEAR)

        when:
        obj.utilDate = obj.calendar = obj.sqlDate = null
        binder.bind(obj, new SimpleMapDataBindingSource([utilDate_month: '11',
            utilDate_day: '15',
            utilDate_year: '1969',
            calendar_month: '4',
            calendar_day: '21',
            calendar_year: '2049',
            sqlDate_month: '6',
            sqlDate_day: '14',
            sqlDate_year: '1937',
            sqlDate: 'date.struct',
            calendar: 'date.struct',
            utilDate: 'date.struct']))
        utilDate = obj.utilDate
        calendar = obj.calendar
        sqlDate = obj.sqlDate

        then:
        Calendar.NOVEMBER == utilDate.month
        15 == utilDate.date
        69 == utilDate.year
        Calendar.JUNE == sqlDate.month
        14 == sqlDate.date
        37 == sqlDate.year
        Calendar.APRIL == calendar.get(Calendar.MONTH)
        21 == calendar.get(Calendar.DATE)
        2049 == calendar.get(Calendar.YEAR)

        when:
        obj.utilDate = obj.calendar = obj.sqlDate = null
        binder.bind(obj, new SimpleMapDataBindingSource([utilDate_month: '11',
            utilDate_day: '15',
            utilDate_year: '1969',
            calendar_month: '4',
            calendar_day: '21',
            calendar_year: '2049',
            sqlDate_month: '6',
            sqlDate_day: '14',
            sqlDate_year: '1937',
            sqlDate: 'struct',
            calendar: 'struct',
            utilDate: 'struct']), null, ['sqlDate', 'utilDate'])
        utilDate = obj.utilDate
        calendar = obj.calendar
        sqlDate = obj.sqlDate

        then:
        sqlDate == null
        utilDate == null
        Calendar.APRIL == calendar.get(Calendar.MONTH)
        21 == calendar.get(Calendar.DATE)
        2049 == calendar.get(Calendar.YEAR)
    }

    void 'Test struct binding to a list'() {
        given:
        def binder = new SimpleDataBinder()
        def obj = new DateCollection()

        when:
        binder.bind(obj, new SimpleMapDataBindingSource([
                'dates[0]'      : 'struct',
                'dates[0]_day'  : '09',
                'dates[0]_month': '11',
                'dates[0]_year' : '2012',
                'dates[1]'      : 'struct',
                'dates[1]_day'  : '13',
                'dates[1]_month': '12',
                'dates[1]_year' : '2012',
        ]))
        def dates = obj.dates

        then:
        dates == [new SimpleDateFormat('yyyy-MM-d').parse("2012-11-9"), new SimpleDateFormat('yyyy-MM-d').parse("2012-12-13")]
    }

    void 'Test binding String to enum'() {
        given:
        def binder = new SimpleDataBinder()
        def user = new SystemUser()

        when:
        binder.bind user, new SimpleMapDataBindingSource([role: 'ADMIN'])

        then:
        user.role == Role.ADMIN

        when:
        binder.bind user, new SimpleMapDataBindingSource([role: null])

        then:
        user.role == null

        when:
        binder.bind user, new SimpleMapDataBindingSource([role: 'BAD'])

        then:
        user.role == null

        when:
        binder.bind user, new SimpleMapDataBindingSource([role: 'USER'])

        then:
        user.role == Role.USER
    }

    void 'Test special boolean handling'() {
        given:
        def binder = new SimpleDataBinder()
        def factory = new Factory()
        factory.isActive = true

        when:
        binder.bind factory, new SimpleMapDataBindingSource([_isActive: ''])

        then:
        !factory.isActive

        when:
        // the underscore one should be ignored since the real one is present
        binder.bind factory, new SimpleMapDataBindingSource([isActive: true, _isActive: ''])

        then:
        factory.isActive
    }

    void 'Test binding to a List with a combination of Map values and instances of the actual type contained in the List '() {
        given:
        def binder = new SimpleDataBinder()
        def bindingSource = [:]
        bindingSource.name = 'My Factory'

        // this list contains Maps and a Widget instance.  The Maps should be transformed into Widget instances
        bindingSource.widgets = [widget:[[alpha: 'alpha 1', beta: 'beta 1'], new Widget(alpha: 'alpha 2', beta: 'beta 2'), [alpha: 'alpha 3', beta: 'beta 3']]]
        def factory = new Factory()

        when:
        binder.bind factory, new SimpleMapDataBindingSource(bindingSource)

        then:
        factory.name == 'My Factory'
        factory.widgets.size() == 3
        factory.widgets[0] instanceof Widget
        factory.widgets[1] instanceof Widget
        factory.widgets[2] instanceof Widget
        factory.widgets[0].alpha == 'alpha 1'
        factory.widgets[0].beta == 'beta 1'
        factory.widgets[1].alpha == 'alpha 2'
        factory.widgets[1].beta == 'beta 2'
        factory.widgets[2].alpha == 'alpha 3'
        factory.widgets[2].beta == 'beta 3'
    }

    void 'Test binding to a Set of Integer'() {
        given:
        def binder = new SimpleDataBinder()
        def widget = new Widget()

        when:
        binder.bind widget, new SimpleMapDataBindingSource([numbers: ['4', '5', '6']])

        then:
        widget.numbers.size() == 3
        widget.numbers.contains 4
        widget.numbers.contains 5
        widget.numbers.contains 6

        when:
        widget.numbers = [1, 2, 3]
        binder.bind widget, new SimpleMapDataBindingSource([numbers: ['4', '5', '6']])

        then:
        widget.numbers.size() == 3
        widget.numbers.contains 4
        widget.numbers.contains 5
        widget.numbers.contains 6
    }

    void 'Test binding to an array of Integer'() {
        given:
        def widget = new Widget()
        def binder = new SimpleDataBinder()

        when:
        binder.bind widget, new SimpleMapDataBindingSource(['integers[0]': 42, 'integers[1]': '2112'])

        then:
        widget.integers.length == 2
        widget.integers[0] == 42
        widget.integers[1] == 2112
    }

    void 'Test binding ranges'() {
        given:
        def widget = new Widget()
        def binder = new SimpleDataBinder()

        when:
        binder.bind widget, new SimpleMapDataBindingSource([validNumbers: 3..5])

        then:
        widget.validNumbers == 3..5

        when:
        binder.bind widget, new SimpleMapDataBindingSource([validNumbers: null])

        then:
        widget.validNumbers == null

        when:
        binder.bind widget, new SimpleMapDataBindingSource([validNumbers: 1..5])

        then:
        widget.validNumbers == 1..5

        when:
        binder.bind widget, new SimpleMapDataBindingSource([byteArray: 3..7])

        then:
        widget.byteArray.length == 5
        widget.byteArray == ([3,4,5,6,7] as byte[])
    }

    void 'Test auto grow collection limit'() {
        given:
        def binder = new SimpleDataBinder()
        def widget = new Widget()
        binder.autoGrowCollectionLimit = 3

        when:
        binder.bind widget, new SimpleMapDataBindingSource(['integers[5]': 50, 'integers[0]': 10, 'integers[3]': 30, 'integers[2]': 20])

        then:
        widget.integers.length == 3
        widget.integers[0] == 10
        widget.integers[1] == null
        widget.integers[2] == 20

        when:
        binder.bind widget, new SimpleMapDataBindingSource(['names[5]': 'five', 'names[0]': 'zero', 'names[3]': 'three', 'names[2]': 'two'])

        then:
        widget.names.size() == 3
        widget.names[0] == 'zero'
        widget.names[1] == null
        widget.names[2] == 'two'
    }
    
    void 'Test @BindUsing on a List<Integer>'() {
        given:
        def binder = new SimpleDataBinder()
        def widget = new Widget()
        
        when:
        binder.bind widget, [listOfIntegers: '4'] as SimpleMapDataBindingSource
        
        then:
        widget.listOfIntegers == [0, 1, 2, 3]
    }
    
    @Issue('GRAILS-10853')
    void 'Test adding new elements to a Set using indexed properties'() {
        given:
        def binder = new SimpleDataBinder()
        def widget = new Widget()
        
        when:
        binder.bind widget, ['factories[2]': [name: 'Tres'], 'factories[0]': [name: 'Uno'], 'factories[1]': [name: 'Dos']] as SimpleMapDataBindingSource
        
        then:
        widget.factories.size() == 3
        widget.factories.find { it.name == 'Uno' }
        widget.factories.find { it.name == 'Dos' }
        widget.factories.find { it.name == 'Tres' }
    }
    
    @Issue('GRAILS-10865')
    void 'Test binding to an inherited typed collection'() {
        given:
        def binder = new SimpleDataBinder()
        def obj = new ClassWithInheritedTypedCollection()
        
        when:
        binder.bind obj, [list: ['1', '2', '3'], 'map[one]': '1', 'map[two]': '2' ] as SimpleMapDataBindingSource
        
        then:
        obj.list == [1, 2, 3]
        obj.map.one == 1
        obj.map.two == 2
    }

    @Issue('https://github.com/grails/grails-core/issues/11140')
    void 'Test bind a Integer on a List<Long>'() {
        given:
        def binder = new SimpleDataBinder()
        def widget = new Widget()

        when:
        binder.bind widget, [listOfLongs: 4] as SimpleMapDataBindingSource

        then:
        widget.listOfLongs == [4L]
        widget.listOfLongs.first().getClass() == Long
    }

    @Issue('https://github.com/grails/grails-core/issues/11235')
    void 'Test binding to a list using custom value converters'() {
        given:
        def binder = new SimpleDataBinder()
        def comment = new Comment()

        and:
        binder.registerConverter(new ValueConverter() {
            @Override
            boolean canConvert(Object value) {
                value instanceof String
            }

            @Override
            Object convert(Object value) {
                new Attachment(filename: "$value")
            }

            @Override
            Class<?> getTargetType() {
                return Attachment
            }
        })

        when:
        binder.bind comment, [
                'attachments[0]': 'foo.txt',
                'attachments[1]': 'bar.txt'
        ] as SimpleMapDataBindingSource

        then:
        comment.attachments.size() == 2
        comment.attachments.find { it.filename == 'foo.txt' }
        comment.attachments.find { it.filename == 'bar.txt' }
    }
}

class Factory {
    def name
    List<Widget> widgets
    boolean isActive
}

class Widget {
    def alpha
    def beta
    Integer delta
    Number epsilon
    Set<String> widgetChildren
    Gadget nestedGadget
    Set<Integer> numbers
    Range validNumbers = 4..42
    byte[] byteArray
    Integer[] integers
    List<String> names
    @BindUsing({ obj, source -> 
        def cnt = source['listOfIntegers'] as int
        def result = []
        cnt.times { result << it }
        result
    })
    List<Integer> listOfIntegers = []
    List<Long> listOfLongs = []
    Set<Factory> factories
}

class Gadget extends Widget {
    def gamma
}

class Fidget {
    def name
    Gadget gadget
}

class DateContainer {
    Date utilDate
    java.sql.Date sqlDate
    Calendar calendar

    @BindingFormat('MMddyyyy')
    Date formattedUtilDate
}

enum Role {
    ADMIN, USER
}

class SystemUser {
    Role role
}

abstract class AbstractClassWithTypedCollection {
    List<Integer> list
    Map<String, Integer> map
}

class ClassWithInheritedTypedCollection extends AbstractClassWithTypedCollection {}

class DateCollection {
    List<Date> dates
}

class Comment {
    Set<Attachment> attachments
}

class Attachment {
    String filename
}

