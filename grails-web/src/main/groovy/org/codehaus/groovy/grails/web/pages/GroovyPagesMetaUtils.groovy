package org.codehaus.groovy.grails.web.pages

import groovy.lang.MetaClass

import org.codehaus.groovy.runtime.InvokerHelper

class GroovyPagesMetaUtils {
	public static void registerMethodMissingForGSP(Class gspClass, TagLibraryLookup gspTagLibraryLookup) {
		registerMethodMissingForGSP(InvokerHelper.getMetaClass(gspClass), gspTagLibraryLookup)
	}
	
	public static void registerMethodMissingForGSP(MetaClass mc, TagLibraryLookup gspTagLibraryLookup) {
		mc.methodMissing = { String name, args ->
			GroovyObject tagBean = gspTagLibraryLookup.lookupTagLibrary(GroovyPage.DEFAULT_NAMESPACE, name)
			if(tagBean && tagBean.respondsTo(name, args)) {
				MetaMethod method=tagBean.metaClass.getMetaMethod(name, args)
				synchronized(mc) {
					mc."$name" = { Object[] varArgs ->
						method.invoke(tagBean, varArgs ? varArgs[0] : varArgs)
				   }
				}
				return method.invoke(tagBean, args)
			} else {
				throw new MissingMethodException(name, mc.getTheClass(), args)
			}
		}
	}
}
