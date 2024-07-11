package grails.artefact.controller.support

import jakarta.servlet.http.HttpServletRequest

import org.springframework.http.HttpMethod

import spock.lang.Specification

class AllowedMethodsHelperSpec extends Specification {
    
    void 'test isAllowed method'() {
        expect:
        expectedValue == AllowedMethodsHelper.isAllowed(actionName, [getMethod: {requestMethod}] as HttpServletRequest, allowedMethods)
        
        where:
        expectedValue | actionName | requestMethod | allowedMethods
        
        false         | 'alpha'    | 'DELETE'      | [alpha: 'POST']
        false         | 'alpha'    | 'DELETE'      | [alpha: ['POST', 'PUT']]
        false         | 'alpha'    | 'dElEtE'      | [alpha: 'pOsT']
        false         | 'alpha'    | 'DeLeTe'      | [alpha: ['pOsT', 'pUT']]
        
        true          | 'alpha'    | 'DELETE'      | [alpha: 'DELETE']
        true          | 'alpha'    | 'DELETE'      | [beta: 'POST']
        true          | 'alpha'    | 'DELETE'      | [alpha: ['POST', 'DELETE']]
        true          | 'alpha'    | 'DElEtE'      | [alpha: 'dElEtE']
        true          | 'alpha'    | 'DeLeTe'      | [alpha: ['pOsT', 'dElEtE']]
    }
}
