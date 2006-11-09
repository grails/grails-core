import grails.pageflow.*;

class TestPageFlow {
    Flow flow = new PageFlowBuilder().flow {
        firstStage(view:"someView")
    }
}