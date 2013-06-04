package org.codehaus.groovy.grails.web.binding

import grails.util.GrailsWebUtil

import java.lang.reflect.Modifier

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader

import spock.lang.Specification

class DefaultASTDatabindingHelperSpec extends Specification {
    static widgetClass
    static widgetSubclass
    static setterGetterClass
    static dateBindingClass
    static classWithHasMany
    static classWithNoBindableProperties

    def setupSpec() {
        final gcl = new GrailsAwareClassLoader()
        final transformer = new AstDatabindingInjector()
        gcl.classInjectors = [transformer]as ClassInjector[]
        widgetClass = gcl.parseClass('''
            class Widget {
                static transients = ['integerListedInTransientsProperty']
                Integer bindableProperty
                Integer secondBindableProperty
                Integer nonBindableProperty
                Integer secondNonBindableProperty
                static Integer staticProperty
                def untypedProperty
                transient Integer transientInteger
                Integer integerListedInTransientsProperty
                Person person

                static constraints = {
                    nonBindableProperty bindable: false
                    secondNonBindableProperty bindable: false
                    somePropertyThatDoesNotExistAtCompileTime bindable: true
                    someOtherPropertyThatDoesNotExistAtCompileTime bindable: false
                }
            }
            class Person {
                String firstName
            }
            ''')
            widgetSubclass = gcl.parseClass('''
                class WidgetSubclass extends Widget {
                    Integer subclassBindableProperty
                    Integer subclassNonBindableProperty
                    static Integer subclassStaticProperty
                    def subclassUntypedProperty

                    static constraints = {
                        bindableProperty bindable: false
                        subclassNonBindableProperty bindable: false
                        nonBindableProperty bindable: true
                    }
                }
            ''')
            setterGetterClass = gcl.parseClass('''
            class SetterGetterClass {
                private internalFirstName
                private internalMiddleName
                private internalLastName
                private internalTitle

                void setFirstName(String s) {
                    internalFirstName = s
                }

                void setMiddleName(String s, int i) {
                    internalMiddleName = s
                }

                void getLastName() {
                    internalLastName
                }

                void setTitle(String s) {
                    internalTitle = s
                }

                String getTitle() {
                    internalTitle
                }
            }
            ''')
            dateBindingClass = gcl.parseClass('''
                class DateBindingClass {
                    String name
                    Date birthDate
                    Date hireDate
                    Calendar exitDate
                    java.sql.Date sqlDate
                    static constraints = {
                        hireDate bindable: false
                    }
                }
            ''')
            classWithHasMany = gcl.parseClass('''
                class ClassWithHasMany {
                    String name
                    static hasMany = [people: Person, widgets: Widget]
                    static constraints = {
                        widgets bindable: false
                    }
                }
            ''')
            classWithNoBindableProperties = gcl.parseClass('''
                class ClassWithNoBindableProperties {
                    String firstName
                    String lastName
                    static constraints = {
                        firstName bindable: false
                        lastName bindable: false
                    }
                }''')

            // there must be a request bound in order for the structured date editor to be registered
            GrailsWebUtil.bindMockWebRequest()
    }

    void 'Test class with hasMany'() {
        when:
        final whiteListField = classWithHasMany.getDeclaredField(DefaultASTDatabindingHelper.DEFAULT_DATABINDING_WHITELIST)

        then:
            whiteListField?.modifiers & Modifier.STATIC
            whiteListField.type == List

        when:
            final whiteList = whiteListField.get(null)

        then:
            whiteList?.size() == 4
            'name' in whiteList
            'people' in whiteList
            'people_*' in whiteList
            'people.*' in whiteList
    }

    void 'Test class with getters and setters that do not directly map to fields'() {
        when:
        final whiteListField = setterGetterClass.getDeclaredField(DefaultASTDatabindingHelper.DEFAULT_DATABINDING_WHITELIST)

        then:
            whiteListField?.modifiers & Modifier.STATIC
            whiteListField.type == List

        when:
            final whiteList = whiteListField.get(null)

        then:
            whiteList?.size() == 6
            'firstName_*' in whiteList
            'firstName.*' in whiteList
            'firstName' in whiteList
            'title_*' in whiteList
            'title.*' in whiteList
            'title' in whiteList

        when:
            def obj = setterGetterClass.newInstance()
            obj.properties = [firstName: 'a',
                              middleName: 'b',
                              lastName: 'c',
                              title: 'd']

        then:
            'a' == obj.@internalFirstName
            null == obj.@internalMiddleName
            null == obj.@internalLastName
            'd' == obj.title
    }

    void 'Test default generated data binding white list'() {
       when:
           final whiteListField = widgetClass.getDeclaredField(DefaultASTDatabindingHelper.DEFAULT_DATABINDING_WHITELIST)

       then:
           whiteListField?.modifiers & Modifier.STATIC
           whiteListField.type == List

       when:
           final whiteList = whiteListField.get(null)

       then:
           whiteList?.size() == 8
           'bindableProperty' in whiteList
           'secondBindableProperty' in whiteList
           'somePropertyThatDoesNotExistAtCompileTime' in whiteList
           'somePropertyThatDoesNotExistAtCompileTime_*' in whiteList
           'somePropertyThatDoesNotExistAtCompileTime.*' in whiteList
           'person' in whiteList
           'person.*' in whiteList
           'person_*' in whiteList
    }

    void 'Test binding to a class that has no bindable properties'() {
        given:
            def obj = classWithNoBindableProperties.newInstance()

        when:
            obj.properties = [firstName: 'First Name', lastName: 'Last Name']

        then:
            obj.firstName == null
            obj.lastName == null
    }

    void 'Test that binding respects the generated white list'() {
        given:
            def obj = widgetClass.newInstance()

        when:
            obj.properties = [bindableProperty: 1,
                              secondBindableProperty: 2,
                              nonBindableProperty: 3,
                              secondNonBindableProperty: 4,
                              staticProperty: 5,
                              untypedProperty: 6,
                              transientInteger: 7,
                              integerListedInTransientsProperty: 8]

        then:
            1 == obj.bindableProperty
            2 == obj.secondBindableProperty
            null == obj.nonBindableProperty
            null == obj.secondNonBindableProperty
            null == obj.staticProperty
            null == obj.untypedProperty
            null == obj.transientInteger
            null == obj.integerListedInTransientsProperty
    }

    void 'Test explicit white list overrides default white list in class'() {
        given:
            def obj = widgetClass.newInstance()

        when:
            obj.properties['bindableProperty', 'nonBindableProperty'] =
                             [bindableProperty: 1,
                              secondBindableProperty: 2,
                              nonBindableProperty: 3,
                              secondNonBindableProperty: 4,
                              staticProperty: 5,
                              untypedProperty: 6,
                              transientInteger: 7,
                              integerListedInTransientsProperty: 8]

        then:
            1 == obj.bindableProperty
            null == obj.secondBindableProperty
            3 == obj.nonBindableProperty
            null == obj.secondNonBindableProperty
            null == obj.staticProperty
            null == obj.untypedProperty
            null == obj.transientInteger
            null == obj.integerListedInTransientsProperty
    }

    void 'Test default generated data binding white list on a subclass'() {
        when:
        final whiteListField = widgetSubclass.getDeclaredField(DefaultASTDatabindingHelper.DEFAULT_DATABINDING_WHITELIST)

    then:
        whiteListField?.modifiers & Modifier.STATIC
        whiteListField.type == List

    when:
        final whiteList = whiteListField.get(null)

    then:
        whiteList?.size() == 9
        'subclassBindableProperty' in whiteList
        'nonBindableProperty' in whiteList
        'secondBindableProperty' in whiteList
        'somePropertyThatDoesNotExistAtCompileTime' in whiteList
        'somePropertyThatDoesNotExistAtCompileTime_*' in whiteList
        'somePropertyThatDoesNotExistAtCompileTime.*' in whiteList
        'person' in whiteList
        'person.*' in whiteList
        'person_*' in whiteList
    }

    void 'Test that binding respects the generated white list in subclass'() {
        given:
            def obj = widgetSubclass.newInstance()

        when:
            obj.properties = [bindableProperty: 1,
                              secondBindableProperty: 2,
                              nonBindableProperty: 3,
                              secondNonBindableProperty: 4,
                              staticProperty: 5,
                              subclassUntypedProperty: 6,
                              subclassBindableProperty: 7,
                              subclassNonBindableProperty: 8,
                              subclassStaticProperty: 9,
                              subclassUntypedProperty: 10,
                              transientInteger: 11,
                              integerListedInTransientsProperty: 12]

        then:
            null == obj.bindableProperty
            2 == obj.secondBindableProperty
            3 == obj.nonBindableProperty
            null == obj.secondNonBindableProperty
            null == obj.staticProperty
            null == obj.untypedProperty
            7 == obj.subclassBindableProperty
            null == obj.subclassNonBindableProperty
            null == obj.subclassStaticProperty
            null == obj.subclassUntypedProperty
            null == obj.transientInteger
            null == obj.integerListedInTransientsProperty
    }

    void 'Test explicit white list overrides default white list in subclass'() {
        given:
            def obj = widgetSubclass.newInstance()

        when:
            obj.properties['nonBindableProperty', 'subclassNonBindableProperty'] =
                 [bindableProperty: 1,
                  secondBindableProperty: 2,
                  nonBindableProperty: 3,
                  secondNonBindableProperty: 4,
                  staticProperty: 5,
                  subclassUntypedProperty: 6,
                  subclassBindableProperty: 7,
                  subclassNonBindableProperty: 8,
                  subclassStaticProperty: 9,
                  subclassUntypedProperty: 10,
                  transientInteger: 11,
                  integerListedInTransientsProperty: 12]

        then:
            null == obj.bindableProperty
            null == obj.secondBindableProperty
            3 == obj.nonBindableProperty
            null == obj.secondNonBindableProperty
            null == obj.staticProperty
            null == obj.untypedProperty
            null == obj.subclassBindableProperty
            8 == obj.subclassNonBindableProperty
            null == obj.subclassStaticProperty
            null == obj.subclassUntypedProperty
            null == obj.transientInteger
            null == obj.integerListedInTransientsProperty
    }

    void 'Test structured Date binding'() {
        given:
            def obj = dateBindingClass.newInstance()

        when:
            obj.properties = [birthDate_month: '11',
                              birthDate_day: '15',
                              birthDate_year: '1969',
                              exitDate_month: '4',
                              exitDate_day: '21',
                              exitDate_year: '2049',
                              sqlDate_month: '6',
                              sqlDate_day: '14',
                              sqlDate_year: '1937',
                              hireDate_month: '1',
                              hireDate_day: '15',
                              hireDate_year: '2001',
                              hireDate: 'struct',
                              birthDate: 'struct',
                              exitDate: 'struct',
                              sqlDate: 'struct',
                              name: 'Jose']
            def hireDate = obj.hireDate
            def birthDate = obj.birthDate
            def exitDate = obj.exitDate
            def sqlDate = obj.sqlDate
            def name = obj.name

        then:
            'Jose' == name
            !hireDate
            Calendar.NOVEMBER == birthDate.month
            15 == birthDate.date
            69 == birthDate.year
            Calendar.JUNE == sqlDate.month
            14 == sqlDate.date
            37 == sqlDate.year
            Calendar.APRIL == exitDate.get(Calendar.MONTH)
            21 == exitDate.get(Calendar.DATE)
            2049 == exitDate.get(Calendar.YEAR)
    }
}

class AstDatabindingInjector implements ClassInjector {
    void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode)
    }

    @Override
    void performInjectionOnAnnotatedClass(SourceUnit source, ClassNode classNode) {
        performInjection( source, null, classNode)
    }

    void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        new DefaultASTDatabindingHelper().injectDatabindingCode(source, context, classNode)
    }
    boolean shouldInject(URL url) {
        return true
    }
}
