/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.support;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.util.Assert;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;

import java.util.List;
import java.util.ArrayList;

/**
 * IoC class to inject properties of Grails test case classes.
 * 
 * 
 * @author Steven Devijver
 * @since Aug 28, 2005
 */
public class GrailsTestSuite extends TestSuite {

    private static final String CLEANING_ORDER_PROPERTY = "cleaningOrder";

	private AutowireCapableBeanFactory beanFactory = null;
    private List cleaningOrder;

    public GrailsTestSuite(AutowireCapableBeanFactory beanFactory, Class clazz) {
		super(clazz);
        Assert.notNull(beanFactory, "Bean factory should not be null!");
		this.beanFactory = beanFactory;
        Object order = GrailsClassUtils.getStaticPropertyValue( clazz, CLEANING_ORDER_PROPERTY );
        if( order != null && List.class.isAssignableFrom( order.getClass())) {
            cleaningOrder = (List) order;
        } 
	}

	public void runTest(Test test, TestResult result) {
		if (test instanceof TestCase) {
			beanFactory.autowireBeanProperties(test, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		}
		test.run(result);
	}

    public List getCleaningOrder() {
        return (cleaningOrder != null) ? cleaningOrder : new ArrayList();
    }
}
