package org.codehaus.groovy.grails.web.pages

import groovy.lang.MetaClass

import org.codehaus.groovy.grails.commons.GrailsMetaClassUtils
import org.springframework.web.context.request.RequestContextHolder as RCH

class GroovyPagesMetaUtils {
	public static void registerMethodMissingForGSP(Class gspClass, TagLibraryLookup gspTagLibraryLookup) {
		registerMethodMissingForGSP(GrailsMetaClassUtils.getExpandoMetaClass(gspClass), gspTagLibraryLookup)
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
		registerMethodMissingWorkaroundsForDefaultNamespace(mc, gspTagLibraryLookup)
	}
	
	public static registerMethodMissingWorkaroundsForDefaultNamespace(MetaClass mc, TagLibraryLookup gspTagLibraryLookup) {
		// hasErrors gets mixed up by hasErrors method without this metaclass modification
		registerMethodMissingForTags(mc, gspTagLibraryLookup, GroovyPage.DEFAULT_NAMESPACE, 'hasErrors', false)
	}
	
	// copied from /grails-plugin-controllers/src/main/groovy/org/codehaus/groovy/grails/web/plugins/support/WebMetaUtils.groovy
	private static registerMethodMissingForTags(MetaClass mc, TagLibraryLookup gspTagLibraryLookup, String namespace, String name, boolean addAll) {
		mc."$name" = {Map attrs, Closure body ->
			GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, attrs, body, RCH.currentRequestAttributes())
		}
		mc."$name" = {Map attrs, CharSequence body ->
			GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, attrs, new GroovyPage.ConstantClosure(body), RCH.currentRequestAttributes())
		}
		mc."$name" = {Map attrs ->
			GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, attrs, null, RCH.currentRequestAttributes())
		}
		if(addAll) {
			mc."$name" = {Closure body ->
				GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, [:], body, RCH.currentRequestAttributes())
			}
			mc."$name" = {->
				GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, [:], null, RCH.currentRequestAttributes())
			}
		}
	}
}
