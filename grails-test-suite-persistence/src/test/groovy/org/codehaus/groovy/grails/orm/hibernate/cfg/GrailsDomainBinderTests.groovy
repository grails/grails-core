/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.orm.hibernate.cfg

import java.lang.reflect.Field

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.plugins.MockGrailsPluginManager
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.codehaus.groovy.grails.validation.TestClass
import org.hibernate.cfg.ImprovedNamingStrategy
import org.hibernate.mapping.Bag
import org.hibernate.mapping.Column
import org.hibernate.mapping.ForeignKey
import org.hibernate.mapping.PersistentClass
import org.hibernate.mapping.Property
import org.hibernate.mapping.SimpleValue
import org.hibernate.mapping.Table
import org.hibernate.mapping.UniqueKey
import org.hibernate.util.StringHelper
import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl

/**
 * @author Jason Rudolph
 * @author Sergey Nebolsin
 * @since 0.4
 */
class GrailsDomainBinderTests extends GroovyTestCase {

    private static final String CACHED_MAP = '''
class Area {
    Long id
    Long version
    Map names
    static mapping = {
        names cache: true
    }
}
'''

    private static final String ONE_TO_ONE_CLASSES_DEFINITION = '''
class Species {
    Long id
    Long version
    String name
}
class Pet {
    Long id
    Long version
    Species species
}'''

    private static final String ONE_TO_MANY_CLASSES_DEFINITION = '''
class Visit {
    Long id
    Long version
    String description
}
class Pet {
    Long id
    Long version
    Set visits
    static hasMany = [visits:Visit]
    static mapping = { visits joinTable:false, nullable:false }
}
'''

    private static final String BAG_ONE_TO_MANY_CLASSES_DEFINITION = '''
class Bagged {
    Long id
    Long version
    String description
}
class Bagger {
    Long id
    Long version
    Collection bagged
    static hasMany = [bagged: Bagged]
}'''

    private static final String MANY_TO_MANY_CLASSES_DEFINITION = '''
class Specialty {
    Long id
    Long version
    String name
    Set vets
    static hasMany = [vets:Vet]
    static belongsTo = Vet
}
class Vet {
    Long id
    Long version
    Set specialities
    static hasMany = [specialities:Specialty]
}
'''

    private static final String BAG_MANY_TO_MANY_CLASSES_DEFINITION = '''
class ManyBagged {
    Long id
    Long version
    String name
    Collection baggers
    static hasMany = [baggers: ManyBagger]
    static belongsTo = ManyBagger
}
class ManyBagger {
    Long id
    Long version
    Collection bagged
    static hasMany = [bagged: ManyBagged]
}'''

    private static final String MULTI_COLUMN_USER_TYPE_DEFINITION = '''
import org.codehaus.groovy.grails.orm.hibernate.cfg.*
class Item {
    Long id
    Long version
    String name
    MyType other
    MonetaryAmount price
    static mapping = {
        name column: 's_name', sqlType: 'text'
        other type: MyUserType, sqlType: 'wrapper-characters', params:[param1: 'myParam1', param2: 'myParam2']
        price type: MonetaryAmountUserType, {
            column name: 'value'
            column name: 'currency_code', sqlType: 'text'
        }
    }
}
'''

    private static final String UNIQUE_PROPERTIES = '''
class User {
    Long id
    Long version
    String login
    String group
    String camelCased
    String employeeID
    static constraints = {
        employeeID(unique:true)
        group(unique:'camelCased')
        login(unique:['group','camelCased'])
   }
}'''

    private static final String TABLE_PER_HIERARCHY = '''
class TablePerHierarchySuperclass {
    Long id
    Long version
    String stringProperty
    String optionalStringProperty
    ProductStatus someProductStatus
    ProductStatus someOptionalProductStatus
    static constraints = {
        optionalStringProperty nullable: true
        someOptionalProductStatus nullable: true
    }
}
enum ProductStatus {
    GOOD, BAD
}
class TablePerHierarchySubclass extends TablePerHierarchySuperclass {
    Long id
    Long version
    ProductStatus productStatus
    String productName
    Integer productCount
    ProductStatus optionalProductStatus
    String optionalProductName
    Integer optionalProductCount
    static constraints = {
        optionalProductName nullable: true
        optionalProductCount nullable: true
        optionalProductStatus nullable: true
    }
}
'''

    private static final String TABLE_PER_SUBCLASS = '''
class TablePerSubclassSuperclass {
    Long id
    Long version
    String stringProperty
    String optionalStringProperty
    ProductStatus someProductStatus
    ProductStatus someOptionalProductStatus
    static constraints = {
        optionalStringProperty nullable: true
        someOptionalProductStatus nullable: true
    }
    static mapping = {
        tablePerHierarchy false
    }
}
enum ProductStatus {
    GOOD, BAD
}
class TablePerSubclassSubclass extends TablePerSubclassSuperclass {
    Long id
    Long version
    ProductStatus productStatus
    String productName
    Integer productCount
    ProductStatus optionalProductStatus
    String optionalProductName
    Integer optionalProductCount
    static constraints = {
        optionalProductName nullable: true
        optionalProductCount nullable: true
        optionalProductStatus nullable: true
    }
}
'''

    private GroovyClassLoader cl = new GroovyClassLoader()

    @Override
    protected void setUp() {
        super.setUp()
        ExpandoMetaClass.enableGlobally()
        MockGrailsPluginManager pluginManager = new MockGrailsPluginManager()
        PluginManagerHolder.setPluginManager(pluginManager)
    }

    @Override
    protected void tearDown() {
        super.tearDown()
        GrailsDomainBinder.NAMING_STRATEGIES.clear()
        GrailsDomainBinder.NAMING_STRATEGIES.put(
              GrailsDomainClassProperty.DEFAULT_DATA_SOURCE, ImprovedNamingStrategy.INSTANCE)
        PluginManagerHolder.setPluginManager(null)
    }

    /**
     * Test for GRAILS-4200
     */
    void testEmbeddedComponentMapping() {
        DefaultGrailsDomainConfiguration config = getDomainConfig('''
class Widget {
    Long id
    Long version
    EmbeddedWidget ew
    static embedded = ['ew']
}
class EmbeddedWidget {
   String ew
   static mapping = {
       ew column: 'widget_name'
   }
}''')

        Table tableMapping = getTableMapping("widget", config)
        Column embeddedComponentMappedColumn = tableMapping.getColumn(new Column("widget_name"))
        assertNotNull(embeddedComponentMappedColumn)
        assertEquals("widget_name", embeddedComponentMappedColumn.name)
    }

    void testLengthProperty() {
        DefaultGrailsDomainConfiguration config = getDomainConfig('''
class Widget {
    Long id
    Long version
    String name
    String description
    static mapping = {
        name column: 's_name', sqlType: 'text', length: 42
        description column: 's_description', sqlType: 'text'
    }
}''')
        Table tableMapping = getTableMapping("widget", config)
        Column nameColumn = tableMapping.getColumn(new Column("s_name"))
        Column descriptionColumn = tableMapping.getColumn(new Column("s_description"))
        assertEquals(42, nameColumn.length)
        assertEquals(Column.DEFAULT_LENGTH, descriptionColumn.length)
    }

    void testUniqueProperty() {
        DefaultGrailsDomainConfiguration config = getDomainConfig('''
class Widget {
    Long id
    Long version
    String name
    String description
    static mapping = {
        name unique: true
    }
}''')

        Table tableMapping = getTableMapping("widget", config)
        Column nameColumn = tableMapping.getColumn(new Column("name"))
        Column descriptionColumn = tableMapping.getColumn(new Column("description"))
        assertTrue nameColumn.isUnique()
        assertFalse descriptionColumn.isUnique()
    }

    void testPrecisionProperty() {
        DefaultGrailsDomainConfiguration config = getDomainConfig('''
class Widget {
    Long id
    Long version
    Float width
    Float height
    static mapping = {
        width precision: 3
    }
}''')
        Table tableMapping = getTableMapping("widget", config)
        Column heightColumn = tableMapping.getColumn(new Column("height"))
        Column widthColumn = tableMapping.getColumn(new Column("width"))
        assertEquals(3, widthColumn.precision)
        assertEquals(Column.DEFAULT_PRECISION, heightColumn.precision)
    }

    void testScaleProperty() {
        DefaultGrailsDomainConfiguration config = getDomainConfig('''
class Widget {
    Long id
    Long version
    Float width
    Float height
    static mapping = {
        width scale: 7
    }
}''')

        Table tableMapping = getTableMapping("widget", config)
        Column heightColumn = tableMapping.getColumn(new Column("height"))
        Column widthColumn = tableMapping.getColumn(new Column("width"))
        assertEquals(7, widthColumn.scale)
        assertEquals(Column.DEFAULT_SCALE, heightColumn.scale)
    }

    void testCachedMapProperty() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(CACHED_MAP)
        Table table = getTableMapping("area_names", config)
        assertEquals(255, table.getColumn(new Column("names_elt")).length)
    }

    void testColumnNullabilityWithTablePerHierarchy() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(TABLE_PER_HIERARCHY)

        // with tablePerHierarchy all columns related to properties defined in subclasses must
        // be nullable to allow instances of other classes in the hierarchy to be persisted
        assertColumnNullable("table_per_hierarchy_superclass", "product_status", config)
        assertColumnNullable("table_per_hierarchy_superclass", "product_name", config)
        assertColumnNullable("table_per_hierarchy_superclass", "product_count", config)
        assertColumnNullable("table_per_hierarchy_superclass", "optional_product_status", config)
        assertColumnNullable("table_per_hierarchy_superclass", "optional_product_name", config)
        assertColumnNullable("table_per_hierarchy_superclass", "optional_product_count", config)

        // columns related to required properties in the root class should not be nullable
        assertColumnNotNullable("table_per_hierarchy_superclass", "string_property", config)
        assertColumnNotNullable("table_per_hierarchy_superclass", "some_product_status", config)

        // columns related to optional properties in the root class should be nullable
        assertColumnNullable("table_per_hierarchy_superclass", "optional_string_property", config)
        assertColumnNullable("table_per_hierarchy_superclass", "some_optional_product_status", config)
    }

    void testColumnNullabilityWithTablePerSubclass() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(TABLE_PER_SUBCLASS)

        // with tablePerSubclass columns related to required properties defined in subclasses
        // should not be nullable
        assertColumnNotNullable("table_per_subclass_subclass", "product_status", config)
        assertColumnNotNullable("table_per_subclass_subclass", "product_name", config)
        assertColumnNotNullable("table_per_subclass_subclass", "product_count", config)

        // with tablePerSubclass columns related to optional properties defined in subclasses
        // should be nullable
        assertColumnNullable("table_per_subclass_subclass", "optional_product_status", config)
        assertColumnNullable("table_per_subclass_subclass", "optional_product_name", config)
        assertColumnNullable("table_per_subclass_subclass", "optional_product_count", config)

        // columns related to required properties in the root class should not be nullable
        assertColumnNotNullable("table_per_subclass_superclass", "string_property", config)
        assertColumnNotNullable("table_per_subclass_superclass", "some_product_status", config)

        // columns related to optional properties in the root class should be nullable
        assertColumnNullable("table_per_subclass_superclass", "optional_string_property", config)
        assertColumnNullable("table_per_subclass_superclass", "some_optional_product_status", config)
    }

    void testUniqueConstraintGeneration() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(UNIQUE_PROPERTIES)
        assertEquals("Tables created", 1, getTableCount(config))
        List expectedKeyColumns1 = [new Column("camel_cased"), new Column("group"), new Column("login")]
        List expectedKeyColumns2 = [new Column("camel_cased"), new Column("group")]
        Table mapping = config.tableMappings.next()
        int cnt = 0
        boolean found1 = false, found2 = false
        for (UniqueKey key in mapping.uniqueKeyIterator) {
            List keyColumns = key.columns
            if (keyColumns.equals(expectedKeyColumns1)) {
                found1 = true
            }
            if (keyColumns.equals(expectedKeyColumns2)) {
                found2 = true
            }
            cnt++
        }
        assertEquals(2, cnt)
        assertTrue mapping.getColumn(new Column("employeeID")).isUnique()
        assertTrue found1
        assertTrue found2
    }

    void testInsertableHibernateMapping() {
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
            cl.parseClass('''
class TestInsertableDomain {
    Long id
    Long version
    String testString1
    String testString2

    static mapping = {
       testString1 insertable:false
       testString2 insertable:true
    }
}'''))

        DefaultGrailsDomainConfiguration config = getDomainConfig(cl, [domainClass.clazz])
        Field privateDomainClasses = DefaultGrailsDomainConfiguration.getDeclaredField("domainClasses")
        privateDomainClasses.setAccessible(true)

        PersistentClass persistentClass = config.getClassMapping("TestInsertableDomain")

        assertFalse persistentClass.getProperty("testString1").isInsertable()
        assertTrue persistentClass.getProperty("testString2").isInsertable()
    }

    void testUpdateableHibernateMapping() {
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
            cl.parseClass('''
class TestInsertableDomain {
    Long id
    Long version
    String testString1
    String testString2

    static mapping = {
       testString1 updateable:false
       testString2 updateable:true
    }
}'''))

        DefaultGrailsDomainConfiguration config = getDomainConfig(cl, [domainClass.clazz])

        PersistentClass persistentClass = config.getClassMapping("TestInsertableDomain")

        assertFalse persistentClass.getProperty("testString1").isUpdateable()
        assertTrue persistentClass.getProperty("testString2").isUpdateable()
    }

    void testInsertableUpdateableHibernateMapping() {
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
            cl.parseClass('''
class TestInsertableUpdateableDomain {
    Long id
    Long version
    String testString1
    String testString2

    static mapping = {
       testString1 insertable:false, updateable:false
       testString2 updateable:false, insertable:false
    }
}'''))

        DefaultGrailsDomainConfiguration config = getDomainConfig(cl, [domainClass.clazz])

        PersistentClass persistentClass = config.getClassMapping("TestInsertableUpdateableDomain")

        Property property1 = persistentClass.getProperty("testString1")
        assertFalse property1.isInsertable()
        assertFalse property1.isUpdateable()

        Property property2 = persistentClass.getProperty("testString2")
        assertFalse property2.isUpdateable()
        assertFalse property2.isInsertable()
    }

    void testOneToOneBindingTables() {
        assertEquals("Tables created", 2, getTableCount(getDomainConfig(ONE_TO_ONE_CLASSES_DEFINITION)))
    }

    void testOneToOneBindingFk() {
        assertForeignKey("species", "pet", getDomainConfig(ONE_TO_ONE_CLASSES_DEFINITION))
    }

    void testOneToOneBindingFkColumn() {
        assertColumnNotNullable("pet", "species_id", getDomainConfig(ONE_TO_ONE_CLASSES_DEFINITION))
    }

    void testOneToManyBindingTables() {
        assertEquals("Tables created", 2, getTableCount(getDomainConfig(ONE_TO_MANY_CLASSES_DEFINITION)))
    }

    void testOneToManyBindingFk() {
        assertForeignKey("pet", "visit", getDomainConfig(ONE_TO_MANY_CLASSES_DEFINITION))
    }

/*    void testOneToManyBindingFkColumn() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(ONE_TO_MANY_CLASSES_DEFINITION)
        assertColumnNotNullable("visit", "pet_visits_id", config)
    }*/

    void testManyToManyBindingTables() {
        assertEquals("Tables created", 3, getTableCount(getDomainConfig(MANY_TO_MANY_CLASSES_DEFINITION)))
    }

    void testManyToManyBindingPk() {
        Table table = getTableMapping("vet_specialities", getDomainConfig(MANY_TO_MANY_CLASSES_DEFINITION))
        assertNotNull("VET_SPECIALTY table has a PK", table.primaryKey)
        assertEquals("VET_SPECIALTY table has a 2 column PK", 2, table.primaryKey.columns.size())
    }

    void testManyToManyBindingFk() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(MANY_TO_MANY_CLASSES_DEFINITION)
        assertForeignKey("specialty", "vet_specialities", config)
        assertForeignKey("vet", "vet_specialities", config)
    }

    void testManyToManyBindingFkColumn() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(MANY_TO_MANY_CLASSES_DEFINITION)
        assertColumnNotNullable("vet_specialities", "vet_id", config)
        assertColumnNotNullable("vet_specialities", "specialty_id", config)
    }

    /**
     * Tests that single- and multi-column user type mappings work
     * correctly. Also Checks that the "sqlType" property is honoured.
     */
    void testUserTypeMappings() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(MULTI_COLUMN_USER_TYPE_DEFINITION)
        PersistentClass persistentClass = config.getClassMapping("Item")

        // First check the "name" property and its associated column.
        Property nameProperty = persistentClass.getProperty("name")
        assertEquals(1, nameProperty.columnSpan)
        assertEquals("name", nameProperty.name)

        Column column = nameProperty.columnIterator.next()
        assertEquals("s_name", column.name)
        assertEquals("text", column.sqlType)

        // Next the "other" property.
        Property otherProperty = persistentClass.getProperty("other")
        assertEquals(1, otherProperty.columnSpan)
        assertEquals("other", otherProperty.name)

        column = otherProperty.columnIterator.next()
        assertEquals("other", column.name)
        assertEquals("wrapper-characters", column.sqlType)
        assertEquals(MyUserType.name, column.value.type.name)
        assertTrue(column.value instanceof SimpleValue)
        SimpleValue v = column.value
        assertEquals("myParam1", v.typeParameters.get("param1"))
        assertEquals("myParam2", v.typeParameters.get("param2"))

        // And now for the "price" property, which should have two columns.
        Property priceProperty = persistentClass.getProperty("price")
        assertEquals(2, priceProperty.columnSpan)
        assertEquals("price", priceProperty.name)

        Iterator<?> colIter = priceProperty.columnIterator
        column = colIter.next()
        assertEquals("value", column.name)
        assertNull("SQL type should have been 'null' for 'value' column.", column.sqlType)

        column = colIter.next()
        assertEquals("currency_code", column.name)
        assertEquals("text", column.sqlType)
    }

    void testDomainClassBinding() {
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
            cl.parseClass('''
class BinderTestClass {
    Long id
    Long version

    String firstName
    String lastName
    String comment
    Integer age
    boolean active = true

    static constraints = {
        firstName(nullable:true,size:4..15)
        lastName(nullable:false)
        age(nullable:true)
    }
}'''))

        DefaultGrailsDomainConfiguration config = getDomainConfig(cl, cl.loadedClasses)

        // Test database mappings
        PersistentClass persistentClass = config.getClassMapping("BinderTestClass")
        assertTrue("Property [firstName] must be optional in db mapping", persistentClass.getProperty("firstName").isOptional())
        assertFalse("Property [lastName] must be required in db mapping", persistentClass.getProperty("lastName").isOptional())
        // Property must be required by default
        assertFalse("Property [comment] must be required in db mapping", persistentClass.getProperty("comment").isOptional())

        // Test properties
        assertTrue("Property [firstName] must be optional", domainClass.getPropertyByName("firstName").isOptional())
        assertFalse("Property [lastName] must be optional", domainClass.getPropertyByName("lastName").isOptional())
        assertFalse("Property [comment] must be required", domainClass.getPropertyByName("comment").isOptional())
        assertTrue("Property [age] must be optional", domainClass.getPropertyByName("age").isOptional())
    }

    void testForeignKeyColumnBinding() {
        GrailsDomainClass oneClass = new DefaultGrailsDomainClass(
                cl.parseClass('''
class TestOneSide {
    Long id
    Long version
    String name
    String description
}'''))
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
                cl.parseClass('''
class TestManySide {
    Long id
    Long version
    String name
    TestOneSide testOneSide

    static mapping = {
        columns {
            testOneSide column:'EXPECTED_COLUMN_NAME'
        }
    }
}'''))

        DefaultGrailsDomainConfiguration config = getDomainConfig(cl,
              [oneClass.clazz, domainClass.clazz])

        PersistentClass persistentClass = config.getClassMapping("TestManySide")

        Column column = persistentClass.getProperty("testOneSide").columnIterator.next()
        assertEquals("EXPECTED_COLUMN_NAME", column.name)
    }

    /**
     * @see GrailsDomainBinder#bindStringColumnConstraints(Column, ConstrainedProperty)
     */
    void testBindStringColumnConstraints() {
        // Verify that the correct length is set when a maxSize constraint is applied
        ConstrainedProperty constrainedProperty = getConstrainedStringProperty()
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_SIZE_CONSTRAINT, 30)
        assertColumnLength(constrainedProperty, 30)

        // Verify that the correct length is set when a size constraint is applied
        constrainedProperty = getConstrainedStringProperty()
        constrainedProperty.applyConstraint(ConstrainedProperty.SIZE_CONSTRAINT, new IntRange(6, 32768))
        assertColumnLength(constrainedProperty, 32768)

        // Verify that the default length remains intact when no size-related constraints are applied
        constrainedProperty = getConstrainedStringProperty()
        assertColumnLength(constrainedProperty, Column.DEFAULT_LENGTH)

        // Verify that the correct length is set when an inList constraint is applied
        constrainedProperty = getConstrainedStringProperty()
        List validValuesList = ["Groovy", "Java", "C++"]
        constrainedProperty.applyConstraint(ConstrainedProperty.IN_LIST_CONSTRAINT, validValuesList)
        assertColumnLength(constrainedProperty, 6)

        // Verify that the correct length is set when a maxSize constraint *and* an inList constraint are *both* applied
        constrainedProperty = getConstrainedStringProperty()
        constrainedProperty.applyConstraint(ConstrainedProperty.IN_LIST_CONSTRAINT, validValuesList)
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_SIZE_CONSTRAINT, 30)
        assertColumnLength(constrainedProperty, 30)
    }

    /**
     * @see GrailsDomainBinder#bindNumericColumnConstraints(Column, ConstrainedProperty)
     */
    void testBindNumericColumnConstraints() {
        ConstrainedProperty constrainedProperty = getConstrainedBigDecimalProperty()
        // maxSize and minSize constraint has the number with the most digits
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_SIZE_CONSTRAINT, 123)
        constrainedProperty.applyConstraint(ConstrainedProperty.MIN_SIZE_CONSTRAINT, 0)
        assertColumnPrecisionAndScale(constrainedProperty, Column.DEFAULT_PRECISION, Column.DEFAULT_SCALE)

        // Verify that the correct precision is set when the max constraint has the number with the most digits
        constrainedProperty = getConstrainedBigDecimalProperty()
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new BigDecimal("123.45"))
        constrainedProperty.applyConstraint(ConstrainedProperty.MIN_CONSTRAINT, new BigDecimal("0"))
        assertColumnPrecisionAndScale(constrainedProperty, 5, Column.DEFAULT_SCALE)

        // Verify that the correct precision is set when the minSize constraint has the number with the most digits
        constrainedProperty = getConstrainedBigDecimalProperty()
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new BigDecimal("123"))
        constrainedProperty.applyConstraint(ConstrainedProperty.MIN_CONSTRAINT, new BigDecimal("-123.45"))
        assertColumnPrecisionAndScale(constrainedProperty, 5, Column.DEFAULT_SCALE)

        // Verify that the correct precision is set when the high value of a floating point range constraint has the number with the most digits
        constrainedProperty = getConstrainedBigDecimalProperty()
        constrainedProperty.applyConstraint(ConstrainedProperty.RANGE_CONSTRAINT, new ObjectRange(new BigDecimal("0"), new BigDecimal("123.45")))
        assertColumnPrecisionAndScale(constrainedProperty, 5, Column.DEFAULT_SCALE)

        // Verify that the correct precision is set when the low value of a floating point range constraint has the number with the most digits
        constrainedProperty = getConstrainedBigDecimalProperty()
        constrainedProperty.applyConstraint(ConstrainedProperty.RANGE_CONSTRAINT, new ObjectRange(new BigDecimal("-123.45"), new BigDecimal("123")))
        assertColumnPrecisionAndScale(constrainedProperty, 5, Column.DEFAULT_SCALE)

        // Verify that the correct scale is set when the scale constraint is specified in isolation
        constrainedProperty = getConstrainedBigDecimalProperty()
        constrainedProperty.applyConstraint(ConstrainedProperty.SCALE_CONSTRAINT, 4)
        assertColumnPrecisionAndScale(constrainedProperty, Column.DEFAULT_PRECISION, 4)

        // Verify that the precision is set correctly for a floating point number with a min/max constraint and a scale...
        //  1) where the min/max constraint includes fewer decimal places than the scale constraint
        constrainedProperty = getConstrainedBigDecimalProperty()
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new BigDecimal("123.45"))
        constrainedProperty.applyConstraint(ConstrainedProperty.MIN_CONSTRAINT, new BigDecimal("0"))
        constrainedProperty.applyConstraint(ConstrainedProperty.SCALE_CONSTRAINT, 3)
        assertColumnPrecisionAndScale(constrainedProperty, 6, 3) // precision (6) = number of integer digits in max constraint ("123.45") + scale (3)

        //  2) where the min/max constraint includes more decimal places than the scale constraint
        constrainedProperty = getConstrainedBigDecimalProperty()
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new BigDecimal("123.4567"))
        constrainedProperty.applyConstraint(ConstrainedProperty.MIN_CONSTRAINT, new BigDecimal("0"))
        constrainedProperty.applyConstraint(ConstrainedProperty.SCALE_CONSTRAINT, 3)
        assertColumnPrecisionAndScale(constrainedProperty, 7, 3) // precision (7) = number of digits in max constraint ("123.4567")

        // Verify that the correct precision is set when the only one of 'min' and 'max' constraint specified
        constrainedProperty = getConstrainedBigDecimalProperty()
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new BigDecimal("123.4567"))
        assertColumnPrecisionAndScale(constrainedProperty, Column.DEFAULT_PRECISION, Column.DEFAULT_SCALE)
        constrainedProperty = getConstrainedBigDecimalProperty()
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new BigDecimal("12345678901234567890.4567"))
        assertColumnPrecisionAndScale(constrainedProperty, 24, Column.DEFAULT_SCALE)
        constrainedProperty = getConstrainedBigDecimalProperty()
        constrainedProperty.applyConstraint(ConstrainedProperty.MIN_CONSTRAINT, new BigDecimal("-123.4567"))
        assertColumnPrecisionAndScale(constrainedProperty, Column.DEFAULT_PRECISION, Column.DEFAULT_SCALE)
        constrainedProperty = getConstrainedBigDecimalProperty()
        constrainedProperty.applyConstraint(ConstrainedProperty.MIN_CONSTRAINT, new BigDecimal("-12345678901234567890.4567"))
        assertColumnPrecisionAndScale(constrainedProperty, 24, Column.DEFAULT_SCALE)
    }

    void testDefaultNamingStrategy() {

        GrailsDomainClass oneClass = new DefaultGrailsDomainClass(
            cl.parseClass('''
class TestOneSide {
    Long id
    Long version
    String fooName
    String barDescriPtion
}'''))
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
            cl.parseClass('''
class TestManySide {
    Long id
    Long version
    TestOneSide testOneSide

    static mapping = {
        columns {
            testOneSide column:'EXPECTED_COLUMN_NAME'
        }
    }
}'''))

        DefaultGrailsDomainConfiguration config = getDomainConfig(cl,
                [oneClass.clazz, domainClass.clazz])

        PersistentClass persistentClass = config.getClassMapping("TestOneSide")
        assertEquals("test_one_side", persistentClass.table.name)

        Column column = persistentClass.getProperty("id").columnIterator.next()
        assertEquals("id", column.name)

        column = persistentClass.getProperty("version").columnIterator.next()
        assertEquals("version", column.name)

        column = persistentClass.getProperty("fooName").columnIterator.next()
        assertEquals("foo_name", column.name)

        column = persistentClass.getProperty("barDescriPtion").columnIterator.next()
        assertEquals("bar_descri_ption", column.name)

        persistentClass = config.getClassMapping("TestManySide")
        assertEquals("test_many_side", persistentClass.table.name)

        column = persistentClass.getProperty("id").columnIterator.next()
        assertEquals("id", column.name)

        column = persistentClass.getProperty("version").columnIterator.next()
        assertEquals("version", column.name)

        column = persistentClass.getProperty("testOneSide").columnIterator.next()
        assertEquals("EXPECTED_COLUMN_NAME", column.name)
    }

    void testCustomNamingStrategy() {

        // somewhat artificial in that it doesn't test that setting the property
        // in DataSource.groovy works, but that's handled in DataSourceConfigurationTests
        GrailsDomainBinder.configureNamingStrategy(CustomNamingStrategy)

        GrailsDomainClass oneClass = new DefaultGrailsDomainClass(
            cl.parseClass('''
class TestOneSide {
    Long id
    Long version
    String fooName
    String barDescriPtion
}'''))
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
            cl.parseClass('''
class TestManySide {
    Long id
    Long version
    TestOneSide testOneSide

    static mapping = {
        columns {
            testOneSide column:'EXPECTED_COLUMN_NAME'
        }
    }
}'''))

        DefaultGrailsDomainConfiguration config = getDomainConfig(cl,
                [oneClass.clazz, domainClass.clazz])

        PersistentClass persistentClass = config.getClassMapping("TestOneSide")
        assertEquals("table_TestOneSide", persistentClass.table.name)

        Column column = persistentClass.getProperty("id").columnIterator.next()
        assertEquals("col_id", column.name)

        column = persistentClass.getProperty("version").columnIterator.next()
        assertEquals("col_version", column.name)

        column = persistentClass.getProperty("fooName").columnIterator.next()
        assertEquals("col_fooName", column.name)

        column = persistentClass.getProperty("barDescriPtion").columnIterator.next()
        assertEquals("col_barDescriPtion", column.name)

        persistentClass = config.getClassMapping("TestManySide")
        assertEquals("table_TestManySide", persistentClass.table.name)

        column = persistentClass.getProperty("id").columnIterator.next()
        assertEquals("col_id", column.name)

        column = persistentClass.getProperty("version").columnIterator.next()
        assertEquals("col_version", column.name)

        column = persistentClass.getProperty("testOneSide").columnIterator.next()
        assertEquals("EXPECTED_COLUMN_NAME", column.name)
    }

    void testCustomNamingStrategyAsInstance() {

        // somewhat artificial in that it doesn't test that setting the property
        // in DataSource.groovy works, but that's handled in DataSourceConfigurationTests
        def instance = new CustomNamingStrategy()
        GrailsDomainBinder.configureNamingStrategy(instance)

        GrailsDomainClass oneClass = new DefaultGrailsDomainClass(
            cl.parseClass('''
class TestOneSide {
    Long id
    Long version
    String fooName
    String barDescriPtion
}'''))

        assert instance.is(GrailsDomainBinder.getNamingStrategy('sessionFactory'))
        assert instance.is(GrailsDomainBinder.NAMING_STRATEGIES.DEFAULT)

        DefaultGrailsDomainConfiguration config = getDomainConfig(cl, [oneClass.clazz])

        PersistentClass persistentClass = config.getClassMapping("TestOneSide")
        assertEquals("table_TestOneSide", persistentClass.table.name)

        Column column = persistentClass.getProperty("id").columnIterator.next()
        assertEquals("col_id", column.name)
    }

    void testManyToManyWithBag() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(BAG_MANY_TO_MANY_CLASSES_DEFINITION)

        org.hibernate.mapping.Collection c = findCollection(config, 'ManyBagged.baggers')
        assertNotNull c
        assertTrue c instanceof Bag
        assertSame getTableMapping('many_bagged', config), c.table

        c = findCollection(config, 'ManyBagger.bagged')
        assertNotNull c
        assertTrue c instanceof Bag
        assertSame getTableMapping('many_bagger', config), c.table
    }

    void testOneToManyWithBag() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(BAG_ONE_TO_MANY_CLASSES_DEFINITION)
        org.hibernate.mapping.Collection c = findCollection(config, 'Bagger.bagged')
        assertNotNull c
        assertTrue c instanceof Bag
        assertSame getTableMapping('bagger', config), c.table
    }

    void testEnumProperty() {
        DefaultGrailsDomainConfiguration config = getDomainConfig('''
enum AlertType {
    INFO, WARN, ERROR
}
class Alert {
    Long id
    Long version
    String message
    AlertType alertType
    static mapping = {
        alertType sqlType: 'char', length: 5
    }
}''')
        Table tableMapping = getTableMapping("alert", config)
        assertNotNull("Cannot find table mapping", tableMapping)
        Column enumColumn = tableMapping.getColumn(new Column("alert_type"))
        // we are mainly interested in length, but also check sqlType.
        assertNotNull(enumColumn)
        assertEquals(5, enumColumn.length)
        assertEquals('char', enumColumn.sqlType)
        assertEquals(Column.DEFAULT_PRECISION, enumColumn.precision)
        assertEquals(Column.DEFAULT_SCALE, enumColumn.scale)
    }

    private org.hibernate.mapping.Collection findCollection(DefaultGrailsDomainConfiguration config, String role) {
        config.collectionMappings.find { it.role == role }
    }

    private DefaultGrailsDomainConfiguration getDomainConfig(String classesDefinition) {
        cl.parseClass(classesDefinition)
        return getDomainConfig(cl, cl.loadedClasses)
    }

    private DefaultGrailsDomainConfiguration getDomainConfig(GroovyClassLoader cl, classes) {
        GrailsApplication grailsApplication = new DefaultGrailsApplication(classes as Class[], cl)
        grailsApplication.initialise()
        DefaultGrailsDomainConfiguration config = new DefaultGrailsDomainConfiguration(
            grailsApplication: grailsApplication)
        config.buildMappings()
        return config
    }

    private Table getTableMapping(String tablename, DefaultGrailsDomainConfiguration config) {
        for (Table table in config.tableMappings) {
            if (tablename.equals(table.name)) {
                return table
            }
        }
        null
    }

    private int getTableCount(DefaultGrailsDomainConfiguration config) {
        config.tableMappings.size()
    }

    private void assertForeignKey(String parentTablename, String childTablename, DefaultGrailsDomainConfiguration config) {
        Table childTable = getTableMapping(childTablename, config)
        for (ForeignKey fk in childTable.foreignKeyIterator) {
            if (parentTablename.equals(fk.referencedTable.name)) {
                return
            }
        }
        fail "FK $childTablename->$parentTablename doesn't exist"
    }

    private void assertColumnNotNullable(String tablename, String columnName, DefaultGrailsDomainConfiguration config) {
        Table table = getTableMapping(tablename, config)
        assertFalse(table.name + "." + columnName +  " is nullable",
            table.getColumn(new Column(columnName)).isNullable())
    }

    private void assertColumnNullable(String tablename, String columnName, DefaultGrailsDomainConfiguration config) {
        Table table = getTableMapping(tablename, config)
        assertTrue(table.name + "." + columnName +  " is not nullable",
                table.getColumn(new Column(columnName)).isNullable())
    }

    private void assertColumnLength(ConstrainedProperty constrainedProperty, int expectedLength) {
        Column column = new Column()
        GrailsDomainBinder.bindStringColumnConstraints(column, constrainedProperty)
        assertEquals(expectedLength, column.length)
    }

    private void assertColumnPrecisionAndScale(ConstrainedProperty constrainedProperty, int expectedPrecision, int expectedScale) {
        Column column = new Column()
        GrailsDomainBinder.bindNumericColumnConstraints(column, constrainedProperty)
        assertEquals(expectedPrecision, column.precision)
        assertEquals(expectedScale, column.scale)
    }

    private ConstrainedProperty getConstrainedBigDecimalProperty() {
        return getConstrainedProperty("testBigDecimal")
    }

    private ConstrainedProperty getConstrainedStringProperty() {
        return getConstrainedProperty("testString")
    }

    private ConstrainedProperty getConstrainedProperty(String propertyName) {
        BeanWrapper constrainedBean = new BeanWrapperImpl(new TestClass())
        return new ConstrainedProperty(constrainedBean.wrappedClass, propertyName, constrainedBean.getPropertyType(propertyName))
    }

    static class CustomNamingStrategy extends ImprovedNamingStrategy {
       private static final long serialVersionUID = 1

       @Override
       String classToTableName(String className) {
           "table_" + StringHelper.unqualify(className)
       }

       @Override
       String propertyToColumnName(String propertyName) {
           "col_" + StringHelper.unqualify(propertyName)
       }
   }
}
