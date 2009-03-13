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
package org.codehaus.groovy.grails.commons.spring;

import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.UiApplicationContextUtils;

/**
 * An ApplicationContext that extends StaticApplicationContext and implements GroovyObject such that beans can be retrieved with the dot
 * de-reference syntax instead of using getBean('name')
 *
 * @author Graeme Rocher
 * @since 1.0
 *        <p/>
 *        Created: Nov 23, 2007
 */
public class GrailsApplicationContext extends StaticApplicationContext implements GroovyObject {
    protected MetaClass metaClass;
    private BeanWrapper ctxBean = new BeanWrapperImpl(this);
    private ThemeSource themeSource;

    public GrailsApplicationContext(org.springframework.context.ApplicationContext parent) throws org.springframework.beans.BeansException {
        super(parent);
        this.metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(getClass());
    }

    public GrailsApplicationContext() throws org.springframework.beans.BeansException {
        super();
        this.metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(getClass());
    }

    public MetaClass getMetaClass() {
		return this.metaClass;
	}

    public Object getProperty(String property) {
		if(containsBean(property)) {
			return getBean(property);
		}
		else if(ctxBean.isReadableProperty(property)) {
			return ctxBean.getPropertyValue(property);
		}
		return null;
	}

    public Object invokeMethod(String name, Object args) {
		return metaClass.invokeMethod(this, name, args);
	}

    public void setMetaClass(MetaClass metaClass) {
		this.metaClass = metaClass;
	}

    /**
	 * Initialize the theme capability.
     */
    protected void onRefresh() {
        this.themeSource = UiApplicationContextUtils.initThemeSource(this);
    }

    public Theme getTheme(String themeName) {
		return this.themeSource.getTheme(themeName);
	}

    public void setProperty(String property, Object newValue) {
		if(newValue instanceof BeanDefinition) {
            if(containsBean(property)) {
                removeBeanDefinition(property);
            }

            registerBeanDefinition(property, (BeanDefinition)newValue);
		}
		else {
			metaClass.setProperty(this, property, newValue);
		}
	}
}
