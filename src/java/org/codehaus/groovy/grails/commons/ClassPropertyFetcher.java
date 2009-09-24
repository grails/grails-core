/**
 * 
 */
package org.codehaus.groovy.grails.commons;

import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.ReflectionUtils.MethodCallback;

/**
 * 
 * Accesses class "properties": static fields, static getters, instance fields
 * or instance getters
 * 
 * Method and Field instances are cached for fast access
 * 
 * 
 * 
 * @author Lari Hotari, Sagire Software Oy
 * 
 */
public class ClassPropertyFetcher {
	private final Class clazz;
	final Map<String, PropertyFetcher> staticFetchers = new HashMap<String, PropertyFetcher>();
	final Map<String, PropertyFetcher> instanceFetchers = new HashMap<String, PropertyFetcher>();
	private final ReferenceInstanceCallback callback;

	ClassPropertyFetcher(Class clazz, ReferenceInstanceCallback callback) {
		this.clazz = clazz;
		this.callback = callback;
		init();
	}

	public boolean isReadableProperty(String name) {
		return staticFetchers.containsKey(name)
				|| instanceFetchers.containsKey(name);
	}

	private void init() {
		FieldCallback fieldCallback = new ReflectionUtils.FieldCallback() {
			public void doWith(Field field) {
				if (field.getName().indexOf('$') == -1) {
					boolean staticField = Modifier.isStatic(field
							.getModifiers());
					if (staticField) {
						staticFetchers.put(field.getName(),
								new FieldReaderFetcher(field, staticField));
					} else {
						instanceFetchers.put(field.getName(),
								new FieldReaderFetcher(field, staticField));
					}
				}
			}
		};

		MethodCallback methodCallback = new ReflectionUtils.MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException,
					IllegalAccessException {
				if (Modifier.isStatic(method.getModifiers())
						&& method.getReturnType() != Void.class) {
					if (method.getParameterTypes().length == 0) {
						String name = method.getName();
						if (name.indexOf('$') == -1) {
							if (name.length() > 3 && name.startsWith("get") && Character.isUpperCase(name.charAt(3))) {
								name = name.substring(3);
							} else if (name.length() > 2 && name.startsWith("is") && Character.isUpperCase(name.charAt(2)) && (method.getReturnType()==Boolean.class || method.getReturnType()==boolean.class)) {
								name = name.substring(2);
							}
							PropertyFetcher fetcher = new GetterPropertyFetcher(
									method, true);
							staticFetchers.put(name, fetcher);
							staticFetchers.put(StringUtils.uncapitalize(name),
									fetcher);
						}
					}
				}
			}
		};

		List<Class> allClasses = resolveAllClasses(clazz);
		for (Class c : allClasses) {
			Field[] fields=c.getDeclaredFields();
			for(Field field : fields) {
				try {
					fieldCallback.doWith(field);
				} catch (IllegalAccessException ex) {
					throw new IllegalStateException("Shouldn't be illegal to access field '" + field.getName() + "': "
							+ ex);
				}
			}
			Method[] methods=c.getDeclaredMethods();
			for(Method method : methods) {
				try {
					methodCallback.doWith(method);
				} catch (IllegalAccessException ex) {
					throw new IllegalStateException("Shouldn't be illegal to access method '" + method.getName()
							+ "': " + ex);
				}
			}
		}

		PropertyDescriptor[] descriptors = BeanUtils
				.getPropertyDescriptors(clazz);
		for (PropertyDescriptor desc : descriptors) {
			Method readMethod = desc.getReadMethod();
			if (readMethod != null) {
				boolean staticReadMethod = Modifier.isStatic(readMethod
						.getModifiers());
				if (staticReadMethod) {
					staticFetchers.put(desc.getName(),
							new GetterPropertyFetcher(readMethod,
									staticReadMethod));
				} else {
					instanceFetchers.put(desc.getName(),
							new GetterPropertyFetcher(readMethod,
									staticReadMethod));
				}
			}
		}
	}

	private List<Class> resolveAllClasses(Class c) {
		List<Class> list = new ArrayList<Class>();
		Class currentClass = c;
		while (currentClass != null) {
			list.add(currentClass);
			currentClass = currentClass.getSuperclass();
		}
		Collections.reverse(list);
		return list;
	}

	public Object getPropertyValue(String name) {
		return getPropertyValue(name, false);
	}

	public Object getPropertyValue(String name, boolean onlyInstanceProperties) {
		PropertyFetcher fetcher = resolveFetcher(name, onlyInstanceProperties);
		if (fetcher != null) {
			try {
				return fetcher.get(callback);
			} catch (Exception e) {
				AbstractGrailsClass.LOG.warn("Error fetching property's "
						+ name + " value from class " + clazz.getName(), e);
			}
		}
		return null;
	}

	private PropertyFetcher resolveFetcher(String name,
			boolean onlyInstanceProperties) {
		PropertyFetcher fetcher = null;
		if (!onlyInstanceProperties) {
			fetcher = staticFetchers.get(name);
		}
		if (fetcher == null) {
			fetcher = instanceFetchers.get(name);
		}
		return fetcher;
	}

	public Class getPropertyType(String name) {
		return getPropertyType(name, false);
	}
	
	public Class getPropertyType(String name, boolean onlyInstanceProperties) {
		PropertyFetcher fetcher = resolveFetcher(name, onlyInstanceProperties);
		if (fetcher != null) {
			return fetcher.getPropertyType(name);
		}
		return null;
	}

	public static interface ReferenceInstanceCallback {
		public Object getReferenceInstance();
	}

	static interface PropertyFetcher {
		public Object get(ReferenceInstanceCallback callback)
				throws IllegalArgumentException, IllegalAccessException,
				InvocationTargetException;

		public Class getPropertyType(String name);
	}

	static class GetterPropertyFetcher implements PropertyFetcher {
		private final Method readMethod;
		private final boolean staticMethod;

		GetterPropertyFetcher(Method readMethod, boolean staticMethod) {
			this.readMethod = readMethod;
			this.staticMethod = staticMethod;
			ReflectionUtils.makeAccessible(readMethod);
		}

		public Object get(ReferenceInstanceCallback callback)
				throws IllegalArgumentException, IllegalAccessException,
				InvocationTargetException {
			if (staticMethod) {
				return readMethod.invoke(null, (Object[]) null);
			} else {
				return readMethod.invoke(callback.getReferenceInstance(),
						(Object[]) null);
			}
		}

		public Class getPropertyType(String name) {
			return readMethod.getReturnType();
		}
	}

	static class FieldReaderFetcher implements PropertyFetcher {
		private final Field field;
		private final boolean staticField;

		public FieldReaderFetcher(Field field, boolean staticField) {
			this.field = field;
			this.staticField = staticField;
			ReflectionUtils.makeAccessible(field);
		}

		public Object get(ReferenceInstanceCallback callback)
				throws IllegalArgumentException, IllegalAccessException,
				InvocationTargetException {
			if (staticField) {
				return field.get(null);
			} else {
				return field.get(callback.getReferenceInstance());
			}
		}

		public Class getPropertyType(String name) {
			return field.getType();
		}
	}
}



