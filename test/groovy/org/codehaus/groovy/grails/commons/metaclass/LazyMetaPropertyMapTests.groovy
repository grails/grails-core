package org.codehaus.groovy.grails.commons.metaclass
/**
 * @author Graeme Rocher
 */
class LazyMetaPropertyMapTests extends GroovyTestCase {

    void testOverridePropertiesRecursionBug() {
        PropertyMapTest.metaClass.getProperties = {-> new LazyMetaPropertyMap(delegate) }

        def obj = new PropertyMapTest(name:"Homer", age:45)

        assertFalse obj.properties.containsKey('properties')
        assertEquals 4, obj.properties.size()
        obj.properties.each {
            println "${it.key} : ${it.value}"
        }
    }

    void testSize() {
        def map = new LazyMetaPropertyMap(new PropertyMapTest())

        assertEquals 4, map.size()
    }

    void testIsEmpty() {
        def map = new LazyMetaPropertyMap(new PropertyMapTest())

        assertFalse map.isEmpty()        
    }

    void testContainsKey() {
        def map = new LazyMetaPropertyMap(new PropertyMapTest())

        assertTrue map.containsKey("name")
        assertTrue map.containsKey("age")
        assertFalse map.containsKey("fo")

    }

    void testContainsValue() {
        def map = new LazyMetaPropertyMap(new PropertyMapTest(name:"Homer", age:45))

        assertTrue map.containsValue("Homer")
        assertTrue map.containsValue(45)
        assertFalse map.containsValue("fo")

    }

    void testGet() {
        def map = new LazyMetaPropertyMap(new PropertyMapTest(name:"Homer", age:45))

        assertEquals "Homer", map.get("name")
        assertEquals "Homer", map.name
        assertEquals "Homer", map['name']

        assertEquals 45, map.get("age")
        assertEquals 45, map.age
        assertEquals 45, map['age']

        assertNull map.foo
        assertNull map['foo']
        assertNull map.get('foo')

    }

    void testPut() {
        def map = new LazyMetaPropertyMap(new PropertyMapTest(name:"Bart", age:11))

        map.name = "Homer"
        map.age = 45
        assertEquals "Homer", map.get("name")
        assertEquals "Homer", map.name
        assertEquals "Homer", map['name']

        def old = map.put("name", "lisa")
        assertEquals "Homer", old

        assertEquals "lisa", map.name

    }

    void testKeySet() {
        def map = new LazyMetaPropertyMap(new PropertyMapTest(name:"Bart", age:11))

        def keys = map.keySet()

        assertTrue keys.contains("name")
        assertTrue keys.contains("age")
        assertTrue keys.contains("class")
    }


    void testValues() {
        def map = new LazyMetaPropertyMap(new PropertyMapTest(name:"Bart", age:11))

        def values = map.values()

        assertTrue values.contains("Bart")
        assertTrue values.contains(11)
        assertTrue values.contains(PropertyMapTest)
    }


}
class PropertyMapTest {
    String name
    Integer age
}