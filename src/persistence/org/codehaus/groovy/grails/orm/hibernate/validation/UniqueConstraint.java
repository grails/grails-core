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

import org.springframework.validation.Errors;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.grails.metaclass.IdentDynamicMethod;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.FlushMode;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * Class description here.
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Jan 23, 2007
 *        Time: 2:00:55 PM
 */
/**
 *
 * A constraint that validates the uniqueness of a property
 */
public class UniqueConstraint extends AbstractPersistentConstraint {

    private static final String DEFAULT_NOT_UNIQUE_MESSAGE_CODE = "default.not.unique.message";


    public static final String UNIQUE_CONSTRAINT = "unique";
    

    private boolean unique;


    /**
     * @return Returns the unique.
     */
    public boolean isUnique() {
        return unique;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.validation.ConstrainedProperty.AbstractConstraint#setParameter(java.lang.Object)
     */
    public void setParameter(Object constraintParameter) {
        if(!(constraintParameter instanceof Boolean))
            throw new IllegalArgumentException("Parameter for constraint ["+UNIQUE_CONSTRAINT+"] of property ["+constraintPropertyName+"] of class ["+constraintOwningClass+"] must be a boolean value");

        this.unique = ((Boolean)constraintParameter).booleanValue();
        super.setParameter(constraintParameter);
    }

    public String getName() {
        return UNIQUE_CONSTRAINT;
    }

    protected void processValidate(Object target, final Object propertyValue, Errors errors) {

        if(unique) {
            final Object id = InvokerHelper.invokeMethod(target, IdentDynamicMethod.METHOD_SIGNATURE,null);
            List results = this.constraintHibernateTemplate.executeFind( new HibernateCallback() {

                public Object doInHibernate(Session session) throws HibernateException {
                    session.setFlushMode(FlushMode.MANUAL);

                    try {
                        return session.createCriteria( constraintOwningClass )
                                .add( Restrictions.eq( constraintPropertyName, propertyValue ) )
                                .list();
                    } finally {
                        session.setFlushMode(FlushMode.AUTO);
                    }

                }

            });

            if(results.size() > 0) {
                boolean reject = false;
                if(id != null) {
                    Object existing = results.get(0);
                    Object existingId = InvokerHelper.invokeMethod(existing, IdentDynamicMethod.METHOD_SIGNATURE,null);
                    if(!id.equals(existingId)) {
                        reject = true;
                    }
                }
                else {
                    reject = true;
                }
                if(reject) {
                    Object[] args = new Object[] { constraintPropertyName, constraintOwningClass, propertyValue };
                    super.rejectValue(errors,UNIQUE_CONSTRAINT,args,getDefaultMessage( DEFAULT_NOT_UNIQUE_MESSAGE_CODE, args ));
                }
            }
        }
    }

    public boolean supports(Class type) {
        return true;
    }

}
