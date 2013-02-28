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
package org.grails.databinding

import spock.lang.Ignore
import spock.lang.Specification

class SimpleDataBinderSpec extends Specification {

    void 'Test binding to dynamically typed properties'() {
        given:
        def binder = new SimpleDataBinder()
        def w = new Widget()

        when:
        binder.bind(w, [alpha: 1, beta: 2])

        then:
        w.alpha == 1
        w.beta == 2
    }

    void 'Test binding to dynamically typed inherited properties'() {
        given:
        def binder = new SimpleDataBinder()
        def g = new Gadget()

        when:
        binder.bind(g, [alpha: 1, beta: 2, gamma: 3])

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
        binder.bind(f, [name: 'Stuff', 'gadget.gamma': 42, 'gadget.alpha': 43])

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
        binder.bind f, [name: 'Stuff', gadget: [gamma: 42, alpha: 43]]

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
        binder.bind(w, [alpha: 1, beta: 2, delta: '42'])

        then:
        w.alpha == 1
        w.beta == 2
        w.delta == 42
    }

    void 'Test binding Integer to Number'() {
        given: def binder = new SimpleDataBinder()
        def w = new Widget()

        when:
        binder.bind(w, [epsilon: 42])

        then:
        w.epsilon == 42
    }

    void 'Test binding array of String to a Set of Strings'() {
        given:
        def binder = new SimpleDataBinder()
        def obj = new Widget()
        
        when:
        binder.bind obj, [widgetChildren: ['Child 1', 'Child 2', 'Child 3'] as String[]]
        
        then:
        3 == obj.widgetChildren?.size()
        'Child 1' in obj.widgetChildren
        'Child 2' in obj.widgetChildren
        'Child 3' in obj.widgetChildren
    }

    void 'Test binding to primitives from Strings'() {
        given:
        def binder = new SimpleDataBinder()
        def obj = new PrimitiveContainer()

        when:
        binder.bind(obj, [someBoolean: 'true',
            someByte: '1',
            someChar: 'a',
            someShort: '2',
            someInt: '3',
            someLong: '4',
            someFloat: '5.5',
            someDouble: '6.6'])

        then:
        obj.someBoolean == true
        obj.someByte == 1
        obj.someChar == ('a' as char)
        obj.someShort == 2
        obj.someInt == 3
        obj.someLong == 4
        obj.someFloat == 5.5
        obj.someDouble == 6.6
    }

    void 'Test date binding'() {
        given:
        def binder = new SimpleDataBinder()
        def obj = new DateContainer()
        def nowUtilDate = new Date()
        def nowSqlDate = new java.sql.Date(nowUtilDate.time)
        def nowCalendar = Calendar.instance

        when:
        binder.bind(obj, [utilDate: nowUtilDate, sqlDate: nowSqlDate, calendar: nowCalendar])

        then:
        obj.utilDate == nowUtilDate
        obj.sqlDate == nowSqlDate
        obj.calendar == nowCalendar
    }

    void 'Test structured date binding'() {
        given:
        def binder = new SimpleDataBinder()
        def obj = new DateContainer()

        when:
        binder.bind(obj, [utilDate_month: '11',
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
            utilDate: 'struct'])
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
        binder.bind(obj, [utilDate_month: '11',
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
            utilDate: 'struct'], null, ['sqlDate', 'utilDate'])
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
    
    void 'Test custom data converter'() {
        given:
        def binder = new SimpleDataBinder()
        def converter = new StringToGadgetBindingHelper()
        binder.registerTypeConverter Gadget, converter
        def obj = new Widget()
        
        when:
        binder.bind obj, [nestedGadget: [gamma: 'Some Gamma']]
        
        then:
        'SOME GAMMA' == obj.nestedGadget.gamma
    }
    
    void 'Test binding String to enum'() {
        given:
        def binder = new SimpleDataBinder()
        def user = new SystemUser()
        
        when:
        binder.bind user, [role: 'ADMIN']
        
        then:
        user.role == Role.ADMIN
        
        when:
        binder.bind user, [role: null]
        
        then:
        user.role == null
        
        when:
        binder.bind user, [role: 'BAD']
        
        then:
        user.role == null
        
        when:
        binder.bind user, [role: 'USER']
        
        then:
        user.role == Role.USER
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
        binder.bind factory, bindingSource
        
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
}

class StringToGadgetBindingHelper implements BindingHelper {

    public Object getPropertyValue(Object obj, String propertyName,
            Map<String, Object> source) {
        def gammaValue = source[propertyName]['gamma']
        
        new Gadget(gamma: gammaValue?.toUpperCase())
    }
}

class Factory {
    def name
    List<Widget> widgets
}

class Widget {
    def alpha
    def beta
    Integer delta
    Number epsilon
    Set<String> widgetChildren
    Gadget nestedGadget
}

class Gadget extends Widget {
    def gamma
}

class Fidget {
    def name
    Gadget gadget
}

class PrimitiveContainer {
    boolean someBoolean
    byte someByte
    char someChar
    short someShort
    int someInt
    long someLong
    float someFloat
    double someDouble
}

class DateContainer {
    java.util.Date utilDate
    java.sql.Date sqlDate
    java.util.Calendar calendar
}

enum Role {
    ADMIN, USER
}

class SystemUser {
    Role role
}