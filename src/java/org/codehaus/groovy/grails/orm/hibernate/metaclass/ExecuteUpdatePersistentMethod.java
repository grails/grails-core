package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import groovy.lang.Closure;
import groovy.lang.MissingMethodException;
import org.codehaus.groovy.grails.orm.hibernate.exceptions.GrailsQueryException;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.SessionFactoryUtils;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Allows the executing of abituary HQL updates.
 * <p/>
 * eg. Account.executeUpdate("delete from Account a where a.branch = ?", 'London') or
 * Account.executeUpdate("delete from Account a where a.branch = :branch", [branch:'London'])
 *
 * @author Burt Beckwith
 */
public class ExecuteUpdatePersistentMethod extends AbstractStaticPersistentMethod {

	private static final String METHOD_SIGNATURE = "executeUpdate";
	private static final Pattern METHOD_PATTERN = Pattern.compile("^executeUpdate$");

	public ExecuteUpdatePersistentMethod(SessionFactory sessionFactory, ClassLoader classLoader) {
		super(sessionFactory, classLoader, METHOD_PATTERN);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Object doInvokeInternal(final Class clazz, final String methodName, Closure additionalCriteria, final Object[] arguments) {

		checkMethodSignature(clazz, arguments);

		return getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				Query q = session.createQuery(arguments[0].toString());
				SessionFactoryUtils.applyTransactionTimeout(q, getHibernateTemplate().getSessionFactory());

				// process positional HQL params
				int index = 0;
				for (Object parameter : extractPositionalParams(arguments)) {
					q.setParameter(index++, parameter);
				}

				// process named HQL params
				for (Map.Entry entry : (Set<Map.Entry>)extractNamedParams(arguments).entrySet()) {
					if (!(entry.getKey() instanceof String)) {
						throw new GrailsQueryException("Named parameter's name must be of type String");
					}

					String parameterName = (String)entry.getKey();
					Object parameterValue = entry.getValue();
					if (Collection.class.isAssignableFrom(parameterValue.getClass())) {
						q.setParameterList(parameterName, (Collection)parameterValue);
					}
					else if (parameterValue.getClass().isArray()) {
						q.setParameterList(parameterName, (Object[])parameterValue);
					}
					else if (parameterValue instanceof CharSequence) {
						q.setParameter(parameterName, parameterValue.toString());
					}
					else {
						q.setParameter(parameterName, parameterValue);
					}
				}
				return q.executeUpdate();
			}
		});
	}

	@SuppressWarnings("unchecked")
	private void checkMethodSignature(Class clazz, Object[] arguments) {
		boolean valid = true;
		if (arguments.length == 0 || arguments.length > 2) {
			valid = false;
		}
		else if (arguments.length == 2 && !(arguments[1] instanceof Map || arguments[1] instanceof Collection)) {
			valid = false;
		}

		if (!valid) {
			throw new MissingMethodException(METHOD_SIGNATURE, clazz, arguments);
		}
	}

	@SuppressWarnings("unchecked")
	private List extractPositionalParams(Object[] arguments) {
		if (arguments.length == 1 || arguments[1] instanceof Map) {
			return Collections.EMPTY_LIST;
		}

		List result = new ArrayList();
		if (arguments[1] instanceof Collection) {
			result.addAll((Collection)arguments[1]);
		}
		else if (arguments[1].getClass().isArray()) {
			result.addAll(Arrays.asList((Object[])arguments[1]));
		}
		else {
			result.add(arguments[1]);
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	private Map extractNamedParams(Object[] arguments) {
		if (arguments.length == 1 || !(arguments[1] instanceof Map)) {
			return Collections.EMPTY_MAP;
		}

		return (Map)arguments[1];
	}
}
