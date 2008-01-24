/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.orm.hibernate.validation;

import groovy.lang.GString;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.validation.Errors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A constraint that validates the uniqueness of a property (will query the 
 * database during validation process).
 *
 * @author Graeme Rocher
 * @author Sergey Nebolsin
 * @since 0.4
 *        <p/>
 *        Created: Jan 23, 2007
 *        Time: 2:00:55 PM
 */
public class UniqueConstraint extends AbstractPersistentConstraint {

    private static final String DEFAULT_NOT_UNIQUE_MESSAGE_CODE = "default.not.unique.message";


    public static final String UNIQUE_CONSTRAINT = "unique";
    

    private boolean unique;
    private List uniquenessGroup = new ArrayList();


    /**
     * @return Returns the unique.
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * @return Whether the property is unique within a group
     */
    public boolean isUniqueWithinGroup() {
        return uniquenessGroup.size() > 0;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.validation.ConstrainedProperty.AbstractConstraint#setParameter(java.lang.Object)
     */
    public void setParameter(Object constraintParameter) {
        if(!(constraintParameter instanceof Boolean || 
        		constraintParameter instanceof String || 
        		constraintParameter instanceof GString ||
        		constraintParameter instanceof List)) {
            throw new IllegalArgumentException("Parameter for constraint ["+UNIQUE_CONSTRAINT+"] of property ["+constraintPropertyName+"] of class ["+constraintOwningClass+"] must be a boolean or string value");
        }

        if( constraintParameter instanceof List ) {
        	for( Iterator it = ((List)constraintParameter).iterator(); it.hasNext(); ) {
        		Object parameter = it.next();
                if(!(parameter instanceof String || parameter instanceof GString) ) {
                    throw new IllegalArgumentException("Parameter for constraint ["+UNIQUE_CONSTRAINT+"] of property ["+constraintPropertyName+"] of class ["+constraintOwningClass+"] must be a boolean or string value");
                } else {
                	this.uniquenessGroup.add(parameter.toString());
                }
        	}
        } else if( constraintParameter instanceof String || constraintParameter instanceof GString ) {
        	this.uniquenessGroup.add(constraintParameter.toString());
        	this.unique = true;
        } else {
        	this.unique = ((Boolean)constraintParameter).booleanValue();
        }
    	if( uniquenessGroup.size() > 0 ) {
            unique = true;
            for( Iterator it = uniquenessGroup.iterator(); it.hasNext(); ) {
    			String propertyName = (String) it.next();
    			if( GrailsClassUtils.getPropertyType(constraintOwningClass, propertyName) == null ) {
    				throw new IllegalArgumentException("Scope for constraint ["+UNIQUE_CONSTRAINT+"] of property ["+constraintPropertyName+"] of class ["+constraintOwningClass+"] must be a valid property name of same class"); 
    			}
    		}
    	} 

        super.setParameter(constraintParameter);
    }

    public String getName() {
        return UNIQUE_CONSTRAINT;
    }

    protected void processValidate(final Object target, final Object propertyValue, Errors errors) {

        if(unique) {
        	final Object id = InvokerHelper.invokeMethod(target, "ident",null);
            HibernateTemplate hibernateTemplate = getHibernateTemplate();
            if(hibernateTemplate == null) throw new IllegalStateException("Unable use [unique] constraint, no Hibernate SessionFactory found!");
            List results = hibernateTemplate.executeFind( new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException {
                    session.setFlushMode(FlushMode.MANUAL);

                    try {
                    	Criteria criteria = session.createCriteria( constraintOwningClass )
                    		.add( Restrictions.eq( constraintPropertyName, propertyValue ) );
                    	if( uniquenessGroup != null ) {
                    		for( Iterator it = uniquenessGroup.iterator(); it.hasNext(); ) {
                    			String propertyName = (String) it.next();
                    			criteria.add(Restrictions.eq( propertyName, 
                    					GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(target, propertyName)));
                    		}
                    	}
                        return criteria.list();
                    } finally {
                        session.setFlushMode(FlushMode.AUTO);
                    }
                }
            });

            if(results.size() > 0) {
                boolean reject = false;
                if(id != null) {
                    Object existing = results.get(0);
                    Object existingId = InvokerHelper.invokeMethod(existing, "ident", null);
                    if(!id.equals(existingId)) {
                        reject = true;
                    }
                }
                else {
                    reject = true;
                }
                if(reject) {
                    Object[] args = new Object[] { constraintPropertyName, constraintOwningClass, propertyValue };
                    super.rejectValue(target, errors, UNIQUE_CONSTRAINT, args, getDefaultMessage(DEFAULT_NOT_UNIQUE_MESSAGE_CODE));
                }
            }
        }
    }
    
    public boolean supports(Class type) {
   		return true;
    }
}
