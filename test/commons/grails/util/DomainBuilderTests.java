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
package grails.util;

import groovy.util.GroovyTestCase;
import groovy.util.ObjectGraphBuilder;
import org.codehaus.groovy.grails.commons.ApplicationHolder;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;

import java.util.List;

public class DomainBuilderTests extends GroovyTestCase
{
	private DomainBuilder builder;
	private ObjectGraphBuilder.ChildPropertySetter childPropertySetter;

	private Employer employer = null;

	public DomainBuilderTests()
	{
		super();
	}

	public void setUp() throws Exception
	{
		ApplicationHolder.setApplication(new DefaultGrailsApplication());

		builder = new DomainBuilder();
		childPropertySetter = builder.getChildPropertySetter();

		employer = new Employer();
		employer.setName("Spacely Space Sprockets");
	}

	public void tearDown() throws Exception
	{
		builder = null;
	}

	public void testChildIsCollection() throws Exception
	{
		Employee one = new Employee();
		one.setName("Cosmo");

		Employee two = new Employee();
		two.setName("George");

		childPropertySetter.setChild(employer, one, null, "employees");
		childPropertySetter.setChild(employer, two, null, "employees");

		List employees = employer.getEmployees();

		assertNull(employer.getAddress());
		assertEquals(2, employees.size());

		assertEquals(one.getName(), ((Employee)employees.get(0)).getName());
		assertEquals(two.getName(), ((Employee)employees.get(1)).getName());
	}

	public void testChildIsNotCollection() throws Exception
	{
		Address address = new Address();
		address.setStreet("Park Pl.");

		childPropertySetter.setChild(employer, address, null, "address");

		Address a = employer.getAddress();

		assertEquals(address.getStreet(), a.getStreet());

		assertEquals(0, employer.getEmployees().size());
	}


	public static class Employer
	{
		private String name = null;
		private Address address = null;
		private List employees = new java.util.ArrayList();

		public void addToEmployees(Employee employee) {
			employees.add(employee);
		}
		public List getEmployees() {
			return employees;
		}
		public void setAddress(Address a) {
			address = a;
		}
		public Address getAddress() {
			return address;
		}
		public void setName(String n) {
			name = n;
		}
		public String getName() {
			return name;
		}
	}

	public static class Employee
	{
		private String name = null;

		public void setName(String n) {
			name = n;
		}
		public String getName() {
			return name;
		}
	}

	public static class Address
	{
		private String street = null;

		public void setStreet(String s) {
			street = s;
		}
		public String getStreet() {
			return street;
		}
	}

}


