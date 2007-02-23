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
package org.codehaus.groovy.grails.domain;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.MissingMethodException;

import java.util.Set;
import java.util.SortedSet;

import junit.framework.TestCase;

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.metaclass.AbstractAddRelatedDynamicMethod;
import org.codehaus.groovy.grails.metaclass.AddRelatedDynamicMethod;

/**
 * Tests the relationship management method
 *
 * @author Graeme Rocher
 *         <p/>
 *         Date: Sep 19, 2006
 *         Time: 11:07:14 PM
 */
public class AddRelatedDynamicMethodTests extends TestCase {

    public void testInvoke() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader();
        gcl.parseClass("class Book {\n" +
                                     "\tLong id\n" +
                                     "\tLong version\n" +
                                     "\n" +
                                     "\tAuthor author\n" +
                                     "}\n" +
                                     "class Author {\n" +
                                     "\tLong id\n" +
                                     "\tLong version\n" +
                                     "\tdef relatesToMany = [books:Book]\n" +
                                     "\tSet books\n" +
                                     "}\n"+
                                     "class SubBook extends Book{\n" +
                                     "\tString subtitle\n" +
                                     "\n" +
                                     "}"
                                     );


         GroovyObject book = (GroovyObject)gcl.loadClass("Book",false,true).newInstance();
         GroovyObject subbook = (GroovyObject)gcl.loadClass("SubBook",false,true).newInstance();
         GroovyObject author = (GroovyObject)gcl.loadClass("Author",false,true).newInstance();

         GrailsApplication ga = new DefaultGrailsApplication(new Class[]{book.getClass(),author.getClass()},gcl);
         GrailsDomainClass authorDC = (GrailsDomainClass) ga.getArtefact(DomainClassArtefactHandler.TYPE, "Author");

         AbstractAddRelatedDynamicMethod ardm = new AddRelatedDynamicMethod(authorDC.getPropertyByName("books"));

         try {
             ardm.invoke(author,"addAuthor",new Object[0]);
             fail("Should have thrown missing method exception!");
         } catch (MissingMethodException e) {
             // expected
         }

         try {
             ardm.invoke(author,"addAuthor",new Object[]{"blah"});
             fail("Should have thrown missing method exception!");
         } catch (MissingMethodException e) {
             // expected
         }

         ardm.invoke(author,"addAuthor", new Object[]{book});
         ardm.invoke(author,"addAuthor", new Object[]{subbook});
         
         assertEquals(author,book.getProperty("author"));
         Set books = (Set)author.getProperty("books");
         assertNotNull(books);
         assertEquals(2,books.size());
         assertTrue(books.contains(book));
     }

     public void testInvokeWithSortedSet() throws Exception {
         GroovyClassLoader gcl = new GroovyClassLoader();
         gcl.parseClass("class Book implements Comparable {\n" +
                                      "\tLong id\n" +
                                      "\tLong version\n" +
                                      "\n" +
                                      "\tAuthor author\n" +
                                      "int compareTo(other) { 1 }" +
                                      "}\n" +
                                      "class Author {\n" +
                                      "\tLong id\n" +
                                      "\tLong version\n" +
                                      "\tdef relatesToMany = [books:Book]\n" +
                                      "\tSortedSet books\n" +
                                      "}");


          GroovyObject book = (GroovyObject)gcl.loadClass("Book",false,true).newInstance();
          GroovyObject author = (GroovyObject)gcl.loadClass("Author",false,true).newInstance();

          GrailsApplication ga = new DefaultGrailsApplication(new Class[]{book.getClass(),author.getClass()},gcl);
          GrailsDomainClass authorDC = (GrailsDomainClass) ga.getArtefact(DomainClassArtefactHandler.TYPE, "Author");

          AbstractAddRelatedDynamicMethod ardm = new AddRelatedDynamicMethod(authorDC.getPropertyByName("books"));

          try {
              ardm.invoke(author,"addAuthor",new Object[0]);
              fail("Should have thrown missing method exception!");
          } catch (MissingMethodException e) {
              // expected
          }

          try {
              ardm.invoke(author,"addAuthor",new Object[]{"blah"});
              fail("Should have thrown missing method exception!");
          } catch (MissingMethodException e) {
              // expected
          }

          ardm.invoke(author, "addAuthor",new Object[]{book});

          assertEquals(author,book.getProperty("author"));
          SortedSet books = (SortedSet)author.getProperty("books");
          assertNotNull(books);

     }

}
