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
package org.grails.databinding

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.slurpersupport.GPathResult

import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType

import org.apache.commons.collections.set.ListOrderedSet
import org.grails.databinding.converters.ConversionService
import org.grails.databinding.converters.DateConversionHelper
import org.grails.databinding.converters.FormattedDateValueConverter
import org.grails.databinding.converters.FormattedValueConverter
import org.grails.databinding.converters.StructuredCalendarBindingEditor
import org.grails.databinding.converters.StructuredDateBindingEditor
import org.grails.databinding.converters.StructuredSqlDateBindingEditor
import org.grails.databinding.converters.ValueConverter
import org.grails.databinding.errors.SimpleBindingError
import org.grails.databinding.events.DataBindingListener
import org.grails.databinding.xml.GPathResultMap

/** 
 * A data binder that will bind nested Maps to an object.
 * 
 <pre>
 class Person {
     String firstName
     Address homeAddress
 }
 
 class Address {
     String city
     String state
 }
 
 def person = new Person()
 def binder = new SimpleDataBinder()
 binder.bind person, [firstName: 'Steven', homeAddress: [city: 'St. Louis', state: 'Missouri']]
 assert person.firstName == 'Steven'
 assert person.homeAddress.city == 'St. Louis'
 assert person.homeAddress.state == 'Missouri'
 
 </pre>
 *
 * @author Jeff Brown
 * @since 2.3 
 */
@CompileStatic
class SimpleDataBinder implements DataBinder {

    protected Map<Class, StructuredBindingEditor> structuredEditors = new HashMap<Class, StructuredBindingEditor>()
    ConversionService conversionService
    protected Map<Class, ValueConverter> conversionHelpers = new HashMap<Class, ValueConverter>()
    protected Map<Class, FormattedValueConverter> formattedValueConvertersionHelpers = new HashMap<Class, FormattedValueConverter>()

    static final INDEXED_PROPERTY_REGEX = /(.*)\[\s*([^\s]*)\s*\]\s*$/

    SimpleDataBinder() {
        registerConverter Date, new DateConversionHelper()

        registerStructuredEditor java.util.Date.class, new StructuredDateBindingEditor()
        registerStructuredEditor java.sql.Date.class, new StructuredSqlDateBindingEditor()
        registerStructuredEditor java.util.Calendar.class, new StructuredCalendarBindingEditor()
        
        registerFormattedValueConverter new FormattedDateValueConverter()
    }

    void registerStructuredEditor(Class clazz, StructuredBindingEditor editor) {
        structuredEditors[clazz] = editor
    }
    
    void registerConverter(Class clazz, ValueConverter converter) {
        conversionHelpers[clazz] = converter
    }
    void registerFormattedValueConverter(FormattedValueConverter converter) {
        formattedValueConvertersionHelpers[converter.targetType] = converter
    }

    /**
     * @param obj the object to perform data binding on
     * @param source a Map containg the values to be bound to obj
     */
    void bind(obj, Map source) {
        bind obj, source, null, null, null, null
    }

    /**
     * @param obj the object to perform data binding on
     * @param source a Map containg the values to be bound to obj
     * @param listener will be notified of data binding events
     */
    void bind(obj, Map source, DataBindingListener listener) {
        bind obj, source, null, null, null, listener
    }

    /**
     * @param obj the object to perform data binding on
     * @param source a Map containg the values to be bound to obj
     * @param whiteList A list of properties that are eligible for binding, if
     * null all properties are eligible for binding
     */
    void bind(obj, Map source, List whiteList) {
        bind obj, source, null, whiteList, null, null
    }

    /**
     * @param obj the object to perform data binding on
     * @param source a Map containg the values to be bound to obj
     * @param whiteList A list of properties that are eligible for binding, if
     * null all properties are eligible for binding
     * @param blackList A list of properties to exclude from binding
     */
    void bind(obj, Map source, List whiteList, List blackList) {
        bind obj, source, null, whiteList, blackList, null
    }
    
    /**
     * @param obj the object to perform data binding on
     * @param gpath contains an XML representation of the data to be bound to obj
     */
    void bind(obj, GPathResult gpath) {
        bind obj, new GPathResultMap(gpath)
    }

    /**
     * @param obj the object to perform data binding on
     * @param source a Map containg the values to be bound to obj
     * @param whiteList A list of properties that are eligible for binding, if
     * null all properties are eligible for binding
     * @param blackList A list of properties to exclude from binding
     */
    void bind(obj, Map<String, Object> source, String filter, List whiteList, List blackList, DataBindingListener listener) {
        source.each {String propName, val ->
            if(filter && !propName.startsWith(filter + '.')) {
                return
            }
            if(filter) {
                propName = propName[(1+filter.size())..-1]
            }
            processProperty obj, propName, val, source, whiteList, blackList, listener
        }
    }

    protected isOkToBind(String propName, List whiteList, List blackList) {
        'metaClass' != propName && !blackList?.contains(propName) && (!whiteList || whiteList.contains(propName) || whiteList.find { String it -> it.startsWith(propName + '.')})
    }

    protected IndexedPropertyReferenceDescriptor getIndexedPropertyReferenceDescriptor(propName) {
        IndexedPropertyReferenceDescriptor descriptor
        def matcher = propName =~ INDEXED_PROPERTY_REGEX
        if(matcher) {
            def indexedPropertyName = matcher.group(1)
            def index = matcher.group(2)
            descriptor = new IndexedPropertyReferenceDescriptor(propertyName: indexedPropertyName, index: index)
        }
        descriptor
    }
    
    protected processProperty(obj, String propName, val, Map source, List whiteList, List blackList, DataBindingListener listener) {
        def metaProperty = obj.metaClass.getMetaProperty propName
        if(metaProperty) {
            if(isOkToBind(metaProperty.name, whiteList, blackList)) {
                def propertyType = metaProperty.type
                if(structuredEditors.containsKey(propertyType) && ('struct' == val || 'date.struct' == val)) {
                    def structuredEditor = structuredEditors[propertyType]
                    val = structuredEditor.getPropertyValue obj, propName, source
                }
                setPropertyValue obj, source, propName, val, listener
            }
        } else {
            def indexedPropertyReferenceDescriptor = getIndexedPropertyReferenceDescriptor propName
            if(indexedPropertyReferenceDescriptor) {
                def simplePropertyName = indexedPropertyReferenceDescriptor.propertyName
                metaProperty = obj.metaClass.getMetaProperty simplePropertyName
                if(metaProperty && isOkToBind(metaProperty.name, whiteList, blackList)) {
                    def propertyType = metaProperty.type
                    if(Collection.isAssignableFrom(propertyType)) {
                        def index = Integer.parseInt(indexedPropertyReferenceDescriptor.index)
                        Collection collectionInstance = (Collection)obj[simplePropertyName]
                        if(collectionInstance == null) {
                            collectionInstance = initializeCollection obj, simplePropertyName, propertyType
                        }
                        def indexedInstance = collectionInstance[index]
                        if(indexedInstance == null) {
                            Class genericType = getReferencedTypeForCollection(simplePropertyName, obj)
                            if(genericType) {
                                indexedInstance = genericType.newInstance()
                                addElementToCollectionAt obj, simplePropertyName, collectionInstance, index, indexedInstance
                            } else {
                                addElementToCollectionAt obj, simplePropertyName, collectionInstance, index, val
                            }
                        }
                        if(indexedInstance != null) {
                            if(val instanceof Map) {
                                bind indexedInstance, (Map)val, listener
                            } else if (val == null && indexedInstance != null) {
                                addElementToCollectionAt obj, simplePropertyName, collectionInstance, index, null
                            }
                        }
                    } else if(Map.isAssignableFrom(propertyType)) {
                        Map mapInstance = (Map)obj[simplePropertyName]
                        if(mapInstance == null) {
                            mapInstance = initializeMap obj, simplePropertyName
                        }
                        mapInstance[indexedPropertyReferenceDescriptor.index] = val
                    }
                }
            } else if(propName.startsWith('_')) {
                def restOfPropName = propName[1..-1]
                metaProperty = obj.metaClass.getMetaProperty restOfPropName
                if(metaProperty && 
                   (Boolean == metaProperty.type || Boolean.TYPE == metaProperty.type) &&
                   !source.containsKey('restOfPropName')) {
                    setPropertyValue obj, source, restOfPropName, false, listener
                }
            }
        }
    }

    protected Class<?> getReferencedTypeForCollection(String propertyName, Object obj) {
        Class contentType
        def clazz = obj.getClass()
        def field = clazz.getDeclaredField(propertyName)
        def genericType = field.genericType
        if(genericType instanceof ParameterizedType) {
            contentType = ((ParameterizedType)genericType).getActualTypeArguments()[0]
        }
        contentType
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected addElementToCollectionAt(obj, String propertyName, Collection collection, index, val) {
        // TODO
        if(collection instanceof ListOrderedSet) {
            collection.add Math.min(index, collection.size()), val
        } else {
            collection[index] = val
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected Map initializeMap(obj, String propertyName) {
        if(obj[propertyName] == null) {
            obj[propertyName] = [:]
        }
        obj[propertyName]
    }
    
    @CompileStatic(TypeCheckingMode.SKIP)
    protected Collection initializeCollection(obj, String propertyName, Class type) {
        if(obj[propertyName] == null) {
            if(List.isAssignableFrom(type)) {
                obj[propertyName] = new ArrayList()
            } else if (Set.isAssignableFrom(type)) {
                obj[propertyName] = ListOrderedSet.decorate([] as Set)
            }
        }
        obj[propertyName]
    }

    /**
     * Get a ValueConverter for field
     * 
     * @param field The field to retrieve a converter for
     * @param formattingValue The format that the converter will use
     * @return a ValueConverter for field which uses formattingValue for its format
     * @see BindingFormat
     */
    protected ValueConverter getFormattedConverter(Field field, String formattingValue) {
        def converter
        def formattedConverter = formattedValueConvertersionHelpers[field.type]
        if(formattedConverter) {
            converter = { Map source ->
                def value = source[field.name]
                formattedConverter.convert (value, formattingValue)
            } as ValueConverter
        }
        converter
    }
    
    protected ValueConverter getValueConverterForField(obj, String propName) {
        def converter
        try {
            def field = obj.getClass().getDeclaredField propName
            if(field) {
                def annotation = field.getAnnotation BindUsing
                if(annotation) {
                    def valueClass = annotation.value()
                    if(Closure.isAssignableFrom(valueClass)) {
                        Closure closure = (Closure)valueClass.newInstance(null, null)
                        converter = new ClosureValueConverter(converterClosure: closure.curry(obj), targetType: field.type)
                    }
                } else {
                    annotation = field.getAnnotation BindingFormat
                    if(annotation) {
                        converter = getFormattedConverter field, annotation.value()
                    }
                }
            }
        } catch (Exception e) {
        }
        converter
    }

    protected ValueConverter getValueConverterForClass(obj, String propName) {
        def converter
        def annotation = obj.getClass().getAnnotation BindUsing
        if(annotation) {
            def valueClass = annotation.value()
            if(BindingHelper.isAssignableFrom(valueClass)) {
                BindingHelper dataConverter = (BindingHelper)valueClass.newInstance()
                converter = new ClosureValueConverter(converterClosure: { Map it -> dataConverter.getPropertyValue(obj, propName, it) })
            }
        }
        converter
    }

    protected ValueConverter getValueConverter(obj, String propName) {
        def converter = getValueConverterForField obj, propName
        if(!converter) {
            converter = getValueConverterForClass obj, propName
        }
        converter
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected convertStringToEnum(Class<? extends Enum> enumClass, String value) {
        Enum enumValue = null
        try {
            enumValue = enumClass.valueOf(value)
        } catch (IllegalArgumentException iae) {}
        
        enumValue
    }
    protected setPropertyValue(obj, Map source, String propName, propertyValue, DataBindingListener listener) {
        def converter = getValueConverter obj, propName

        if(converter) {
            propertyValue = converter.convert source
        }
        if(listener == null || listener.beforeBinding(obj, propName, propertyValue) != false) {
            def metaProperty = obj.metaClass.getMetaProperty(propName)
            def propertyType
            if(metaProperty instanceof MetaBeanProperty) {
                def mbp = (MetaBeanProperty)metaProperty
                propertyType = mbp.field?.type
            }
            if(propertyType == null) {
                propertyType = metaProperty.type
            }

            if(propertyValue == null || propertyType == Object || propertyType.isAssignableFrom(propertyValue.getClass())) {
                obj[propName] = propertyValue
            } else if(propertyValue instanceof List && 
//                      !propertyValue instanceof ListOrderedSet &&
                      Set.isAssignableFrom(propertyType) &&
                      !SortedSet.isAssignableFrom(propertyType)) {
                addElementsToCollection(obj, propName, propertyValue, true)
            } else if(Enum.isAssignableFrom(propertyType) && propertyValue instanceof String) {
                obj[propName] = convertStringToEnum(propertyType, propertyValue)
            } else {
                try {
                    if(propertyValue instanceof Map) {
                        if(Collection.isAssignableFrom(propertyType) && 
                            propertyValue.size() == 1 &&
                            ((Map)propertyValue)[propertyValue.keySet()[0]] instanceof List) {
                            def key = propertyValue.keySet()[0]
                            List list = (List)((Map)propertyValue)[key]
                            addElementsToCollection(obj, propName, list)
                        } else {
                            initializeProperty(obj, propName, propertyType, source)
                            bind obj[propName], propertyValue
                        }
                    } else {
                        obj[propName] = convert(propertyType, propertyValue)
                    }
                } catch (Exception e) {
                    if(listener) {
                        def error = new SimpleBindingError(obj, propName, propertyValue, e.cause ?: e)
                        listener.bindingError error
                    }
                }
            }
        } else if(listener != null && propertyValue instanceof Map && obj[propName] != null) {
            bind obj[propName], propertyValue
        }
        listener?.afterBinding obj, propName
    }

    private void addElementsToCollection(obj, String collectionPropertyName, Collection collection, boolean removeExistingElements = false) {
        Class propertyType = obj.metaClass.getMetaProperty(collectionPropertyName).type
        def referencedType = getReferencedTypeForCollection(collectionPropertyName, obj)
        def coll = initializeCollection(obj, collectionPropertyName, propertyType)
        if(removeExistingElements == true) {
            coll.clear()
        }
        collection?.each { element ->
            if(referencedType.isAssignableFrom(element.getClass())) {
                coll << element
            } else {
                coll << convert(referencedType, element)
            }
        }
    }

    protected initializeProperty(obj, String propName, Class propertyType, Map<String, Object> source) {
        obj[propName] = propertyType.newInstance()
    }

    protected convert(Class typeToConvertTo, value) {
        if(conversionHelpers.containsKey(typeToConvertTo)) {
            return conversionHelpers.get(typeToConvertTo).convert(value)
        } else if(conversionService?.canConvert(value.getClass(), typeToConvertTo)) {
            return conversionService.convert(value, typeToConvertTo)
        } else if(Collection.isAssignableFrom(typeToConvertTo) && value instanceof String[]) {
            if(Set == typeToConvertTo) {
                return value as Set
            } else if(List == typeToConvertTo) {
                return value as List
            }
        }
        typeToConvertTo.newInstance value
    }
}
