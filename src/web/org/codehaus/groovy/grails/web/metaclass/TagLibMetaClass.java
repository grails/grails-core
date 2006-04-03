package org.codehaus.groovy.grails.web.metaclass;

import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import groovy.lang.MetaClassRegistry;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;

import java.beans.IntrospectionException;

import javax.servlet.ServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.metaclass.PropertyAccessProxyMetaClass;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.runtime.InvokerHelper;

public class TagLibMetaClass extends PropertyAccessProxyMetaClass {
	private static final Log LOG = LogFactory.getLog(TagLibMetaClass.class);
	
	public TagLibMetaClass(MetaClassRegistry registry, Class theClass, MetaClass adaptee) throws IntrospectionException {
		super(registry, theClass, adaptee);
	}

	public static TagLibMetaClass getTagLibInstance(Class theClass) throws IntrospectionException {
        MetaClassRegistry metaRegistry = InvokerHelper.getInstance().getMetaRegistry();
        MetaClass meta = metaRegistry.getMetaClass(theClass);
        return new TagLibMetaClass(metaRegistry, theClass, meta);
    }
	
	
	/* (non-Javadoc)
	 * @see groovy.lang.ProxyMetaClass#invokeMethod(java.lang.Object, java.lang.String, java.lang.Object[])
	 */
	public Object invokeMethod(Object object, String methodName, Object[] arguments) {
		try {
			return super.invokeMethod(object, methodName, arguments);
		}
		catch(MissingMethodException mme) {
			GroovyObject taglib = (GroovyObject)object;
			GrailsApplicationAttributes applicationAttributes = (GrailsApplicationAttributes)taglib.getProperty(ControllerDynamicMethods.GRAILS_ATTRIBUTES);
			ServletRequest request = (ServletRequest)taglib.getProperty(ControllerDynamicMethods.REQUEST_PROPERTY);
			GroovyObject tagLibrary = applicationAttributes.getTagLibraryForTag(request,methodName);
			if(tagLibrary == null) throw mme;
			if(tagLibrary.getClass().equals(object.getClass())) throw mme;
			
			if(LOG.isDebugEnabled())
				LOG.debug("Tag ["+methodName+"] not found in existing library, found in ["+tagLibrary.getClass().getName()+"]. Invoking..");
			tagLibrary.setProperty(TagLibDynamicMethods.OUT_PROPERTY,taglib.getProperty(TagLibDynamicMethods.OUT_PROPERTY));
			return tagLibrary.invokeMethod(methodName,arguments);
		}
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.commons.metaclass.PropertyAccessProxyMetaClass#getProperty(java.lang.Object, java.lang.String)
	 */
	public Object getProperty(Object object, String property) {
		try {
			return super.getProperty(object, property);
		}
		catch(MissingPropertyException mpe) {
			GroovyObject taglib = (GroovyObject)object;
			GrailsApplicationAttributes applicationAttributes = (GrailsApplicationAttributes)taglib.getProperty(ControllerDynamicMethods.GRAILS_ATTRIBUTES);
			ServletRequest request = (ServletRequest)taglib.getProperty(ControllerDynamicMethods.REQUEST_PROPERTY);
			GroovyObject tagLibrary = applicationAttributes.getTagLibraryForTag(request,property);
			if(tagLibrary == null) throw mpe;
			
			if(LOG.isDebugEnabled())
				LOG.debug("Tag ["+property+"] not found in existing library, found in ["+tagLibrary.getClass().getName()+"]. Retrieving");
			return tagLibrary.getProperty(property);
		}
	}	
	
	
}
