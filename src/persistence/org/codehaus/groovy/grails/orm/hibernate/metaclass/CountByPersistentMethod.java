/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Dynamic method that allows counting the values of the specified property names
 *
 * eg. Account.countByBranch('London') // returns how many accounts are in london
 *
 *
 * @author Graeme Rocher
 * @since 04-May-2006
 */
public class CountByPersistentMethod extends
		AbstractClausedStaticPersistentMethod {

	private static final String OPERATOR_OR = "Or";
	private static final String OPERATOR_AND = "And";

	private static final Pattern METHOD_PATTERN = Pattern.compile("(countBy)(\\w+)");
	private static final String[] OPERATORS = new String[]{ OPERATOR_AND, OPERATOR_OR };

	public CountByPersistentMethod(GrailsApplication application, SessionFactory sessionFactory, ClassLoader classLoader) {
		super(application, sessionFactory, classLoader, METHOD_PATTERN, OPERATORS);
	}

	protected Object doInvokeInternalWithExpressions(final Class clazz,
                                                     String methodName, Object[] arguments, final List expressions, String operatorInUse) {
        final String operator = OPERATOR_OR.equals(operatorInUse) ? OPERATOR_OR : OPERATOR_AND;
        return super.getHibernateTemplate().execute( new HibernateCallback() {

			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				Criteria crit = session.createCriteria(clazz);
				crit.setProjection(Projections.rowCount());
                populateCriteriaWithExpressions(crit, operator, expressions);

                return crit.uniqueResult();
			}
		});
	}

    
    protected void populateCriteriaWithExpressions(Criteria crit, String operator, List expressions) {
        if(operator.equals(OPERATOR_OR)) {
            Disjunction dis = Restrictions.disjunction();
            for (Iterator i = expressions.iterator(); i.hasNext();) {
                GrailsMethodExpression current = (GrailsMethodExpression) i.next();
                dis.add( current.getCriterion() );
            }
            crit.add(dis);
        }
        else {
            for (Iterator i = expressions.iterator(); i.hasNext();) {
                GrailsMethodExpression current = (GrailsMethodExpression) i.next();
                crit.add( current.getCriterion() );

            }
        }
    }

}
