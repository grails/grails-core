package org.codehaus.groovy.grails.orm.hibernate

class CircularOneToManyTests extends AbstractGrailsHibernateTests  {

    void testCircularDomain() {
        def taskDomain = ga.getDomainClass("Task")
        def tasks = taskDomain?.getPropertyByName("tasks")
        def task = taskDomain?.getPropertyByName("task")
        assertNotNull tasks
        assertNotNull task

        assertTrue tasks.isOneToMany()
        assertTrue tasks.isBidirectional()
        assertTrue task.isManyToOne()
        assertTrue task.isBidirectional()
    }

    void onSetUp() {
        gcl.parseClass '''
class Task {
    Long id
    Long version
    Set tasks
    Task task
    static belongsTo = Task
    static hasMany = [tasks:Task]
}
'''
    }
}
