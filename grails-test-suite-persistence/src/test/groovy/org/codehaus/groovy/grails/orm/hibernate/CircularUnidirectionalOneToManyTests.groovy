package org.codehaus.groovy.grails.orm.hibernate

class CircularUnidirectionalOneToManyTests extends AbstractGrailsHibernateTests {

    void testCircularDomain() {
        def taskDomain = ga.getDomainClass("Task")
        def tasks = taskDomain?.getPropertyByName("tasks")

        assertNotNull tasks
        assertTrue tasks.isOneToMany()
        assertFalse tasks.isBidirectional()
    }

    void testOneToMany() {
        def taskClass = ga.getDomainClass("Task")

        def taskParent = taskClass.newInstance()
        def taskChild = taskClass.newInstance()

        taskParent.addToTasks(taskChild)
        taskParent.save()
        session.flush()

        session.evict(taskParent)
        session.evict(taskChild)

        taskParent = taskClass.clazz.get(1)

        assertNotNull taskParent
        assertNotNull taskParent.tasks
    }

    void onSetUp() {
        gcl.parseClass '''
class Task {
    Long id
    Long version
    Set tasks
    static hasMany = [tasks:Task]
}
'''
    }
}

