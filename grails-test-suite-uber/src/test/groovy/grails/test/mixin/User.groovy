package grails.test.mixin

import grails.gorm.annotation.Entity

@Entity
class User {
    String username
    String password
}
