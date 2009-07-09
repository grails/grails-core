package org.codehaus.groovy.grails.orm.hibernate;


class CircularOneToManyTests extends AbstractGrailsHibernateTests  {

	void testCircularDomain() {
		def taskDomain = ga.getDomainClass("Task")

		
		def tasks = taskDomain?.getPropertyByName("tasks")
		def task = taskDomain?.getPropertyByName("task")
		assert tasks
		assert task
		
		assert tasks.isOneToMany()
		assert tasks.isBidirectional()
		assert task.isManyToOne()
		assert task.isBidirectional()
		
	}

	void onSetUp() {
		this.gcl.parseClass('''
class Task {
	Long id
	Long version
	Set tasks
	Task task
	def belongsTo = Task 
	def hasMany = [tasks:Task]
}
'''
		)
	}	
	
	void onTearDown() {
		
	}
}
