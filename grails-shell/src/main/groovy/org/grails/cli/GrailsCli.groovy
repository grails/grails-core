package org.grails.cli

class GrailsCli {

    public static void main(String[] args) {
        if(!args) {
            println "usage: create-app appname --profile=web"
            System.exit(1)
        }
        if(args[0] == 'create-app') {
            def appname = args[1]
            def profile=null
            if(args.size() > 2) {
                def matches = (args[2] =~ /^--profile=(.*?)$/)
                if (matches) {
                    profile=matches.group(1)
                }
            }            
            println "app: $appname profile: $profile"
            CreateAppCommand cmd = new CreateAppCommand(appname: appname, profile: profile)
            cmd.run()
        }
    }
}
