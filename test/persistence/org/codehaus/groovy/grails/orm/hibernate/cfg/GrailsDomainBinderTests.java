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

import groovy.lang.GroovyClassLoader;
import groovy.lang.IntRange;
import groovy.lang.ObjectRange;
import junit.framework.TestCase;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.codehaus.groovy.grails.validation.TestClass;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author Jason Rudolph
 * @author Sergey Nebolsin
 * @since 0.4
 * 
 * Created: 06-Jan-2007
 */
public class GrailsDomainBinderTests extends TestCase {
    
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
        Table table = getTableMapping("vet_specialty", config);
        assertNotNull("VET_SPECIALTY table has a PK", table.getPrimaryKey());
        assertEquals("VET_SPECIALTY table has a 2 column PK", 2, table.getPrimaryKey().getColumns().size());
    }
    
    public void testManyToManyBindingFk() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(MANY_TO_MANY_CLASSES_DEFINITION);        
        assertForeignKey("specialty", "vet_specialty", config);
        assertForeignKey("vet", "vet_specialty", config);
    }

    public void testManyToManyBindingFkColumn() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(MANY_TO_MANY_CLASSES_DEFINITION);        
        assertColumnNotNullable("vet_specialty", "vets_id", config);
        assertColumnNotNullable("vet_specialty", "specialities_id", config);
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
	            "}")
		);
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
			            "}")
		);
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
			            "}")
		);

        DefaultGrailsDomainConfiguration config = getDomainConfig(cl, 
            new Class[]{ oneClass.getClazz(), domainClass.getClazz() });

        PersistentClass persistentClass = config.getClassMapping("TestManySide");

        Column column = (Column) persistentClass.getProperty("testOneSide").getColumnIterator().next();
        assertEquals("EXPECTED_COLUMN_NAME", column.getName());
	}
	
	/**
     * @see GrailsDomainBinder#bindStringColumnConstraints(Column, ConstrainedProperty)
     */
    public void testBindStringColumnConstraints() {
        // Verify that the correct length is set when a maxSize constraint is applied
        ConstrainedProperty constrainedProperty = getConstrainedStringProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_SIZE_CONSTRAINT, new Integer(30));
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
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_SIZE_CONSTRAINT, new Integer(30));
        assertColumnLength(constrainedProperty, 30);
    }

    /**
     * @see GrailsDomainBinder#bindNumericColumnConstraints(Column, ConstrainedProperty)
     */
    public void testBindNumericColumnConstraints() {
        ConstrainedProperty constrainedProperty = getConstrainedBigDecimalProperty();
        // maxSize and minSize constraint has the number with the most digits
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_SIZE_CONSTRAINT, new Integer(123));
        constrainedProperty.applyConstraint(ConstrainedProperty.MIN_SIZE_CONSTRAINT, new Integer(0));
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
        constrainedProperty.applyConstraint(ConstrainedProperty.SCALE_CONSTRAINT, new Integer(4));
        assertColumnPrecisionAndScale(constrainedProperty, Column.DEFAULT_PRECISION, 4);

        // Verify that the precision is set correctly for a floating point number with a min/max constraint and a scale...
        //  1) where the min/max constraint includes fewer decimal places than the scale constraint
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new BigDecimal("123.45"));
        constrainedProperty.applyConstraint(ConstrainedProperty.MIN_CONSTRAINT, new BigDecimal("0"));
        constrainedProperty.applyConstraint(ConstrainedProperty.SCALE_CONSTRAINT, new Integer(3));
        assertColumnPrecisionAndScale(constrainedProperty, 6, 3); // precision (6) = number of integer digits in max constraint ("123.45") + scale (3)

        //  2) where the min/max constraint includes more decimal places than the scale constraint
        constrainedProperty = getConstrainedBigDecimalProperty();
        constrainedProperty.applyConstraint(ConstrainedProperty.MAX_CONSTRAINT, new BigDecimal("123.4567"));
        constrainedProperty.applyConstraint(ConstrainedProperty.MIN_CONSTRAINT, new BigDecimal("0"));
        constrainedProperty.applyConstraint(ConstrainedProperty.SCALE_CONSTRAINT, new Integer(3));
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

    private DefaultGrailsDomainConfiguration getDomainConfig(String classesDefinition) {
        GroovyClassLoader cl = new GroovyClassLoader();
        cl.parseClass(classesDefinition);
        return getDomainConfig(cl, cl.getLoadedClasses());
    }

    private DefaultGrailsDomainConfiguration getDomainConfig(GroovyClassLoader cl, Class[] classes) {
        GrailsApplication grailsApplication = new DefaultGrailsApplication(
                classes, cl);
        grailsApplication.initialise();
        DefaultGrailsDomainConfiguration config = new DefaultGrailsDomainConfiguration();
        config.setGrailsApplication(grailsApplication);
        config.buildMappings();
        return config;
    }

    private Table getTableMapping(String tablename, DefaultGrailsDomainConfiguration config) {
        Table result = null;
        for (Iterator tableMappings = config.getTableMappings(); tableMappings.hasNext(); ) {
            Table table = (Table) tableMappings.next();
            if (tablename.equals(table.getName())) {
                result = table;
            }
        }
        return result;
    }
    
    private int getTableCount(DefaultGrailsDomainConfiguration config) {
        int count = 0;
        for (Iterator tables = config.getTableMappings(); tables.hasNext(); tables.next()) {
            count++;
        }
        return count;
    }

    private void assertForeignKey(String parentTablename, String childTablename, DefaultGrailsDomainConfiguration config) {
        boolean fkFound = false;
        Table childTable = getTableMapping(childTablename, config);
        for (Iterator fks = childTable.getForeignKeyIterator(); fks.hasNext(); ) {
            ForeignKey fk = (ForeignKey) fks.next();            
            if (parentTablename.equals(fk.getReferencedTable().getName())) {
                fkFound = true;
            }
        }
        assertTrue("FK exists " + childTablename + "->" + parentTablename, fkFound);
    }

    private void assertColumnNotNullable(String tablename, String columnName, DefaultGrailsDomainConfiguration config) {
        Table table = getTableMapping(tablename, config);
        assertTrue(table.getName() + "." + columnName +  " is not nullable",
            !table.getColumn(new Column(columnName)).isNullable());        
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
}
