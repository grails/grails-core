package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jun 9, 2009
 */

public class BidirectionalOneToOneWithProxyTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Face {
  Integer height
  Integer width
  Nose nose
  static constraints = {
  }
}

@Entity
class Nose {
  Integer length
  static belongsTo = [face:Face]
  static constraints = {
  }
}

''')
    }


    // test for GRAILS-4580
    void testBidirectionalOneToOneWithProxy() {
        def Nose = ga.getDomainClass("Nose").clazz
        def Face = ga.getDomainClass("Face").clazz
        def nose = Nose.newInstance(length:2)
        def face = Face.newInstance(width:10, height:8, nose:nose)
        assertNotNull "should have saved face",face.save(flush:true)

        assertEquals 1, Nose.count()
        session.clear()



        def faces = Face.list()
        print faces
        face = faces[0]
        nose = face.nose
        nose.length = 3
       
        assertNotNull "saving nose should have been successful",nose.save()
    }

}