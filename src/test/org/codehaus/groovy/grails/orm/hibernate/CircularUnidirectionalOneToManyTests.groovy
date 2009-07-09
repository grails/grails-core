package org.codehaus.groovy.grails.orm.hibernate;


class CircularUnidirectionalOneToManyTests extends AbstractGrailsHibernateTests {

	void testCircularDomain() {
		def taskDomain = ga.getDomainClass("Task")

		
		def tasks = taskDomain?.getPropertyByName("tasks")

		assert tasks		
		assert tasks.isOneToMany()
		assert !tasks.isBidirectional()
		
	}
	
	void testOneToMany() {
		def taskClass = ga.getDomainClass("Task")
		
		def taskParent = taskClass.newInstance()
		def taskChild = taskClass.newInstance()
		
		taskParent.addToTasks(taskChild)
		taskParent.save(true)
		session.flush()
		
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
'''
		)
	}	
	
	void onTearDown() {
		
	}

}
