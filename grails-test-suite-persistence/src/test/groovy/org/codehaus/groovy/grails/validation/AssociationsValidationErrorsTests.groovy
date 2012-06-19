package org.codehaus.groovy.grails.validation

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests

/**
 * @author Adrian Stachowiak
 * @since 1.0
 */
class AssociationsValidationErrorsTests extends AbstractGrailsHibernateTests {

  protected void onSetUp() {
    gcl.parseClass('''
import grails.persistence.*

@Entity
class BaseTest {

    String name

    List listTests = new ArrayList()
    Map mapTests
    Set setTests

    static hasMany = [listTests:ListTest, mapTests:MapTest, setTests:SetTest]

    static constraints = {
        name nullable:false, blank:false
    }
}

@Entity
class ListTest {
    String name

    List list2Tests = new ArrayList()

    static belongsTo = BaseTest

    static hasMany = [list2Tests:List2Test]

    static constraints = {
        name nullable:false, blank:false
    }
}

@Entity
class List2Test {
    String name

    static belongsTo = ListTest

    static constraints = {
        name nullable:false, blank:false
    }
}

@Entity
class MapTest {
    String name

    Map map2Tests

    static belongsTo = BaseTest

    static hasMany = [map2Tests:Map2Test]

    static constraints = {
        name nullable:false, blank:false
    }
}

@Entity
class Map2Test {
    String name

    static belongsTo = MapTest

    static constraints = {
        name nullable:false, blank:false
    }
}

@Entity
class SetTest {
    String name

    static belongsTo = BaseTest

    static constraints = {
        name nullable:false, blank:false
    }
}
''')
  }

  void testListValidation() {
    def baseTest = ga.getDomainClass('BaseTest').newInstance()
    baseTest.name = 'Base Name'

    def listTest0 = ga.getDomainClass('ListTest').newInstance()
    listTest0.name = 'LIST TEST'

    def listTest1 = ga.getDomainClass('ListTest').newInstance()

    baseTest.addToListTests(listTest0)
    baseTest.addToListTests(listTest1)
    baseTest.validate()

    assertNotNull("Error not found for field listTests[1].name, errors were: ${baseTest.errors}", baseTest.errors.getFieldError('listTests[1].name'))
  }

  void test2ndLevelListValidation() {
    def baseTest = ga.getDomainClass('BaseTest').newInstance()
    baseTest.name = 'Base Name'

    def listTest = ga.getDomainClass('ListTest').newInstance()
    listTest.name = 'LIST TEST'

    def list2Test0 = ga.getDomainClass('List2Test').newInstance()
    list2Test0.name = 'LIST2 TEST'

    def list2Test1 = ga.getDomainClass('List2Test').newInstance()

    listTest.addToList2Tests(list2Test0)
    listTest.addToList2Tests(list2Test1)
    baseTest.addToListTests(listTest)
    baseTest.validate()

    assertNotNull("Error not found for field listTests[0].list2Tests[1].name, errors were: ${baseTest.errors}", baseTest.errors.getFieldError('listTests[0].list2Tests[1].name'))
  }


  void testMapValidation() {
    def baseTest = ga.getDomainClass('BaseTest').newInstance()
    baseTest.name = 'Base Name'

    def mapTest0 = ga.getDomainClass('MapTest').newInstance()
    mapTest0.name = 'MAP TEST'

    def mapTest1 = ga.getDomainClass('MapTest').newInstance()

    def key = 'key with spaces and non-standard chars $!@3��'
    baseTest.mapTests = [MKEY0:mapTest0, "${key}":mapTest1]
    baseTest.validate()

    assertNotNull("Error not found for field mapTests[${key}].name, errors were: ${baseTest.errors}", baseTest.errors.getFieldError("mapTests[${key}].name"))
  }

  void test2ndLevelMapValidation() {
    def baseTest = ga.getDomainClass('BaseTest').newInstance()
    baseTest.name = 'Base Name'

    def mapTest = ga.getDomainClass('MapTest').newInstance()
    mapTest.name = 'MAP TEST'

    def map2Test0 = ga.getDomainClass('Map2Test').newInstance()
    map2Test0.name = 'MAP2 TEST'

    def map2Test1 = ga.getDomainClass('Map2Test').newInstance()

    mapTest.map2Tests = [M2KEY0:map2Test0, M2KEY1:map2Test1]
    baseTest.mapTests = [MKEY:mapTest]
    baseTest.validate()

    assertNotNull("Error not found for field mapTests[MKEY].map2Tests[M2KEY1].name, errors were: ${baseTest.errors}", baseTest.errors.getFieldError('mapTests[MKEY].map2Tests[M2KEY1].name'))
  }

  void testSetValidation() {
    def baseTest = ga.getDomainClass('BaseTest').newInstance()
    baseTest.name = 'Base Name'

    def setTest0 = ga.getDomainClass('SetTest').newInstance()
    setTest0.name = 'SET TEST'

    def setTest1 = ga.getDomainClass('SetTest').newInstance()

    baseTest.setTests = [setTest1, setTest0]
    baseTest.validate()

    int index = baseTest.setTests.findIndexOf { it == setTest1 }

    assertNotNull("Error not found for field setTests[${index}].name, errors were: ${baseTest.errors}", baseTest.errors.getFieldError("setTests[${index}].name"))
  }
}