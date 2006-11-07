package org.codehaus.groovy.grails.orm.hibernate;


class CircularUnidirectionalOneToManyTests extends AbstractGrailsHibernateTests {

	void testCircularDomain() {
		def taskDomain = ga.getGrailsDomainClass("Task")

		
		def tasks = taskDomain?.getPropertyByName("tasks")

		assert tasks		
		assert tasks.isOneToMany()
		assert !tasks.isBidirectional()
		
	}
	
	void testOneToMany() {
		def taskClass = ga.getGrailsDomainClass("Task")
		
		def taskParent = taskClass.newInstance()
		def taskChild = taskClass.newInstance()
		
		taskParent.addTask(taskChild)
		taskParent.save(true)
		
		session.evict(taskParent)
		session.evict(taskChild)
		
		taskParent = taskClass.clazz.get(1)
		
		assert taskParent
		assert taskParent.tasks
	}

	void onSetUp() {
		this.gcl.parseClass('''
class Task {
	Long id
	Long version
	Set tasks
	def hasMany = [tasks:Task]
}
class ApplicationDataSource {
	   boolean pooling = true
	   boolean logSql = true
	   String dbCreate = "create-drop" // one of 'create', 'create-drop','update'
	   String url = "jdbc:hsqldb:mem:testDB"
	   String driverClassName = "org.hsqldb.jdbcDriver"
	   String username = "sa"
	   String password = ""  
}
'''
		)
	}	
	
	void onTearDown() {
		
	}

}
