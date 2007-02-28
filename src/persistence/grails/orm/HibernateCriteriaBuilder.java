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
package grails.orm;

import grails.util.ExtendProxy;
import groovy.lang.Closure;
import groovy.lang.GString;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;
import groovy.util.BuilderSupport;
import groovy.util.Proxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.ResultTransformer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * <p>Wraps the Hibernate Criteria API in a builder. The builder can be retrieved through the "createCriteria()" dynamic static 
 * method of Grails domain classes (Example in Groovy): 
 * 
 * <pre>
 * 		def c = Account.createCriteria()
 * 		def results = c {
 * 			projections {
 * 				groupProperty("branch")
 * 			}
 * 			like("holderFirstName", "Fred%")
 * 			and {
 * 				between("balance", 500, 1000)
 * 				eq("branch", "London")
 * 			}
 * 			maxResults(10)
 * 			order("holderLastName", "desc")
 * 		}
 * </pre>
 * 
 * <p>The builder can also be instantiated standalone with a SessionFactory and persistent Class instance:
 * 
 * <pre>
 * 	 new HibernateCriteriaBuilder(clazz, sessionFactory).list {
 * 		eq("firstName", "Fred")
 * 	 }
 * </pre>
 * 
 * @author Graeme Rocher
 * @since Oct 10, 2005
 */
public class HibernateCriteriaBuilder extends BuilderSupport {

    public static final String AND = "and"; // builder
    public static final String IS_NULL = "isNull"; // builder
    public static final String IS_NOT_NULL = "notNull"; // builder
    public static final String NOT = "not";// builder
    public static final String OR = "or"; // builder
    public static final String ID_EQUALS = "idEq"; // builder
    public static final String IS_EMPTY = "isEmpty"; //builder
    public static final String IS_NOT_EMPTY = "isNotEmpty"; //builder


    public static final String BETWEEN = "between";//method
    public static final String EQUALS = "eq";//method
    public static final String EQUALS_PROPERTY = "eqProperty";//method
    public static final String GREATER_THAN = "gt";//method
    public static final String GREATER_THAN_PROPERTY = "gtProperty";//method
    public static final String GREATER_THAN_OR_EQUAL = "ge";//method
    public static final String GREATER_THAN_OR_EQUAL_PROPERTY = "geProperty";//method
    public static final String ILIKE = "ilike";//method
    public static final String IN = "in";//method
    public static final String LESS_THAN = "lt"; //method
    public static final String LESS_THAN_PROPERTY = "ltProperty";//method
    public static final String LESS_THAN_OR_EQUAL = "le";//method
    public static final String LESS_THAN_OR_EQUAL_PROPERTY = "leProperty";//method
    public static final String LIKE = "like";//method
    public static final String NOT_EQUAL = "ne";//method
    public static final String NOT_EQUAL_PROPERTY = "neProperty";//method
    public static final String SIZE_EQUALS = "sizeEq"; //method
    public static final String ORDER_DESCENDING = "desc";
    public static final String ORDER_ASCENDING = "asc";


    private static final String ROOT_CALL = "doCall";
    private static final String LIST_CALL = "list";
    private static final String COUNT_CALL = "count";
    private static final String GET_CALL = "get";
    private static final String SCROLL_CALL = "scroll";
    private static final String PROJECTIONS = "projections";


    private SessionFactory sessionFactory;
    private Session session;
    private Class targetClass;
    private Criteria criteria;
    private boolean uniqueResult = false;
    private Proxy resultProxy = new ExtendProxy();
    private Proxy criteriaProxy;
    private Object parent;
    private List logicalExpressions = new ArrayList();
    private List logicalExpressionArgs = new ArrayList();
    private boolean participate;
    private boolean scroll;
    private boolean count;
    private ProjectionList projectionList;
    private BeanWrapper targetBean;
    private List aliasStack = new ArrayList();
    private static final String ALIAS = "_alias";
    private ResultTransformer resultTransformer;


    public HibernateCriteriaBuilder(Class targetClass, SessionFactory sessionFactory) {
        super();
        this.targetClass = targetClass;
        this.targetBean = new BeanWrapperImpl(BeanUtils.instantiateClass(targetClass));
        this.sessionFactory = sessionFactory;
    }

    public HibernateCriteriaBuilder(Class targetClass, SessionFactory sessionFactory, boolean uniqueResult) {
        super();
        this.targetClass = targetClass;
        this.sessionFactory = sessionFactory;
        this.uniqueResult = uniqueResult;
    }

    public void setUniqueResult(boolean uniqueResult) {
        this.uniqueResult = uniqueResult;
    }

    /**
     * A projection that selects a property name
     * @param propertyName The name of the property
     */
    public void property(String propertyName) {
        if(this.projectionList == null) {
            throwRuntimeException( new IllegalArgumentException("call to [property] must be within a [projections] node"));
        }
        else {
            this.projectionList.add(Projections.property(propertyName));
        }    	
    }
    
    /**
     * A projection that selects a distince property name
     * @param propertyName The property name
     */
    public void distinct(String propertyName) {
        if(this.projectionList == null) {
            throwRuntimeException( new IllegalArgumentException("call to [distinct] must be within a [projections] node"));
        }
        else {
            this.projectionList.add(Projections.distinct(Projections.property(propertyName)));
        }
    }

    /**
     * A distinct projection that takes a list
     *
     * @param propertyNames The list of distince property names
     */
    public void distinct(Collection propertyNames) {
        if(this.projectionList == null) {
            throwRuntimeException( new IllegalArgumentException("call to [distinct] must be within a [projections] node"));
        }
        else {
            ProjectionList list = Projections.projectionList();
            for (Iterator i = propertyNames.iterator(); i.hasNext();) {
                Object o = i.next();
                list.add(Projections.property(o.toString()));
            }
            this.projectionList.add(Projections.distinct(list));
        }
    }
    /**
     * Adds a projection that allows the criteria to return the property average value
     *
     * @param propertyName The name of the property
     */
    public void avg(String propertyName) {
        if(this.projectionList == null) {
            throwRuntimeException( new IllegalArgumentException("call to [avg] must be within a [projections] node"));
        }
        else {
            this.projectionList.add(Projections.avg(propertyName));
        }
    }

    /**
     * Calculates the property name including any alias paths
     *
     * @param propertyName The property name
     * @return The calculated property name
     */
    private String calculatePropertyName(String propertyName) {
        if(this.aliasStack.size()>0) {
            return this.aliasStack.get(this.aliasStack.size()-1).toString()+'.'+propertyName;
        }
        return propertyName;
    }

    /**
     * Calculates the property value, converting GStrings if necessary
     *
     * @param propertyValue The property value
     * @return The calculated property value
     */
    private Object calculatePropertyValue(Object propertyValue) {
        if(propertyValue instanceof GString) {
            return propertyValue.toString();
        }
        return propertyValue;
    }

    /**
     * Adds a projection that allows the criteria to return the property count
     *
     * @param propertyName The name of the property
     */
    public void count(String propertyName) {
        if(this.projectionList == null) {
            throwRuntimeException( new IllegalArgumentException("call to [count] must be within a [projections] node"));
        }
        else {
            this.projectionList.add(Projections.count(propertyName));
        }
    }

    /**
     * Adds a projection that allows the criteria to return the distinct property count
     *
     * @param propertyName The name of the property
     */
    public void countDistinct(String propertyName) {
        if(this.projectionList == null) {
            throwRuntimeException( new IllegalArgumentException("call to [countDistinct] must be within a [projections] node"));
        }
        else {
            this.projectionList.add(Projections.countDistinct(propertyName));
        }
    }

    /**
     * Adds a projection that allows the criteria's result to be grouped by a property
     *
     * @param propertyName The name of the property
     */
    public void groupProperty(String propertyName) {
        if(this.projectionList == null) {
            throwRuntimeException( new IllegalArgumentException("call to [groupProperty] must be within a [projections] node"));
        }
        else {
            this.projectionList.add(Projections.groupProperty(propertyName));
        }
    }

    /**
     * Adds a projection that allows the criteria to retrieve a  maximum property value
     *
     * @param propertyName The name of the property
     */
    public void max(String propertyName) {
        if(this.projectionList == null) {
            throwRuntimeException( new IllegalArgumentException("call to [max] must be within a [projections] node"));
        }
        else {
            this.projectionList.add(Projections.max(propertyName));
        }
    }

    /**
     * Adds a projection that allows the criteria to retrieve a  minimum property value
     *
     * @param propertyName The name of the property
     */
    public void min(String propertyName) {
        if(this.projectionList == null) {
            throwRuntimeException( new IllegalArgumentException("call to [min] must be within a [projections] node"));
        }
        else {
            this.projectionList.add(Projections.min(propertyName));
        }
    }

    /**
     * Adds a projection that allows the criteria to return the row count
     *
     */
    public void rowCount() {
        if(this.projectionList == null) {
            throwRuntimeException( new IllegalArgumentException("call to [rowCount] must be within a [projections] node"));
        }
        else {
            this.projectionList.add(Projections.rowCount());
        }
    }

    /**
     * Adds a projection that allows the criteria to retrieve the sum of the results of a property
     *
     * @param propertyName The name of the property
     */
    public void sum(String propertyName) {
        if(this.projectionList == null) {
            throwRuntimeException( new IllegalArgumentException("call to [sum] must be within a [projections] node"));
        }
        else {
            this.projectionList.add(Projections.sum(propertyName));
        }
    }

    /**
     * Sets the fetch mode of an associated path
     *
     * @param associationPath The name of the associated path
     * @param fetchMode The fetch mode to set
     */
    public void fetchMode(String associationPath, FetchMode fetchMode) {
        if(criteria!=null) {
            criteria.setFetchMode(associationPath, fetchMode);
        }
    }

    /**
     * Sets the resultTransformer.  
     * @param resultTransformer The result transformer to use.
     */
    public void resultTransformer(ResultTransformer resultTransformer) {
		if (criteria == null) {
            throwRuntimeException( new IllegalArgumentException("Call to [resultTransformer] not supported here"));
		}
        this.resultTransformer = resultTransformer;
    }

    /**
     * Creates a Criterion that compares to class properties for equality
     * @param propertyName The first property name
     * @param otherPropertyName The second property name
     * @return A Criterion instance
     */
    public Object eqProperty(String propertyName, String otherPropertyName) {
        if(!validateSimpleExpression()) {
            throwRuntimeException( new IllegalArgumentException("Call to [eqProperty] with propertyName ["+propertyName+"] and other property name ["+otherPropertyName+"] not allowed here.") );
        }
        propertyName = calculatePropertyName(propertyName);
        otherPropertyName = calculatePropertyName(otherPropertyName);
        Criterion c = Restrictions.eqProperty( propertyName, otherPropertyName );

        if(isInsideLogicalExpression()) {
            this.logicalExpressionArgs.add(c);
        }else {
           addToCriteria(c);
        }
        return c;
    }


    /**
     * Creates a Criterion that compares to class properties for !equality
     * @param propertyName The first property name
     * @param otherPropertyName The second property name
     * @return A Criterion instance
     */
    public Object neProperty(String propertyName, String otherPropertyName) {
        if(!validateSimpleExpression()) {
            throwRuntimeException( new IllegalArgumentException("Call to [neProperty] with propertyName ["+propertyName+"] and other property name ["+otherPropertyName+"] not allowed here."));
        }
        propertyName = calculatePropertyName(propertyName);
        otherPropertyName = calculatePropertyName(otherPropertyName);

        Criterion c = Restrictions.neProperty( propertyName, otherPropertyName );

        if(isInsideLogicalExpression()) {
            this.logicalExpressionArgs.add(c);
        }else {
            addToCriteria(c);
        }
        return c;
    }
    /**
     * Creates a Criterion that tests if the first property is greater than the second property
     * @param propertyName The first property name
     * @param otherPropertyName The second property name
     * @return A Criterion instance
     */
    public Object gtProperty(String propertyName, String otherPropertyName) {
        if(!validateSimpleExpression()) {
            throwRuntimeException( new IllegalArgumentException("Call to [gtProperty] with propertyName ["+propertyName+"] and other property name ["+otherPropertyName+"] not allowed here."));
        }
        propertyName = calculatePropertyName(propertyName);
        otherPropertyName = calculatePropertyName(otherPropertyName);

        Criterion c = Restrictions.gtProperty( propertyName, otherPropertyName );

        if(isInsideLogicalExpression()) {
            this.logicalExpressionArgs.add(c);
        }else {
            addToCriteria(c);
        }
        return c;
    }
    /**
     * Creates a Criterion that tests if the first property is greater than or equal to the second property
     * @param propertyName The first property name
     * @param otherPropertyName The second property name
     * @return A Criterion instance
     */
    public Object geProperty(String propertyName, String otherPropertyName) {
        if(!validateSimpleExpression()) {
            throwRuntimeException( new IllegalArgumentException("Call to [geProperty] with propertyName ["+propertyName+"] and other property name ["+otherPropertyName+"] not allowed here."));
        }
        propertyName = calculatePropertyName(propertyName);
        otherPropertyName = calculatePropertyName(otherPropertyName);

        Criterion c = Restrictions.geProperty( propertyName, otherPropertyName );

        if(isInsideLogicalExpression()) {
            this.logicalExpressionArgs.add(c);
        }else {
            addToCriteria(c);
        }
        return c;
    }
    /**
     * Creates a Criterion that tests if the first property is less than the second property
     * @param propertyName The first property name
     * @param otherPropertyName The second property name
     * @return A Criterion instance
     */
    public Object ltProperty(String propertyName, String otherPropertyName) {
        if(!validateSimpleExpression()) {
            throwRuntimeException( new IllegalArgumentException("Call to [ltProperty] with propertyName ["+propertyName+"] and other property name ["+otherPropertyName+"] not allowed here."));
        }
        propertyName = calculatePropertyName(propertyName);
        otherPropertyName = calculatePropertyName(otherPropertyName);

        Criterion c = Restrictions.ltProperty( propertyName, otherPropertyName );

        if(isInsideLogicalExpression()) {
            this.logicalExpressionArgs.add(c);
        }else {
            addToCriteria(c);
        }
        return c;
    }
    /**
     * Creates a Criterion that tests if the first property is less than or equal to the second property
     * @param propertyName The first property name
     * @param otherPropertyName The second property name
     * @return A Criterion instance
     */
    public Object leProperty(String propertyName, String otherPropertyName) {
        if(!validateSimpleExpression()) {
            throwRuntimeException( new IllegalArgumentException("Call to [leProperty] with propertyName ["+propertyName+"] and other property name ["+otherPropertyName+"] not allowed here."));
        }
        propertyName = calculatePropertyName(propertyName);
        otherPropertyName = calculatePropertyName(otherPropertyName);

        Criterion c = Restrictions.leProperty( propertyName, otherPropertyName );

        if(isInsideLogicalExpression()) {
            this.logicalExpressionArgs.add(c);
        }else {
            addToCriteria(c);
        }
        return c;
    }
    /**
     * Creates a "greater than" Criterion based on the specified property name and value
     * @param propertyName The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    public Object gt(String propertyName, Object propertyValue) {
        if(!validateSimpleExpression()) {
            throwRuntimeException( new IllegalArgumentException("Call to [gt] with propertyName ["+propertyName+"] and value ["+propertyValue+"] not allowed here."));
        }
        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);

        Criterion c = Restrictions.gt( propertyName, propertyValue );

        if(isInsideLogicalExpression()) {
            this.logicalExpressionArgs.add(c);
        }else {
            addToCriteria(c);
        }
        return c;
    }



    /**
     * Creates a "greater than or equal to" Criterion based on the specified property name and value
     * @param propertyName The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    public Object ge(String propertyName, Object propertyValue) {
        if(!validateSimpleExpression()) {
            throwRuntimeException( new IllegalArgumentException("Call to [ge] with propertyName ["+propertyName+"] and value ["+propertyValue+"] not allowed here."));
        }
        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);
        Criterion c = Restrictions.ge( propertyName, propertyValue );

        if(isInsideLogicalExpression()) {
            this.logicalExpressionArgs.add(c);
        }else {
            addToCriteria(c);
        }
        return c;
    }
    /**
     * Creates a "less than" Criterion based on the specified property name and value
     * @param propertyName The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    public Object lt(String propertyName, Object propertyValue) {
        if(!validateSimpleExpression()) {
            throwRuntimeException( new IllegalArgumentException("Call to [lt] with propertyName ["+propertyName+"] and value ["+propertyValue+"] not allowed here."));
        }
        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);
        Criterion c = Restrictions.lt( propertyName, propertyValue );

        if(isInsideLogicalExpression()) {
            this.logicalExpressionArgs.add(c);
        }else {
            addToCriteria(c);
        }
        return c;
    }
    /**
     * Creates a "less than or equal to" Criterion based on the specified property name and value
     * @param propertyName The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    public Object le(String propertyName, Object propertyValue) {
        if(!validateSimpleExpression()) {
            throwRuntimeException( new IllegalArgumentException("Call to [le] with propertyName ["+propertyName+"] and value ["+propertyValue+"] not allowed here."));
        }
        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);
        Criterion c = Restrictions.le( propertyName, propertyValue );

        if(isInsideLogicalExpression()) {
            this.logicalExpressionArgs.add(c);
        }else {
            addToCriteria(c);
        }
        return c;
    }
    /**
     * Creates an "equals" Criterion based on the specified property name and value
     * @param propertyName The property name
     * @param propertyValue The property value
     *
     * @return A Criterion instance
     */
    public Object eq(String propertyName, Object propertyValue) {
        if(!validateSimpleExpression()) {
            throwRuntimeException( new IllegalArgumentException("Call to [eq] with propertyName ["+propertyName+"] and value ["+propertyValue+"] not allowed here."));
        }
        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);
        Criterion c = Restrictions.eq( propertyName, propertyValue );

        if(isInsideLogicalExpression()) {
            this.logicalExpressionArgs.add(c);
        }else {
            addToCriteria(c);
        }
        return c;
    }
    /**
     * Creates a Criterion with from the specified property name and "like" expression
     * @param propertyName The property name
     * @param propertyValue The like value
     *
     * @return A Criterion instance
     */
    public Object like(String propertyName, Object propertyValue) {
        if(!validateSimpleExpression()) {
            throwRuntimeException( new IllegalArgumentException("Call to [like] with propertyName ["+propertyName+"] and value ["+propertyValue+"] not allowed here."));
        }
        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);
        Criterion c = Restrictions.like( propertyName, propertyValue );

        if(isInsideLogicalExpression()) {
            this.logicalExpressionArgs.add(c);
        }else {
            addToCriteria(c);
        }
        return c;
    }
    /**
     * Creates a Criterion with from the specified property name and "ilike" (a case sensitive version of "like") expression
     * @param propertyName The property name
     * @param propertyValue The ilike value
     *
     * @return A Criterion instance
     */
    public Object ilike(String propertyName, Object propertyValue) {
        if(!validateSimpleExpression()) {
            throwRuntimeException( new IllegalArgumentException("Call to [ilike] with propertyName ["+propertyName+"] and value ["+propertyValue+"] not allowed here."));
        }
        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);
        Criterion c = Restrictions.ilike( propertyName, propertyValue );

        if(isInsideLogicalExpression()) {
            this.logicalExpressionArgs.add(c);
        }else {
            addToCriteria(c);
        }
        return c;
    }
    /**
     * Applys a "in" contrain on the specified property
     * @param propertyName The property name
     * @param values A collection of values
     *
     * @return A Criterion instance
     */
    public Object in(String propertyName, Collection values) {
        if(!validateSimpleExpression()) {
            throwRuntimeException( new IllegalArgumentException("Call to [in] with propertyName ["+propertyName+"] and values ["+values+"] not allowed here."));
        }
        propertyName = calculatePropertyName(propertyName);
        Criterion c = Restrictions.in( propertyName, values );

        if(isInsideLogicalExpression()) {
            this.logicalExpressionArgs.add(c);
        }else {
            addToCriteria(c);
        }
        return c;
    }

	/**
	 * Delegates to in as in is a Groovy keyword
	 **/
	public Object inList(String propertyName, Collection values) {
		return in(propertyName, values);
	}
	
	/**
	 * Delegates to in as in is a Groovy keyword
	 **/
	public Object inList(String propertyName, Object[] values) {
		return in(propertyName, values);
	}
		
    /**
     * Applys a "in" contrain on the specified property
     * @param propertyName The property name
     * @param values A collection of values
     *
     * @return A Criterion instance
     */
    public Object in(String propertyName, Object[] values) {
        if(!validateSimpleExpression()) {
            throwRuntimeException( new IllegalArgumentException("Call to [in] with propertyName ["+propertyName+"] and values ["+values+"] not allowed here."));
        }
        propertyName = calculatePropertyName(propertyName);
        Criterion c = Restrictions.in( propertyName, values );

        if(isInsideLogicalExpression()) {
            this.logicalExpressionArgs.add(c);
        }else {
            addToCriteria(c);
        }
        return c;
    }

    /**
     * Orders by the specified property name (defaults to ascending)
     *
     * @param propertyName The property name to order by
     * @return A Order instance
     */
    public Object order(String propertyName) {
        if(this.criteria == null)
                throwRuntimeException( new IllegalArgumentException("Call to [order] with propertyName ["+propertyName+"]not allowed here."));
        propertyName = calculatePropertyName(propertyName);
        Order o = Order.asc(propertyName);
        this.criteria.addOrder(o);

        return o;
    }

    /**
     * Orders by the specified property name and direction
     *
     * @param propertyName The property name to order by
     * @param direction Either "asc" for ascending or "desc" for descending
     *
     * @return A Order instance
     */
    public Object order(String propertyName, String direction) {
        if(this.criteria == null)
                throwRuntimeException( new IllegalArgumentException("Call to [order] with propertyName ["+propertyName+"]not allowed here."));
        propertyName = calculatePropertyName(propertyName);
        Order o;
        if(direction.equals( ORDER_DESCENDING )) {
            o = Order.desc(propertyName);
        }
        else {
            o = Order.asc(propertyName);
        }
        this.criteria.addOrder(o);

        return o;
    }
    /**
     * Creates a Criterion that contrains a collection property by size
     *
     * @param propertyName The property name
     * @param size The size to constrain by
     *
     * @return A Criterion instance
     */
    public Object sizeEq(String propertyName, int size) {
        if(!validateSimpleExpression()) {
            throwRuntimeException( new IllegalArgumentException("Call to [sizeEq] with propertyName ["+propertyName+"] and size ["+size+"] not allowed here."));
        }
        propertyName = calculatePropertyName(propertyName);
        Criterion c = Restrictions.sizeEq( propertyName, size );

        if(isInsideLogicalExpression()) {
            this.logicalExpressionArgs.add(c);
        }else {
            addToCriteria(c);
        }
        return c;
    }

    /**
     * Creates a "not equal" Criterion based on the specified property name and value
     * @param propertyName The property name
     * @param propertyValue The property value
     * @return The criterion object
     */
    public Object ne(String propertyName, Object propertyValue) {
        if(!validateSimpleExpression()) {
            throwRuntimeException( new IllegalArgumentException("Call to [ne] with propertyName ["+propertyName+"] and value ["+propertyValue+"] not allowed here."));
        }
        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);
        Criterion c = Restrictions.ne( propertyName, propertyValue );
        if(isInsideLogicalExpression()) {
            this.logicalExpressionArgs.add(c);
        }else {
            addToCriteria(c);
        }
        return c;
    }

	public Object notEqual(String propertyName, Object propertyValue) {
		return ne(propertyName, propertyValue);
	}
    /**
     * Creates a "between" Criterion based on the property name and specified lo and hi values
     * @param propertyName The property name
     * @param lo The low value
     * @param hi The high value
     * @return A Criterion instance
     */
    public Object between(String propertyName, Object lo, Object hi) {
        if(!validateSimpleExpression()) {
            throwRuntimeException( new IllegalArgumentException("Call to [between] with propertyName ["+propertyName+"]  not allowed here."));
        }
        propertyName = calculatePropertyName(propertyName);
        Criterion c = Restrictions.between( propertyName, lo,hi);
        if(isInsideLogicalExpression()) {
            this.logicalExpressionArgs.add(c);
        }else {
            addToCriteria(c);
        }
        return c;
    }

    protected Object createNode(Object name) {
        return createNode( name, Collections.EMPTY_MAP );
    }

    private boolean validateSimpleExpression() {
        if(this.criteria == null)
            return false;

        return !(!isInsideLogicalExpression() &&
                !(this.parent instanceof Proxy) &&
                this.parent != null &&
                this.aliasStack.size() == 0);

    }

    private boolean isInsideLogicalExpression() {
        if(this.logicalExpressions.size() > 0) {
            String currentLogicalExpression = (String)this.logicalExpressions.get( this.logicalExpressions.size() - 1 );
            if(currentLogicalExpression.equals( AND ) ||
               currentLogicalExpression.equals( OR ) ||
               currentLogicalExpression.equals( NOT ))
                    return true;
        }
        return false;
    }

    protected Object createNode(Object name, Map attributes) {
        if(name.equals(ROOT_CALL) ||
                name.equals(LIST_CALL) ||
                name.equals(GET_CALL) ||
                name.equals(COUNT_CALL) ||
                name.equals(SCROLL_CALL)) {

            if(this.criteria != null)
                throwRuntimeException( new IllegalArgumentException("call to [" + name + "] not supported here"));

            if(name.equals(GET_CALL))
                this.uniqueResult = true;
            if(name.equals(SCROLL_CALL)) {
                this.scroll = true;
            }
            else if(name.equals(COUNT_CALL)) {
                this.count = true;
            }

            if(TransactionSynchronizationManager.hasResource(sessionFactory)) {
                this.participate = true;
                this.session = ((SessionHolder)TransactionSynchronizationManager.getResource(sessionFactory)).getSession();
            }
            else {
                this.session = sessionFactory.openSession();
            }
            this.criteria = this.session.createCriteria(targetClass);
            this.criteriaProxy = new ExtendProxy();
            this.criteriaProxy.setAdaptee(this.criteria);
            resultProxy = new ExtendProxy();
            this.parent = resultProxy;

            return resultProxy;
        } else if(name.equals( AND ) ||
                  name.equals( OR ) ||
                  name.equals( NOT )) {
            if(this.criteria == null)
                throwRuntimeException( new IllegalArgumentException("call to [" + name + "] not supported here"));

            this.logicalExpressions.add(name);
            return name;
        } else if(name.equals( PROJECTIONS )) {
            if(this.criteria == null)
                throwRuntimeException( new IllegalArgumentException("call to [" + name + "] not supported here"));

            this.projectionList = Projections.projectionList();

            return name;
        }
        else if(targetBean.isReadableProperty(name.toString())) {
            this.criteria.createAlias(name.toString(), name.toString()+ALIAS,CriteriaSpecification.LEFT_JOIN);
            this.aliasStack.add(name.toString()+ALIAS);
            return name;

        }

        closeSessionFollowingException();
        throw new MissingMethodException((String) name, getClass(), new Object[] {}) ;
    }


    protected void nodeCompleted(Object parent, Object node) {
        if(node instanceof Proxy) {
            if(resultTransformer != null) {
                this.criteria.setResultTransformer(resultTransformer);
            }
            if(!uniqueResult) {
                if(scroll) {
                    resultProxy.setAdaptee(
                            this.criteria.scroll()
                    );
                }
                else if(count) {
                    this.criteria.setProjection(Projections.rowCount());
                    resultProxy.setAdaptee(
                                this.criteria.uniqueResult()
                            );
                }
                else {
                    resultProxy.setAdaptee(
                            this.criteria.list()
                    );
                }
            }
            else {
                resultProxy.setAdaptee(
                        this.criteria.uniqueResult()
                );
            }
            this.criteria = null;
            if(!this.participate) {
                this.session.close();
            }
        }
        else if(node.equals( AND ) ||
                node.equals( OR )) {
            Criterion c = null;
            if(logicalExpressionArgs.size() == 1 && node.equals(AND)) {
                  c =(Criterion)logicalExpressionArgs.remove(0);
            }
            else if(logicalExpressionArgs.size() == 2) {
                Criterion lhs = (Criterion)logicalExpressionArgs.remove(0);
                Criterion rhs = (Criterion)logicalExpressionArgs.remove(0);
                if(node.equals(OR))  {
                    c = Restrictions.or(lhs,rhs);
                }
                else {
                    c = Restrictions.and(lhs,rhs);
                }
            }
            else if(logicalExpressionArgs.size() > 2) {
                if(node.equals(OR)) {
                    c = Restrictions.disjunction();
                }
                else {
                    c = Restrictions.conjunction();
                }
                for (Iterator i = logicalExpressionArgs.iterator(); i.hasNext();) {
                    Criterion criterion = (Criterion) i.next();
                    ((Junction)c).add(criterion);
                }

            }
            if(c!=null) {
                if(parent instanceof Proxy) {
                    addToCriteria( c );
                }
                else if(parent.equals( AND ) ||
                        parent.equals( OR )) {

                    this.logicalExpressionArgs.add(c );
                    this.logicalExpressions.remove(this.logicalExpressions.size() - 1);
                }

            }
        }
        else if(node.equals(NOT)) {
            if(this.logicalExpressionArgs.size() < 1)
                throwRuntimeException( new IllegalArgumentException("Logical expression [" + node +"] must contain at least 1 expression"));

            Criterion c = (Criterion)this.logicalExpressionArgs.remove(this.logicalExpressionArgs.size() - 1);

            if(parent instanceof Proxy) {
                addToCriteria( Restrictions.not( c ) );
            }
            else if(parent.equals( AND ) ||
                    parent.equals( OR ) ||
                    parent.equals( NOT )) {
                this.logicalExpressionArgs.add( Restrictions.not( c ) );
                this.logicalExpressions.remove(this.logicalExpressions.size() - 1);
            }
        }
        else if(node.equals(PROJECTIONS)) {
            if(this.projectionList != null && this.projectionList.getLength() > 0) {
                this.criteria.setProjection(this.projectionList);
            }
        }
        else if(targetBean.isReadableProperty(node.toString()) && aliasStack.size() > 0) {
            aliasStack.remove(aliasStack.size()-1);
        }
        super.nodeCompleted(parent, node);
    }

    /**
     * Throws a runtime exception where necessary to ensure the session gets closed
     */
    private void throwRuntimeException(RuntimeException t) {
        closeSessionFollowingException();
        throw t;
    }

    private void closeSessionFollowingException() {
        if(this.session != null && this.session.isOpen() && !this.participate) {
            this.session.close();
        }
        if(this.criteria != null) {
            this.criteria = null;
        }
    }

    protected void setParent(Object parent, Object child) {
        this.parent = parent;
    }

    protected Object createNode(Object name, Object value) {
        return createNode(name, Collections.EMPTY_MAP, value);
    }

    protected Object createNode(Object name, Map attributes, Object value) {
        if(this.criteria == null)
            throwRuntimeException( new IllegalArgumentException("call to [" + name + "] not supported here"));

        Criterion c = null;
        if(name.equals(ID_EQUALS)) {
            c = Restrictions.idEq(value);
        }
        else {

            if(	name.equals( IS_NULL ) ||
                name.equals( IS_NOT_NULL ) ||
                name.equals( IS_EMPTY ) ||
                name.equals( IS_NOT_EMPTY )) {
                if(!(value instanceof String))
                    throwRuntimeException( new IllegalArgumentException("call to [" + name + "] with value ["+value+"] requires a String value."));

                if(name.equals( IS_NULL )) {
                    c = Restrictions.isNull( (String)value ) ;
                }
                else if(name.equals( IS_NOT_NULL )) {
                    c = Restrictions.isNotNull( (String)value );

                }
                else if(name.equals( IS_EMPTY )) {
                    c = Restrictions.isEmpty( (String)value );
                }
                else if(name.equals( IS_NOT_EMPTY )) {
                    c = Restrictions.isNotEmpty( (String)value );
                }
            }
        }
        if(c != null) {
            if(isInsideLogicalExpression()) {
                this.logicalExpressionArgs.add(c);
            }
            else {
                addToCriteria( c );
            }
            return c;
        }
        else {
            String nameString = name.toString();
            if(value instanceof Closure) {
                if(targetBean.isReadableProperty(nameString)) {
                    this.criteria.createAlias(nameString, nameString+ALIAS,CriteriaSpecification.LEFT_JOIN);
                    this.aliasStack.add(nameString+ALIAS);
                    return name;
                }
            }
            else if(parent instanceof Proxy) {
                try {
                    criteriaProxy.setProperty(nameString, value);
                    return criteria;
                }
                catch(MissingPropertyException mpe) {
                    throwRuntimeException( new MissingMethodException(nameString, getClass(), new Object[] {value}) );
                }
            }

            throwRuntimeException( new MissingMethodException(nameString, getClass(), new Object[] {value}));
        }
        return c;
    }

    private void addToCriteria(Criterion c) {
        this.criteria.add(c);
    }

}

