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

import groovy.lang.*;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.hibernate.*;
import org.hibernate.criterion.*;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;

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
public class HibernateCriteriaBuilder extends GroovyObjectSupport {

    public static final String AND = "and"; // builder
    public static final String IS_NULL = "isNull"; // builder
    public static final String IS_NOT_NULL = "isNotNull"; // builder
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


    private static final String ROOT_DO_CALL = "doCall";
    private static final String ROOT_CALL = "call";
    private static final String LIST_CALL = "list";
    private static final String LIST_DISTINCT_CALL = "listDistinct";
    private static final String COUNT_CALL = "count";
    private static final String GET_CALL = "get";
    private static final String SCROLL_CALL = "scroll";


    private static final String PROJECTIONS = "projections";
    private SessionFactory sessionFactory;
    private Session session;
    private Class targetClass;
    private Criteria criteria;
    private MetaClass criteriaMetaClass;

    private boolean uniqueResult = false;
    private Stack logicalExpressionStack = new Stack();
    private Stack associationStack = new Stack();
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
        return addToCriteria(Restrictions.eqProperty( propertyName, otherPropertyName ));
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
        return addToCriteria(Restrictions.neProperty( propertyName, otherPropertyName ));
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
        return addToCriteria(Restrictions.gtProperty( propertyName, otherPropertyName ));
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
        return addToCriteria(Restrictions.geProperty( propertyName, otherPropertyName ));
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
        return addToCriteria(Restrictions.ltProperty( propertyName, otherPropertyName ));
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
        return addToCriteria(Restrictions.leProperty(propertyName, otherPropertyName));
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
        return addToCriteria(Restrictions.gt(propertyName, propertyValue));
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
        return addToCriteria(Restrictions.ge(propertyName, propertyValue));
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
        return addToCriteria(Restrictions.lt(propertyName, propertyValue));
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
        return addToCriteria(Restrictions.le(propertyName, propertyValue));
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
        return addToCriteria(Restrictions.eq(propertyName, propertyValue));
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
        return addToCriteria(Restrictions.like(propertyName, propertyValue));
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
        return addToCriteria(Restrictions.ilike(propertyName, propertyValue));
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
        return addToCriteria(Restrictions.in(propertyName, values));
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
        return addToCriteria(Restrictions.in(propertyName, values));
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
        return addToCriteria(Restrictions.sizeEq(propertyName, size));
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
        return addToCriteria(Restrictions.ne(propertyName, propertyValue));
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
        return addToCriteria(Restrictions.between(propertyName, lo, hi));
    }


    private boolean validateSimpleExpression() {
        if(this.criteria == null) {
            return false;
        }
        return true;
    }



    public Object invokeMethod(String name, Object obj) {
        Object[] args = obj.getClass().isArray() ? (Object[])obj : new Object[]{obj};

        
        if(isCriteriaConstructionMethod(name, args)) {

            if(this.criteria != null) {
                throwRuntimeException( new IllegalArgumentException("call to [" + name + "] not supported here"));
            }


            if (name.equals(GET_CALL)) {
                this.uniqueResult = true;
            }
            else if (name.equals(SCROLL_CALL)) {
                this.scroll = true;
            }
            else if (name.equals(COUNT_CALL)) {
                this.count = true;
            }
            else if (name.equals(LIST_DISTINCT_CALL)) {
                this.resultTransformer = CriteriaSpecification.DISTINCT_ROOT_ENTITY;
            }

            boolean paginationEnabledList = false;

            createCriteriaInstance();

            // Check for pagination params
            if(name.equals(LIST_CALL) && args.length == 2) {
                paginationEnabledList = true;
                invokeClosureNode(args[1]);
            } else {
                invokeClosureNode(args[0]);
            }


           if(resultTransformer != null) {
                this.criteria.setResultTransformer(resultTransformer);
            }
            Object result;
            if(!uniqueResult) {
                if(scroll) {
                    result = this.criteria.scroll();
                }
                else if(count) {
                    this.criteria.setProjection(Projections.rowCount());
                    result = this.criteria.uniqueResult();
                } else if(paginationEnabledList) {
                    // Calculate how many results there are in total. This has been
                    // moved to before the 'list()' invocation to avoid any "ORDER
                    // BY" clause added by 'populateArgumentsForCriteria()', otherwise
                    // an exception is thrown for non-string sort fields (GRAILS-2690).
                    this.criteria.setFirstResult(0);
                    this.criteria.setMaxResults(Integer.MAX_VALUE);
                    this.criteria.setProjection(Projections.rowCount());
                    int totalCount = ((Integer)this.criteria.uniqueResult()).intValue();

                    // Drop the projection, add settings for the pagination parameters,
                    // and then execute the query.
                    this.criteria.setProjection(null);
                    this.criteria.setResultTransformer(CriteriaSpecification.ROOT_ENTITY);
                    GrailsHibernateUtil.populateArgumentsForCriteria(this.criteria, (Map)args[0]);
                    PagedResultList pagedRes = new PagedResultList(this.criteria.list());

                    // Updated the paged results with the total number of records
                    // calculated previously.
                    pagedRes.setTotalCount(totalCount);
                    result = pagedRes;
                } else {
                    result = this.criteria.list();
                }
            }
            else {
                result = this.criteria.uniqueResult();
            }
            if(!this.participate) {
                this.session.close();
            }
            return result;

        }
        else {

            if(criteria==null) createCriteriaInstance();

           MetaMethod metaMethod = getMetaClass().getMetaMethod(name, args);
            if(metaMethod != null) {
                 return metaMethod.invoke(this, args);
            }

            metaMethod = criteriaMetaClass.getMetaMethod(name, args);
            if(metaMethod != null) {
                 return metaMethod.invoke(criteria, args);
            }
            metaMethod = criteriaMetaClass.getMetaMethod(GrailsClassUtils.getSetterName(name), args);
            if(metaMethod != null) {
                 return metaMethod.invoke(criteria, args);
            }
           else if(args.length == 1 && args[0] instanceof Closure) {
                if(name.equals( AND ) ||
                        name.equals( OR ) ||
                        name.equals( NOT ) ) {
                    if(this.criteria == null)
                        throwRuntimeException( new IllegalArgumentException("call to [" + name + "] not supported here"));

                    this.logicalExpressionStack.add(new LogicalExpression(name));
                    invokeClosureNode(args[0]);

                    LogicalExpression logicalExpression = (LogicalExpression) logicalExpressionStack.pop();
                    addToCriteria(logicalExpression.toCriterion());

                    return name;
                } else if(name.equals( PROJECTIONS ) && args.length == 1 && (args[0] instanceof Closure)) {
                    if(this.criteria == null)
                        throwRuntimeException( new IllegalArgumentException("call to [" + name + "] not supported here"));

                    this.projectionList = Projections.projectionList();
                    invokeClosureNode(args[0]);

                    if(this.projectionList != null && this.projectionList.getLength() > 0) {
                        this.criteria.setProjection(this.projectionList);
                    }


                    return name;
                }
                else if(targetBean.isReadableProperty(name.toString())) {
                    ClassMetadata meta = sessionFactory.getClassMetadata(targetBean.getWrappedClass());
                    Type type = meta.getPropertyType(name.toString());
                    if (type.isAssociationType()) {
                        String otherSideEntityName =
                                ((AssociationType) type).getAssociatedEntityName((SessionFactoryImplementor) sessionFactory);
                        Class oldTargetClass = targetClass;
                        targetClass = sessionFactory.getClassMetadata(otherSideEntityName).getMappedClass(EntityMode.POJO);
                        BeanWrapper oldTargetBean = targetBean;
                        targetBean = new BeanWrapperImpl(BeanUtils.instantiateClass(targetClass));
                        associationStack.push(name.toString());
                        String newAlias = name.toString() + ALIAS + aliasStack.size();
                        StringBuffer fullPath = new StringBuffer();
                        for (Iterator i = associationStack.iterator(); i.hasNext();) {
                            String propertyName = (String) i.next();
                            if(fullPath.length() > 0 ) fullPath.append(".");
                            fullPath.append(propertyName);
                        }
                        this.criteria.createAlias(fullPath.toString(), newAlias, CriteriaSpecification.LEFT_JOIN);
                        this.aliasStack.add(newAlias);
                        // the criteria within an association node are grouped with an implicit AND
                        logicalExpressionStack.push(new LogicalExpression(AND));
                        invokeClosureNode(args[0]);
                        aliasStack.remove(aliasStack.size() - 1);
                        LogicalExpression logicalExpression = (LogicalExpression) logicalExpressionStack.pop();
                        if (!logicalExpression.args.isEmpty()) {
                            addToCriteria(logicalExpression.toCriterion());
                        }
                        associationStack.pop();
                        targetClass = oldTargetClass;
                        targetBean = oldTargetBean;
                        return name;
                    }
                }
            }
            else if(args.length == 1 && args[0] != null) {
                if(this.criteria == null)
                    throwRuntimeException( new IllegalArgumentException("call to [" + name + "] not supported here"));

                Object value = args[0];
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
                        String propertyName = calculatePropertyName((String)value);
                        if(name.equals( IS_NULL )) {
                            c = Restrictions.isNull( propertyName ) ;
                        }
                        else if(name.equals( IS_NOT_NULL )) {
                            c = Restrictions.isNotNull( propertyName );
                        }
                        else if(name.equals( IS_EMPTY )) {
                            c = Restrictions.isEmpty( propertyName );
                        }
                        else if(name.equals( IS_NOT_EMPTY )) {
                            c = Restrictions.isNotEmpty(propertyName );
                        }
                    }
                }
                if(c != null) {
                    return addToCriteria(c);
                }

            }
        }


        closeSessionFollowingException();
        throw new MissingMethodException(name, getClass(), args) ;
    }

    private boolean isCriteriaConstructionMethod(String name, Object[] args) {
        return (name.equals(LIST_CALL) && args.length == 2 && args[0] instanceof Map && args[1] instanceof Closure) ||
               (name.equals(ROOT_CALL) ||
                name.equals(ROOT_DO_CALL) ||
                name.equals(LIST_CALL) ||
                name.equals(LIST_DISTINCT_CALL) ||
                name.equals(GET_CALL) ||
                name.equals(COUNT_CALL) ||
                name.equals(SCROLL_CALL) && args.length == 1 && args[0] instanceof Closure);
    }

    private void createCriteriaInstance() {
        if(TransactionSynchronizationManager.hasResource(sessionFactory)) {
            this.participate = true;
            this.session = ((SessionHolder)TransactionSynchronizationManager.getResource(sessionFactory)).getSession();
        }
        else {
            this.session = sessionFactory.openSession();
        }
        
        this.criteria = this.session.createCriteria(targetClass);
        this.criteriaMetaClass = GroovySystem.getMetaClassRegistry().getMetaClass(criteria.getClass());
    }

    private void invokeClosureNode(Object args) {
        Closure callable = (Closure)args;
        callable.setDelegate(this);
        callable.call();
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


    /**
     * adds and returns the given criterion to the currently active criteria set.
     * this might be either the root criteria or a currently open
     * LogicalExpression.
     */
    private Criterion addToCriteria(Criterion c) {
        if (!logicalExpressionStack.isEmpty()) {
            ((LogicalExpression) logicalExpressionStack.peek()).args.add(c);
        }
        else {
            this.criteria.add(c);
        }
        return c;
    }

	/**
	 * instances of this class are pushed onto the logicalExpressionStack
	 * to represent all the unfinished "and", "or", and "not" expressions.
	 */
    private class LogicalExpression {
        final Object name;
        final ArrayList args = new ArrayList();
        
        LogicalExpression(Object name) {
            this.name = name;
        }
        
        Criterion toCriterion() {
            if (name.equals(NOT)) {
                switch (args.size()) {
                    case 0:
                        throwRuntimeException(new IllegalArgumentException("Logical expression [not] must contain at least 1 expression"));
		                return null;
                    
                    case 1:
                        return Restrictions.not((Criterion) args.get(0));
                        
                    default:
                        // treat multiple sub-criteria as an implicit "OR"
                        return Restrictions.not(buildJunction(Restrictions.disjunction(), args));
                }
            }
            else if (name.equals(AND)) {
                return buildJunction(Restrictions.conjunction(), args);
            }
            else if (name.equals(OR)) {
                return buildJunction(Restrictions.disjunction(), args);
            }
            else {
                throwRuntimeException(new IllegalStateException("Logical expression [" + name + "] not handled!"));
                return null;
            }
        }
        
        // add the Criterion objects in the given list to the given junction.
        Junction buildJunction(Junction junction, List criteria) {
            for (Iterator i = criteria.iterator(); i.hasNext();) {
                junction.add((Criterion) i.next());
            }
            
            return junction;
        }
    }

}

