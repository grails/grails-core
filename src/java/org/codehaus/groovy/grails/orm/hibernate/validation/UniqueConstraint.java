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

import org.codehaus.groovy.grails.commons.*;
import org.codehaus.groovy.grails.exceptions.GrailsRuntimeException;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.validation.Errors;

import java.util.ArrayList;
import java.util.Collections;
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
        		constraintParameter instanceof CharSequence ||
        		constraintParameter instanceof List)) {
            throw new IllegalArgumentException("Parameter for constraint ["+UNIQUE_CONSTRAINT+"] of property ["+constraintPropertyName+"] of class ["+constraintOwningClass+"] must be a boolean or string value");
        }

        if( constraintParameter instanceof List ) {
            for (Object parameter : ((List) constraintParameter)) {
                if (!(parameter instanceof String || parameter instanceof CharSequence)) {
                    throw new IllegalArgumentException("Parameter for constraint [" + UNIQUE_CONSTRAINT + "] of property [" + constraintPropertyName + "] of class [" + constraintOwningClass + "] must be a boolean or string value");
                }
                else {
                    this.uniquenessGroup.add(parameter.toString());
                }
            }
        } else if( constraintParameter instanceof String || constraintParameter instanceof CharSequence ) {
        	this.uniquenessGroup.add(constraintParameter.toString());
        	this.unique = true;
        } else {
        	this.unique = ((Boolean)constraintParameter).booleanValue();
        }
    	if( uniquenessGroup.size() > 0 ) {
            unique = true;
            for (Object anUniquenessGroup : uniquenessGroup) {
                String propertyName = (String) anUniquenessGroup;
                if (GrailsClassUtils.getPropertyType(constraintOwningClass, propertyName) == null) {
                    throw new IllegalArgumentException("Scope for constraint [" + UNIQUE_CONSTRAINT + "] of property [" + constraintPropertyName + "] of class [" + constraintOwningClass + "] must be a valid property name of same class");
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
            final Object id;
            try {
                id = InvokerHelper.invokeMethod(target, "ident",null);
            }
            catch (Exception e) {
                throw new GrailsRuntimeException("Target of [unique] constraints ["+ target +"] is not a domain instance. Unique constraint can only be applied to to domain classes and not custom user types or embedded instances");
            }
            HibernateTemplate hibernateTemplate = getHibernateTemplate();
            if(hibernateTemplate == null) throw new IllegalStateException("Unable use [unique] constraint, no Hibernate SessionFactory found!");
            List results = hibernateTemplate.executeFind( new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException {
                    session.setFlushMode(FlushMode.MANUAL);

                    try {
                        boolean shouldValidate = true;
                        Class constraintClass = constraintOwningClass;
                        if(propertyValue != null && DomainClassArtefactHandler.isDomainClass(propertyValue.getClass())) {
                            shouldValidate = session.contains(propertyValue);
                        }
                        if(shouldValidate) {
	                         GrailsApplication application  = (GrailsApplication) applicationContext.getBean(GrailsApplication.APPLICATION_ID);
 	 	 	                 GrailsDomainClass domainClass = (GrailsDomainClass) application.getArtefact(DomainClassArtefactHandler.TYPE,constraintClass.getName());
 	 	 		             if(domainClass!=null && !domainClass.isRoot() ) {
 	 	 	                        GrailsDomainClassProperty property = domainClass.getPropertyByName(constraintPropertyName);
                                    while(property.isInherited() && domainClass!=null) {
                                        domainClass = (GrailsDomainClass) application.getArtefact(DomainClassArtefactHandler.TYPE,domainClass.getClazz().getSuperclass().getName());
                                        if(domainClass!=null) {
                                            property = domainClass.getPropertyByName(constraintPropertyName);
                                        }
                                    }
	                                constraintClass = domainClass != null ? domainClass.getClazz() : constraintClass;

                            }
                            Criteria criteria = session.createCriteria( constraintClass )
                                    .add( Restrictions.eq( constraintPropertyName, propertyValue ) );
                            if( uniquenessGroup != null ) {
                                for (Object anUniquenessGroup : uniquenessGroup) {
                                    String uniquenessGroupPropertyName = (String) anUniquenessGroup;
                                    Object uniquenessGroupPropertyValue = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(target, uniquenessGroupPropertyName);

                                    if (DomainClassArtefactHandler.isDomainClass(uniquenessGroupPropertyValue.getClass())) {
                                        try {
                                            // We are merely verifying that the object is not transient here
                                            session.lock(uniquenessGroupPropertyValue, LockMode.NONE);
                                        }
                                        catch (TransientObjectException e) {
                                            shouldValidate = false;
                                        }
                                    }
                                    if (shouldValidate) {
                                        criteria.add(Restrictions.eq(uniquenessGroupPropertyName, uniquenessGroupPropertyValue));
                                    }
                                    else {
                                        break; // we aren't validating, so no point continuing
                                    }
                                }
                            }
                            
                            if (shouldValidate) {
                                return criteria.list();
                            } else {
                                return Collections.EMPTY_LIST;
                            }
                        }
                        else {
                            return Collections.EMPTY_LIST;
                        }
                    } finally {
                        session.setFlushMode(FlushMode.AUTO);
                    }
                }
            });

            if(results.size() > 0) {
                boolean reject = false;
                if(id != null) {
                    Object existing = results.get(0);
                    Object existingId = null;
                    try {
                        existingId = InvokerHelper.invokeMethod(existing, "ident", null);
                    }
                    catch (Exception e) {
                        // result is not a domain class
                    }
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

    public List getUniquenessGroup() {
        return uniquenessGroup;
    }
    
    public boolean supports(Class type) {
   		return true;
    }
}
