package org.codehaus.groovy.grails.plugins;

import java.util.Set;

import junit.framework.TestCase;

public class PluginFilterFactoryTests extends TestCase {

	public void testIncludeFilterOne() throws Exception
	{
		PluginFilterRetriever fb = new PluginFilterRetriever();
		PluginFilter bean = fb.getPluginFilter("one", null);
		assertTrue(bean instanceof IncludingPluginFilter);
		
		IncludingPluginFilter filter = (IncludingPluginFilter) bean;
		Set suppliedNames = filter.getSuppliedNames();
		assertEquals(1, suppliedNames.size());
		assertTrue(suppliedNames.contains("one"));
	}	
	
	public void testIncludeFilter() throws Exception
	{	
		PluginFilterRetriever fb = new PluginFilterRetriever();
		PluginFilter bean = fb.getPluginFilter("one, two", " three , four ");
		assertTrue(bean instanceof IncludingPluginFilter);
		
		IncludingPluginFilter filter = (IncludingPluginFilter) bean;
		Set suppliedNames = filter.getSuppliedNames();
		assertEquals(2, suppliedNames.size());
		assertTrue(suppliedNames.contains("two"));
	}
	
	public void testExcludeFilter() throws Exception
	{
		PluginFilterRetriever fb = new PluginFilterRetriever();
		PluginFilter bean = fb.getPluginFilter(null, " three , four ");
		assertTrue(bean instanceof ExcludingPluginFilter);
		
		ExcludingPluginFilter filter = (ExcludingPluginFilter) bean;
		Set suppliedNames = filter.getSuppliedNames();
		assertEquals(2, suppliedNames.size());
		assertTrue(suppliedNames.contains("four"));
	}
	
	public void testDefaultFilter() throws Exception
	{
		PluginFilterRetriever fb = new PluginFilterRetriever();
		PluginFilter bean = fb.getPluginFilter(null, null);
		assertTrue(bean instanceof IdentityPluginFilter);
	}
}
