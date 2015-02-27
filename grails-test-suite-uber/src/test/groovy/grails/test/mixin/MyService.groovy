package grails.test.mixin

class MyService {
    int identity() {
        System.identityHashCode(this)
    }
}
