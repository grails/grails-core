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
package org.codehaus.groovy.grails.orm.hibernate.cfg;

import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovyClassLoader;
import groovy.lang.IntRange;
import groovy.lang.ObjectRange;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.plugins.MockGrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerHolder;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.codehaus.groovy.grails.validation.TestClass;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.util.StringHelper;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/**
 * @author Jason Rudolph
 * @author Sergey Nebolsin
 * @since 0.4
 *
 * Created: 06-Jan-2007
 */
public class GrailsDomainBinderTests extends TestCase {

    private static final String CACHED_MAP =
        "class Area {\n" +
        "    Long id \n" +
        "    Long version \n" +
        "    Map names \n" +
        "    static mapping = { \n" +
        "        names cache: true \n" +
        "    } \n" +
        "}\n";

    private static final String ONE_TO_ONE_CLASSES_DEFINITION =
        "class Species {\n" +
        "    Long id \n" +
        "    Long version \n" +
        "    String name \n" +
        "} \n" +
        "class Pet {\n" +
        "    Long id \n" +
        "    Long version \n" +
        "    Species species \n" +
        "}";

    private static final String ONE_TO_MANY_CLASSES_DEFINITION =
        "class Visit {\n" +
        "    Long id \n" +
        "    Long version \n" +
        "    String description \n" +
        "} \n" +
        "class Pet {\n" +
        "    Long id \n" +
        "    Long version \n" +
        "    Set visits \n" +
        "    static hasMany = [visits:Visit] \n" +
        "    static mapping = { visits joinTable:false, nullable:false }" +
        "}";

    private static final String MANY_TO_MANY_CLASSES_DEFINITION =
        "class Specialty {\n" +
        "    Long id \n" +
        "    Long version \n" +
        "    String name \n" +
        "    Set vets \n" +
        "    static hasMany = [vets:Vet] \n" +
        "    static belongsTo = Vet \n" +
        "} \n" +
        "class Vet {\n" +
        "    Long id \n" +
        "    Long version \n" +
        "    Set specialities \n" +
        "    static hasMany = [specialities:Specialty] \n" +
        "}";

    private static final String MULTI_COLUMN_USER_TYPE_DEFINITION =
        "import org.codehaus.groovy.grails.orm.hibernate.cfg.*\n" +
        "class Item {\n" +
        "    Long id \n" +
        "    Long version \n" +
        "    String name \n" +
        "    MyType other \n" +
        "    MonetaryAmount price \n" +
        "    static mapping = {\n" +
        "        name column: 's_name', sqlType: 'text'\n" +
        "        other type: MyUserType, sqlType: 'wrapper-characters', params:[param1: 'myParam1', param2: 'myParam2']\n" +
        "        price type: MonetaryAmountUserType, {\n" +
        "            column name: 'value'\n" +
        "            column name: 'currency_code', sqlType: 'text'\n" +
        "        }\n" +
        "    }\n" +
        "}";

    private static final String UNIQUE_PROPERTIES =
        "class User {\n" +
        "    Long id \n" +
        "    Long version \n" +
        "    String login \n" +
        "    String group \n" +
        "    String camelCased \n" +
        "    String employeeID \n" +
        "    static constraints = {    \n" +
        "        employeeID(unique:true)     \n" +
        "        group(unique:'camelCased')     \n" +
        "        login(unique:['group','camelCased'])   \n" +
        "   }\n" +
        "}";

    private static final String TABLE_PER_HIERARCHY =
        "class TablePerHierarchySuperclass {\n" +
        "    Long id \n" +
        "    Long version \n" +
        "    String stringProperty \n" +
        "    String optionalStringProperty \n" +
        "    ProductStatus someProductStatus \n" +
        "    ProductStatus someOptionalProductStatus \n" +
        "    static constraints = {" +
        "        optionalStringProperty nullable: true \n" +
        "        someOptionalProductStatus nullable: true \n" +
        "    } \n" +
        "} \n" +
        "enum ProductStatus {\n" +
        "    GOOD, BAD \n" +
        "} \n" +
        "class TablePerHierarchySubclass extends TablePerHierarchySuperclass {\n" +
        "    Long id \n" +
        "    Long version \n" +
        "    ProductStatus productStatus \n" +
        "    String productName \n" +
        "    Integer productCount \n" +
        "    ProductStatus optionalProductStatus \n" +
        "    String optionalProductName \n" +
        "    Integer optionalProductCount \n" +
        "    static constraints = {" +
        "        optionalProductName nullable: true \n" +
        "        optionalProductCount nullable: true \n" +
        "        optionalProductStatus nullable: true \n" +
        "    } \n" +
        "}";

    private static final String TABLE_PER_SUBCLASS =
        "class TablePerSubclassSuperclass {\n" +
        "    Long id \n" +
        "    Long version \n" +
        "    String stringProperty \n" +
        "    String optionalStringProperty \n" +
        "    ProductStatus someProductStatus \n" +
        "    ProductStatus someOptionalProductStatus \n" +
        "    static constraints = {" +
        "        optionalStringProperty nullable: true \n" +
        "        someOptionalProductStatus nullable: true \n" +
        "    } \n" +
        "    static mapping = {\n" +
        "        tablePerHierarchy false\n" +
        "    }\n" +
        "} \n" +
        "enum ProductStatus {\n" +
        "    GOOD, BAD \n" +
        "} \n" +
        "class TablePerSubclassSubclass extends TablePerSubclassSuperclass {\n" +
        "    Long id \n" +
        "    Long version \n" +
        "    ProductStatus productStatus \n" +
        "    String productName \n" +
        "    Integer productCount \n" +
        "    ProductStatus optionalProductStatus \n" +
        "    String optionalProductName \n" +
        "    Integer optionalProductCount \n" +
        "    static constraints = {" +
        "        optionalProductName nullable: true \n" +
        "        optionalProductCount nullable: true \n" +
        "        optionalProductStatus nullable: true \n" +
        "    } \n" +
        "}";


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ExpandoMetaClass.enableGlobally();
        MockGrailsPluginManager pluginManager = new MockGrailsPluginManager();
        PluginManagerHolder.setPluginManager(pluginManager);

    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        GrailsDomainBinder.NAMING_STRATEGIES.clear();
        GrailsDomainBinder.NAMING_STRATEGIES.put(
              GrailsDomainClassProperty.DEFAULT_DATA_SOURCE, ImprovedNamingStrategy.INSTANCE);
        PluginManagerHolder.setPluginManager(null);
    }

    /**
     * Test for GRAILS-4200
     */
    public void testEmbeddedComponentMapping() {
        DefaultGrailsDomainConfiguration config = getDomainConfig("class Widget {\n" +
        "    Long id \n" +
        "    Long version \n" +
        "    EmbeddedWidget ew \n" +
        "    static embedded = ['ew']\n" +
        "}\n" +
        "    class EmbeddedWidget {\n" +
        "       String ew\n" +
        "       static mapping = {\n" +
        "           ew column: 'widget_name'\n" +
        "       }\n" +
        "    }");

        Table tableMapping = getTableMapping("widget", config);
        Column embeddedComponentMappedColumn = tableMapping.getColumn(new Column("widget_name"));
        assertNotNull(embeddedComponentMappedColumn);
        assertEquals("widget_name", embeddedComponentMappedColumn.getName());
    }

    public void testLengthProperty() {
        DefaultGrailsDomainConfiguration config = getDomainConfig("class Widget {\n" +
        "    Long id \n" +
        "    Long version \n" +
        "    String name \n" +
        "    String description \n" +
        "    static mapping = {\n" +
        "        name column: 's_name', sqlType: 'text', length: 42\n" +
        "        description column: 's_description', sqlType: 'text'\n" +
        "    }\n" +
        "}");
        Table tableMapping = getTableMapping("widget", config);
        Column nameColumn = tableMapping.getColumn(new Column("s_name"));
        Column descriptionColumn = tableMapping.getColumn(new Column("s_description"));
        assertEquals(42, nameColumn.getLength());
        assertEquals(Column.DEFAULT_LENGTH, descriptionColumn.getLength());
    }

    public void testUniqueProperty() {
        DefaultGrailsDomainConfiguration config = getDomainConfig("class Widget {\n" +
                "    Long id \n" +
                "    Long version \n" +
                "    String name \n" +
                "    String description \n" +
                "    static mapping = {\n" +
                "        name unique: true\n" +
                "    }\n" +
        "}");
        Table tableMapping = getTableMapping("widget", config);
        Column nameColumn = tableMapping.getColumn(new Column("name"));
        Column descriptionColumn = tableMapping.getColumn(new Column("description"));
        assertEquals(true, nameColumn.isUnique());
        assertEquals(false, descriptionColumn.isUnique());
    }

    public void testPrecisionProperty() {
        DefaultGrailsDomainConfiguration config = getDomainConfig("class Widget {\n" +
                "    Long id \n" +
                "    Long version \n" +
                "    Float width \n" +
                "    Float height \n" +
                "    static mapping = {\n" +
                "        width precision: 3\n" +
                "    }\n" +
        "}");
        Table tableMapping = getTableMapping("widget", config);
        Column heightColumn = tableMapping.getColumn(new Column("height"));
        Column widthColumn = tableMapping.getColumn(new Column("width"));
        assertEquals(3, widthColumn.getPrecision());
        assertEquals(Column.DEFAULT_PRECISION, heightColumn.getPrecision());
    }

    public void testScaleProperty() {
        DefaultGrailsDomainConfiguration config = getDomainConfig("class Widget {\n" +
                "    Long id \n" +
                "    Long version \n" +
                "    Float width \n" +
                "    Float height \n" +
                "    static mapping = {\n" +
                "        width scale: 7\n" +
                "    }\n" +
        "}");
        Table tableMapping = getTableMapping("widget", config);
        Column heightColumn = tableMapping.getColumn(new Column("height"));
        Column widthColumn = tableMapping.getColumn(new Column("width"));
        assertEquals(7, widthColumn.getScale());
        assertEquals(Column.DEFAULT_SCALE, heightColumn.getScale());
    }

    public void testCachedMapProperty() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(CACHED_MAP);
        Table table = getTableMapping("area_names", config);
        assertEquals(255, table.getColumn(new Column("names_elt")).getLength());
    }

    public void testColumnNullabilityWithTablePerHierarchy() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(TABLE_PER_HIERARCHY);

        // with tablePerHierarchy all columns related to properties defined in subclasses
        // must be nullable to allow instances of other classes in the hierarchy to be
        // persisted
        assertColumnNullable("table_per_hierarchy_superclass", "product_status", config);
        assertColumnNullable("table_per_hierarchy_superclass", "product_name", config);
        assertColumnNullable("table_per_hierarchy_superclass", "product_count", config);
        assertColumnNullable("table_per_hierarchy_superclass", "optional_product_status", config);
        assertColumnNullable("table_per_hierarchy_superclass", "optional_product_name", config);
        assertColumnNullable("table_per_hierarchy_superclass", "optional_product_count", config);

        // columns related to required properties in the root class should not be nullable
        assertColumnNotNullable("table_per_hierarchy_superclass", "string_property", config);
        assertColumnNotNullable("table_per_hierarchy_superclass", "some_product_status", config);

        // columns related to optional properties in the root class should be nullable
        assertColumnNullable("table_per_hierarchy_superclass", "optional_string_property", config);
        assertColumnNullable("table_per_hierarchy_superclass", "some_optional_product_status", config);
    }

    public void testColumnNullabilityWithTablePerSubclass() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(TABLE_PER_SUBCLASS);

        // with tablePerSubclass columns related to required properties defined in subclasses
        // should not be nullable
        assertColumnNotNullable("table_per_subclass_subclass", "product_status", config);
        assertColumnNotNullable("table_per_subclass_subclass", "product_name", config);
        assertColumnNotNullable("table_per_subclass_subclass", "product_count", config);

        // with tablePerSubclass columns related to optional properties defined in subclasses
        // should be nullable
        assertColumnNullable("table_per_subclass_subclass", "optional_product_status", config);
        assertColumnNullable("table_per_subclass_subclass", "optional_product_name", config);
        assertColumnNullable("table_per_subclass_subclass", "optional_product_count", config);

        // columns related to required properties in the root class should not be nullable
        assertColumnNotNullable("table_per_subclass_superclass", "string_property", config);
        assertColumnNotNullable("table_per_subclass_superclass", "some_product_status", config);

        // columns related to optional properties in the root class should be nullable
        assertColumnNullable("table_per_subclass_superclass", "optional_string_property", config);
        assertColumnNullable("table_per_subclass_superclass", "some_optional_product_status", config);
    }

    @SuppressWarnings("rawtypes")
    public void testUniqueConstraintGeneration() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(UNIQUE_PROPERTIES);
        assertEquals("Tables created", 1, getTableCount(config));
        List expectedKeyColumns1 = Arrays.asList(new Column[]{new Column("camel_cased"),new Column("group"),new Column("login")});
        List expectedKeyColumns2 = Arrays.asList(new Column[]{new Column("camel_cased"),new Column("group")});
        Table mapping = (Table) config.getTableMappings().next();
        int cnt = 0;
        boolean found1 = false, found2 = false;
        for (Iterator<?> i = mapping.getUniqueKeyIterator(); i.hasNext();) {
            UniqueKey key = (UniqueKey) i.next();
            List keyColumns = key.getColumns();
            if (keyColumns.equals(expectedKeyColumns1)) {
                found1 = true;
            }
            if (keyColumns.equals(expectedKeyColumns2)) {
                found2 = true;
            }
            cnt++;
        }
        assertEquals(2, cnt);
        assertEquals(true, mapping.getColumn(new Column("employeeID")).isUnique());
        assertEquals(true, found1);
        assertEquals(true, found2);
    }

    public void testInsertableHibernateMapping() throws Exception {
        GroovyClassLoader cl = new GroovyClassLoader();
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
            cl.parseClass(
                "class TestInsertableDomain {\n" +
                "    Long id \n" +
                "    Long version \n" +
                "    String testString1 \n" +
                "    String testString2 \n" +
                "\n" +
                "    static mapping = {\n" +
                "       testString1 insertable:false \n" +
                "       testString2 insertable:true \n" +
                "    }\n" +
                "}"));

        DefaultGrailsDomainConfiguration config = getDomainConfig(cl,
                new Class[] { domainClass.getClazz() });
        Field privateDomainClasses = DefaultGrailsDomainConfiguration.class.
            getDeclaredField("domainClasses");
        privateDomainClasses.setAccessible(true);

        PersistentClass persistentClass = config.getClassMapping("TestInsertableDomain");

        Property property1 = persistentClass.getProperty("testString1");
        assertEquals(false,property1.isInsertable());

        Property property2 = persistentClass.getProperty("testString2");
        assertEquals(true,property2.isInsertable());
    }

    public void testUpdateableHibernateMapping() throws Exception {
        GroovyClassLoader cl = new GroovyClassLoader();
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
            cl.parseClass(
                "class TestInsertableDomain {\n" +
                "    Long id \n" +
                "    Long version \n" +
                "    String testString1 \n" +
                "    String testString2 \n" +
                "\n" +
                "    static mapping = {\n" +
                "       testString1 updateable:false \n" +
                "       testString2 updateable:true \n" +
                "    }\n" +
                "}"));

        DefaultGrailsDomainConfiguration config = getDomainConfig(cl,
                new Class[] { domainClass.getClazz() });

        PersistentClass persistentClass = config.getClassMapping("TestInsertableDomain");

        Property property1 = persistentClass.getProperty("testString1");
        assertEquals(false,property1.isUpdateable());

        Property property2 = persistentClass.getProperty("testString2");
        assertEquals(true,property2.isUpdateable());
    }

    public void testInsertableUpdateableHibernateMapping() throws Exception {
        GroovyClassLoader cl = new GroovyClassLoader();
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
            cl.parseClass(
                "class TestInsertableUpdateableDomain {\n" +
                "    Long id \n" +
                "    Long version \n" +
                "    String testString1 \n" +
                "    String testString2 \n" +
                "\n" +
                "    static mapping = {\n" +
                "       testString1 insertable:false, updateable:false \n" +
                "       testString2 updateable:false, insertable:false \n" +
                "    }\n" +
                "}"));

        DefaultGrailsDomainConfiguration config = getDomainConfig(cl,
                new Class[] { domainClass.getClazz() });

        PersistentClass persistentClass = config.getClassMapping("TestInsertableUpdateableDomain");

        Property property1 = persistentClass.getProperty("testString1");
        assertEquals(false,property1.isInsertable());
        assertEquals(false,property1.isUpdateable());

        Property property2 = persistentClass.getProperty("testString2");
        assertEquals(false,property2.isUpdateable());
        assertEquals(false,property2.isInsertable());
    }

    public void testOneToOneBindingTables() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(ONE_TO_ONE_CLASSES_DEFINITION);
        assertEquals("Tables created", 2, getTableCount(config));
    }

    public void testOneToOneBindingFk() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(ONE_TO_ONE_CLASSES_DEFINITION);
        assertForeignKey("species", "pet", config);
    }

    public void testOneToOneBindingFkColumn() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(ONE_TO_ONE_CLASSES_DEFINITION);
        assertColumnNotNullable("pet", "species_id", config);
    }

    public void testOneToManyBindingTables() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(ONE_TO_MANY_CLASSES_DEFINITION);
        assertEquals("Tables created", 2, getTableCount(config));
    }

    public void testOneToManyBindingFk() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(ONE_TO_MANY_CLASSES_DEFINITION);
        assertForeignKey("pet", "visit", config);
    }

/*    public void testOneToManyBindingFkColumn() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(ONE_TO_MANY_CLASSES_DEFINITION);
        assertColumnNotNullable("visit", "pet_visits_id", config);
    }*/

    public void testManyToManyBindingTables() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(MANY_TO_MANY_CLASSES_DEFINITION);
        assertEquals("Tables created", 3, getTableCount(config));
    }

    public void testManyToManyBindingPk() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(MANY_TO_MANY_CLASSES_DEFINITION);
        Table table = getTableMapping("vet_specialities", config);
        assertNotNull("VET_SPECIALTY table has a PK", table.getPrimaryKey());
        assertEquals("VET_SPECIALTY table has a 2 column PK", 2, table.getPrimaryKey().getColumns().size());
    }

    public void testManyToManyBindingFk() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(MANY_TO_MANY_CLASSES_DEFINITION);
        assertForeignKey("specialty", "vet_specialities", config);
        assertForeignKey("vet", "vet_specialities", config);
    }

    public void testManyToManyBindingFkColumn() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(MANY_TO_MANY_CLASSES_DEFINITION);
        assertColumnNotNullable("vet_specialities", "vet_id", config);
        assertColumnNotNullable("vet_specialities", "specialty_id", config);
    }

    /**
     * Tests that single- and multi-column user type mappings work
     * correctly. Also Checks that the "sqlType" property is honoured.
     */
    public void testUserTypeMappings() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(MULTI_COLUMN_USER_TYPE_DEFINITION);
        PersistentClass persistentClass = config.getClassMapping("Item");

        // First check the "name" property and its associated column.
        Property nameProperty = persistentClass.getProperty("name");
        assertEquals(1, nameProperty.getColumnSpan());
        assertEquals("name", nameProperty.getName());

        Column column = (Column) nameProperty.getColumnIterator().next();
        assertEquals("s_name", column.getName());
        assertEquals("text", column.getSqlType());

        // Next the "other" property.
        Property otherProperty = persistentClass.getProperty("other");
        assertEquals(1, otherProperty.getColumnSpan());
        assertEquals("other", otherProperty.getName());

        column = (Column) otherProperty.getColumnIterator().next();
        assertEquals("other", column.getName());
        assertEquals("wrapper-characters", column.getSqlType());
        assertEquals(MyUserType.class.getName(), column.getValue().getType().getName());
        assertTrue(column.getValue() instanceof SimpleValue);
        SimpleValue v = (SimpleValue)column.getValue();
        assertEquals("myParam1", v.getTypeParameters().get("param1"));
        assertEquals("myParam2", v.getTypeParameters().get("param2"));

        // And now for the "price" property, which should have two
        // columns.
        Property priceProperty = persistentClass.getProperty("price");
        assertEquals(2, priceProperty.getColumnSpan());
        assertEquals("price", priceProperty.getName());

        Iterator<?> colIter = priceProperty.getColumnIterator();
        column = (Column) colIter.next();
        assertEquals("value", column.getName());
        assertNull("SQL type should have been 'null' for 'value' column.", column.getSqlType());

        column = (Column) colIter.next();
        assertEquals("currency_code", column.getName());
        assertEquals("text", column.getSqlType());
    }

    public void testDomainClassBinding() {
        GroovyClassLoader cl = new GroovyClassLoader();
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
            cl.parseClass(
                "public class BinderTestClass {\n" +
                "    Long id; \n" +
                "    Long version; \n" +
                "\n" +
                "    String firstName; \n" +
                "    String lastName; \n" +
                "    String comment; \n" +
                "    Integer age;\n" +
                "    boolean active = true" +
                "\n" +
                "    static constraints = {\n" +
                "        firstName(nullable:true,size:4..15)\n" +
                "        lastName(nullable:false)\n" +
                "        age(nullable:true)\n" +
                "    }\n" +
                "}"));

        DefaultGrailsDomainConfiguration config = getDomainConfig(cl, cl.getLoadedClasses());

        // Test database mappings
        PersistentClass persistentClass = config.getClassMapping("BinderTestClass");
        assertTrue("Property [firstName] must be optional in db mapping", persistentClass.getProperty("firstName").isOptional());
        assertFalse("Property [lastName] must be required in db mapping", persistentClass.getProperty("lastName").isOptional());
        // Property must be required by default
        assertFalse("Property [comment] must be required in db mapping", persistentClass.getProperty("comment").isOptional());

        // Test properties
        assertTrue("Property [firstName] must be optional", domainClass.getPropertyByName("firstName").isOptional());
        assertFalse("Property [lastName] must be optional", domainClass.getPropertyByName("lastName").isOptional());
        assertFalse("Property [comment] must be required", domainClass.getPropertyByName("comment").isOptional());
        assertTrue("Property [age] must be optional", domainClass.getPropertyByName("age").isOptional());
    }

    public void testForeignKeyColumnBinding() {
        GroovyClassLoader cl = new GroovyClassLoader();
        GrailsDomainClass oneClass = new DefaultGrailsDomainClass(
                cl.parseClass(
                        "class TestOneSide {\n" +
                        "    Long id \n" +
                        "    Long version \n" +
                        "    String name \n" +
                        "    String description \n" +
                        "}"));
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
                cl.parseClass(
                        "class TestManySide {\n" +
                        "    Long id \n" +
                        "    Long version \n" +
                        "    String name \n" +
                        "    TestOneSide testOneSide \n" +
                        "\n" +
                        "    static mapping = {\n" +
                        "        columns {\n" +
                        "            testOneSide column:'EXPECTED_COLUMN_NAME'" +
                        "        }\n" +
                        "    }\n" +
                        "}"));

        DefaultGrailsDomainConfiguration config = getDomainConfig(cl,
            new Class[]{ oneClass.getClazz(), domainClass.getClazz() });

        PersistentClass persistentClass = config.getClassMapping("TestManySide");

        Column column = (Column) persistentClass.getProperty("testOneSide").getColumnIterator().next();
        assertEquals("EXPECTED_COLUMN_NAME", column.getName());
    }

    /**
     * @see GrailsDomainBinder#bindStringColumnConstraints(Column, ConstrainedProperty)
     */
    @SuppressWarnings("rawtypes")
    public void testBindStringColumnConstraints() {
        // Verify that the correct length is set when a maxSize constraint is applied
        ConstrainedProperty constrainedProperty = getConstrainedStringProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_SIZE_CONSTRAINT, 30);
        assertColumnLength(constrainedProperty, 30);

        // Verify that the correct length is set when a size constraint is applied
        constrainedProperty = getConstrainedStringProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.SIZE_CONSTRAINT, new IntRange(6, 32768));
        assertColumnLength(constrainedProperty, 32768);

        // Verify that the default length remains intact when no size-related constraints are applied
        constrainedProperty = getConstrainedStringProperty();
        assertColumnLength(constrainedProperty, Column.DEFAULT_LENGTH);

        // Verify that the correct length is set when an inList constraint is applied
        constrainedProperty = getConstrainedStringProperty();
        List validValuesList = Arrays.asList(new String[] {"Groovy", "Java", "C++"});
        constrainedProperty.applyConstraint(ConstrainedProperty.IN_LIST_CONSTRAINT, validValuesList);
        assertColumnLength(constrainedProperty, 6);

        // Verify that the correct length is set when a maxSize constraint *and* an inList constraint are *both* applied
        constrainedProperty = getConstrainedStringProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.IN_LIST_CONSTRAINT, validValuesList);
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_SIZE_CONSTRAINT, 30);
        assertColumnLength(constrainedProperty, 30);
    }

    /**
     * @see GrailsDomainBinder#bindNumericColumnConstraints(Column, ConstrainedProperty)
     */
    public void testBindNumericColumnConstraints() {
        ConstrainedProperty constrainedProperty = getConstrainedBigDecimalProperty();
        // maxSize and minSize constraint has the number with the most digits
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_SIZE_CONSTRAINT, 123);
        constrainedProperty.applyConstraint(ConstrainedProperty.MIN_SIZE_CONSTRAINT, 0);
        assertColumnPrecisionAndScale(constrainedProperty, Column.DEFAULT_PRECISION, Column.DEFAULT_SCALE);

        // Verify that the correct precision is set when the max constraint has the number with the most digits
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new BigDecimal("123.45"));
        constrainedProperty.applyConstraint(ConstrainedProperty.MIN_CONSTRAINT, new BigDecimal("0"));
        assertColumnPrecisionAndScale(constrainedProperty, 5, Column.DEFAULT_SCALE);

        // Verify that the correct precision is set when the minSize constraint has the number with the most digits
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new BigDecimal("123"));
        constrainedProperty.applyConstraint(ConstrainedProperty.MIN_CONSTRAINT, new BigDecimal("-123.45"));
        assertColumnPrecisionAndScale(constrainedProperty, 5, Column.DEFAULT_SCALE);

        // Verify that the correct precision is set when the high value of a floating point range constraint has the number with the most digits
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.RANGE_CONSTRAINT, new ObjectRange(new BigDecimal("0"), new BigDecimal("123.45")));
        assertColumnPrecisionAndScale(constrainedProperty, 5, Column.DEFAULT_SCALE);

        // Verify that the correct precision is set when the low value of a floating point range constraint has the number with the most digits
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.RANGE_CONSTRAINT, new ObjectRange(new BigDecimal("-123.45"), new BigDecimal("123")));
        assertColumnPrecisionAndScale(constrainedProperty, 5, Column.DEFAULT_SCALE);

        // Verify that the correct scale is set when the scale constraint is specified in isolation
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.SCALE_CONSTRAINT, 4);
        assertColumnPrecisionAndScale(constrainedProperty, Column.DEFAULT_PRECISION, 4);

        // Verify that the precision is set correctly for a floating point number with a min/max constraint and a scale...
        //  1) where the min/max constraint includes fewer decimal places than the scale constraint
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new BigDecimal("123.45"));
        constrainedProperty.applyConstraint(ConstrainedProperty.MIN_CONSTRAINT, new BigDecimal("0"));
        constrainedProperty.applyConstraint(ConstrainedProperty.SCALE_CONSTRAINT, 3);
        assertColumnPrecisionAndScale(constrainedProperty, 6, 3); // precision (6) = number of integer digits in max constraint ("123.45") + scale (3)

        //  2) where the min/max constraint includes more decimal places than the scale constraint
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new BigDecimal("123.4567"));
        constrainedProperty.applyConstraint(ConstrainedProperty.MIN_CONSTRAINT, new BigDecimal("0"));
        constrainedProperty.applyConstraint(ConstrainedProperty.SCALE_CONSTRAINT, 3);
        assertColumnPrecisionAndScale(constrainedProperty, 7, 3); // precision (7) = number of digits in max constraint ("123.4567")

        // Verify that the correct precision is set when the only one of 'min' and 'max' constraint specified
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new BigDecimal("123.4567"));
        assertColumnPrecisionAndScale(constrainedProperty, Column.DEFAULT_PRECISION, Column.DEFAULT_SCALE);
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new BigDecimal("12345678901234567890.4567"));
        assertColumnPrecisionAndScale(constrainedProperty, 24, Column.DEFAULT_SCALE);
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.MIN_CONSTRAINT, new BigDecimal("-123.4567"));
        assertColumnPrecisionAndScale(constrainedProperty, Column.DEFAULT_PRECISION, Column.DEFAULT_SCALE);
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.MIN_CONSTRAINT, new BigDecimal("-12345678901234567890.4567"));
        assertColumnPrecisionAndScale(constrainedProperty, 24, Column.DEFAULT_SCALE);
    }

    public void testDefaultNamingStrategy() {

        GroovyClassLoader cl = new GroovyClassLoader();
        GrailsDomainClass oneClass = new DefaultGrailsDomainClass(
            cl.parseClass(
                "class TestOneSide {\n" +
                "    Long id \n" +
                "    Long version \n" +
                "    String fooName \n" +
                "    String barDescriPtion \n" +
                "}"));
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
            cl.parseClass(
                "class TestManySide {\n" +
                "    Long id \n" +
                "    Long version \n" +
                "    TestOneSide testOneSide \n" +
                "\n" +
                "    static mapping = {\n" +
                "        columns {\n" +
                "            testOneSide column:'EXPECTED_COLUMN_NAME'" +
                "        }\n" +
                "    }\n" +
                "}"));

        DefaultGrailsDomainConfiguration config = getDomainConfig(cl,
                new Class[]{ oneClass.getClazz(), domainClass.getClazz() });

        PersistentClass persistentClass = config.getClassMapping("TestOneSide");
        assertEquals("test_one_side", persistentClass.getTable().getName());

        Column column = (Column)persistentClass.getProperty("id").getColumnIterator().next();
        assertEquals("id", column.getName());

        column = (Column)persistentClass.getProperty("version").getColumnIterator().next();
        assertEquals("version", column.getName());

        column = (Column)persistentClass.getProperty("fooName").getColumnIterator().next();
        assertEquals("foo_name", column.getName());

        column = (Column)persistentClass.getProperty("barDescriPtion").getColumnIterator().next();
        assertEquals("bar_descri_ption", column.getName());

        persistentClass = config.getClassMapping("TestManySide");
        assertEquals("test_many_side", persistentClass.getTable().getName());

        column = (Column)persistentClass.getProperty("id").getColumnIterator().next();
        assertEquals("id", column.getName());

        column = (Column)persistentClass.getProperty("version").getColumnIterator().next();
        assertEquals("version", column.getName());

        column = (Column)persistentClass.getProperty("testOneSide").getColumnIterator().next();
        assertEquals("EXPECTED_COLUMN_NAME", column.getName());
    }

    public void testCustomNamingStrategy() throws Exception {

        // somewhat artificial in that it doesn't test that setting the property
        // in DataSource.groovy works, but that's handled in DataSourceConfigurationTests
        GrailsDomainBinder.configureNamingStrategy(CustomNamingStrategy.class);

        GroovyClassLoader cl = new GroovyClassLoader();
        GrailsDomainClass oneClass = new DefaultGrailsDomainClass(
            cl.parseClass(
                "class TestOneSide {\n" +
                "    Long id \n" +
                "    Long version \n" +
                "    String fooName \n" +
                "    String barDescriPtion \n" +
                "}"));
        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
            cl.parseClass(
                "class TestManySide {\n" +
                "    Long id \n" +
                "    Long version \n" +
                "    TestOneSide testOneSide \n" +
                "\n" +
                "    static mapping = {\n" +
                "        columns {\n" +
                "            testOneSide column:'EXPECTED_COLUMN_NAME'" +
                "        }\n" +
                "    }\n" +
                "}"));

        DefaultGrailsDomainConfiguration config = getDomainConfig(cl,
                new Class[] { oneClass.getClazz(), domainClass.getClazz() });

        PersistentClass persistentClass = config.getClassMapping("TestOneSide");
        assertEquals("table_TestOneSide", persistentClass.getTable().getName());

        Column column = (Column)persistentClass.getProperty("id").getColumnIterator().next();
        assertEquals("col_id", column.getName());

        column = (Column)persistentClass.getProperty("version").getColumnIterator().next();
        assertEquals("col_version", column.getName());

        column = (Column)persistentClass.getProperty("fooName").getColumnIterator().next();
        assertEquals("col_fooName", column.getName());

        column = (Column)persistentClass.getProperty("barDescriPtion").getColumnIterator().next();
        assertEquals("col_barDescriPtion", column.getName());

        persistentClass = config.getClassMapping("TestManySide");
        assertEquals("table_TestManySide", persistentClass.getTable().getName());

        column = (Column)persistentClass.getProperty("id").getColumnIterator().next();
        assertEquals("col_id", column.getName());

        column = (Column)persistentClass.getProperty("version").getColumnIterator().next();
        assertEquals("col_version", column.getName());

        column = (Column)persistentClass.getProperty("testOneSide").getColumnIterator().next();
        assertEquals("EXPECTED_COLUMN_NAME", column.getName());
    }

    private DefaultGrailsDomainConfiguration getDomainConfig(String classesDefinition) {
        GroovyClassLoader cl = new GroovyClassLoader();
        cl.parseClass(classesDefinition);
        return getDomainConfig(cl, cl.getLoadedClasses());
    }

    private DefaultGrailsDomainConfiguration getDomainConfig(GroovyClassLoader cl, Class<?>[] classes) {
        GrailsApplication grailsApplication = new DefaultGrailsApplication(classes, cl);
        grailsApplication.initialise();
        DefaultGrailsDomainConfiguration config = new DefaultGrailsDomainConfiguration();
        config.setGrailsApplication(grailsApplication);
        config.buildMappings();
        return config;
    }

    private Table getTableMapping(String tablename, DefaultGrailsDomainConfiguration config) {
        Table result = null;
        for (Iterator<?> tableMappings = config.getTableMappings(); tableMappings.hasNext();) {
            Table table = (Table) tableMappings.next();
            if (tablename.equals(table.getName())) {
                result = table;
            }
        }
        return result;
    }

    private int getTableCount(DefaultGrailsDomainConfiguration config) {
        int count = 0;
        for (Iterator<?> tables = config.getTableMappings(); tables.hasNext(); tables.next()) {
            count++;
        }
        return count;
    }

    private void assertForeignKey(String parentTablename, String childTablename, DefaultGrailsDomainConfiguration config) {
        boolean fkFound = false;
        Table childTable = getTableMapping(childTablename, config);
        for (Iterator<?> fks = childTable.getForeignKeyIterator(); fks.hasNext();) {
            ForeignKey fk = (ForeignKey) fks.next();
            if (parentTablename.equals(fk.getReferencedTable().getName())) {
                fkFound = true;
            }
        }
        assertTrue("FK exists " + childTablename + "->" + parentTablename, fkFound);
    }

    private void assertColumnNotNullable(String tablename, String columnName, DefaultGrailsDomainConfiguration config) {
        Table table = getTableMapping(tablename, config);
        assertFalse(table.getName() + "." + columnName +  " is nullable",
            table.getColumn(new Column(columnName)).isNullable());
    }

    private void assertColumnNullable(String tablename, String columnName, DefaultGrailsDomainConfiguration config) {
        Table table = getTableMapping(tablename, config);
        assertTrue(table.getName() + "." + columnName +  " is not nullable",
                table.getColumn(new Column(columnName)).isNullable());
    }

    private void assertColumnLength(ConstrainedProperty constrainedProperty, int expectedLength) {
        Column column = new Column();
        GrailsDomainBinder.bindStringColumnConstraints(column, constrainedProperty);
        assertEquals(expectedLength, column.getLength());
    }

    private void assertColumnPrecisionAndScale(ConstrainedProperty constrainedProperty, int expectedPrecision, int expectedScale) {
        Column column = new Column();
        GrailsDomainBinder.bindNumericColumnConstraints(column, constrainedProperty);
        assertEquals(expectedPrecision, column.getPrecision());
        assertEquals(expectedScale, column.getScale());
    }

    private ConstrainedProperty getConstrainedBigDecimalProperty() {
        return getConstrainedProperty("testBigDecimal");
    }

    private ConstrainedProperty getConstrainedStringProperty() {
        return getConstrainedProperty("testString");
    }

    private ConstrainedProperty getConstrainedProperty(String propertyName) {
        BeanWrapper constrainedBean = new BeanWrapperImpl(new TestClass());
        return new ConstrainedProperty(constrainedBean.getWrappedClass(), propertyName, constrainedBean.getPropertyType(propertyName));
    }

    public static class CustomNamingStrategy extends ImprovedNamingStrategy {
       private static final long serialVersionUID = 1L;

       @Override
       public String classToTableName(String className) {
           return "table_" + StringHelper.unqualify(className);
       }

       @Override
       public String propertyToColumnName(String propertyName) {
           return "col_" + StringHelper.unqualify(propertyName);
       }
   }
}
