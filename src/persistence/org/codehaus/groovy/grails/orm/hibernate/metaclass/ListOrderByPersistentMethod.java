/**
 * 
 */
package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The "listOrderBy*" static persistent method. This method allows
 * ordered listing of instances based on their properties.
 * 
 * eg.
 * Account.listOrderByHolder();
 * Account.listOrderByHolder(max); // max results
 * 
 * @author Graeme
 *
 */
public class ListOrderByPersistentMethod extends AbstractStaticPersistentMethod {

	private static final String METHOD_PATTERN = "(listOrderBy)(\\w+)";

	public ListOrderByPersistentMethod(SessionFactory sessionFactory, ClassLoader classLoader) {
		super(sessionFactory, classLoader, Pattern.compile( METHOD_PATTERN ));
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.orm.hibernate.metaclass.AbstractStaticPersistentMethod#doInvokeInternal(java.lang.Class, java.lang.String, java.lang.Object[])
	 */
	protected Object doInvokeInternal(final Class clazz, String methodName,
			final Object[] arguments) {

		Matcher match = getPattern().matcher( methodName );
		// find match
		match.find();
		
		String nameInSignature = match.group(2);
		final String propertyName = nameInSignature.substring(0,1).toLowerCase(Locale.ENGLISH) +
										nameInSignature.substring(1);
				
		return super.getHibernateTemplate().executeFind( new HibernateCallback() {

			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				
				Criteria crit = session.createCriteria(clazz);
				
				if(arguments != null && arguments.length > 0) {
					if(arguments[0] instanceof Map) {
						Map argMap = (Map)arguments[0];
						argMap.put(GrailsHibernateUtil.ARGUMENT_SORT,propertyName);
						GrailsHibernateUtil.populateArgumentsForCriteria(crit,argMap);
					}
					else {
						crit.addOrder( Order.asc( propertyName ) );												
					}
				}
				else {
					crit.addOrder( Order.asc( propertyName ) );
				}
				return crit.list();
			}
			
		});
	}

}
