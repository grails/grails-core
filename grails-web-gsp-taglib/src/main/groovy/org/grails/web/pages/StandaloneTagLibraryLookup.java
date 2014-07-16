package org.grails.web.pages;

import grails.core.GrailsTagLibClass;
import grails.gsp.TagLib;
import org.grails.core.DefaultGrailsTagLibClass;
import org.grails.plugins.web.taglib.RenderTagLib;
import org.grails.plugins.web.taglib.SitemeshTagLib;
import org.grails.web.taglib.TagLibraryLookup;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.*;

/**
 * GSP TagLibraryLookup class that's used for standalone GSP 
 * 
 * @author Lari Hotari
 * @since 2.4.0
 */
public class StandaloneTagLibraryLookup extends TagLibraryLookup implements ApplicationListener<ContextRefreshedEvent> {
    public static final Class<?>[] DEFAULT_TAGLIB_CLASSES=new Class<?>[] { SitemeshTagLib.class, RenderTagLib.class };
    Set<Object> tagLibInstancesSet; 
    
    private StandaloneTagLibraryLookup() {
        
    }

    public void afterPropertiesSet() {
        registerTagLibraries();
        registerTemplateNamespace();
    }

    protected void registerTagLibraries() {
        if(tagLibInstancesSet != null) {
            for(Object tagLibInstance : tagLibInstancesSet) {
                registerTagLib(new DefaultGrailsTagLibClass(tagLibInstance.getClass()));
            }
        }
    }

    @Override
    protected void putTagLib(Map<String, Object> tags, String name, GrailsTagLibClass taglib) {
        for(Object tagLibInstance : tagLibInstancesSet) {
            if(tagLibInstance.getClass() == taglib.getClazz()) {
                tags.put(name, tagLibInstance);
                break;
            }
        }
    }

    public void setTagLibInstances(List<Object> tagLibInstances) {
        this.tagLibInstancesSet = new LinkedHashSet<Object>();
        tagLibInstancesSet.addAll(tagLibInstances);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        detectAndRegisterTabLibBeans();
    }

    public void detectAndRegisterTabLibBeans() {
        if(tagLibInstancesSet==null) {
            tagLibInstancesSet = new LinkedHashSet<Object>();
        }
        Collection<Object> detectedInstances = ((ListableBeanFactory)applicationContext).getBeansWithAnnotation(TagLib.class).values();
        for(Object instance : detectedInstances) {
            if(!tagLibInstancesSet.contains(instance)) {
                tagLibInstancesSet.add(instance);
                registerTagLib(new DefaultGrailsTagLibClass(instance.getClass()));
            }
        }
    }
}