import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.codehaus.groovy.grails.commons.ApplicationAttributes;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.ApplicationHolder;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.springframework.context.ApplicationContext;
import org.codehaus.groovy.grails.plugins.*
import org.springframework.core.io.*
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.web.MockServletContext
import org.codehaus.groovy.grails.cli.support.CommandLineResourceLoader;
import grails.spring.*

Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Package.groovy" )  

target ('default': "This target will load the Grails application context into the command window with a variable named 'ctx'") {
	bootstrap()
}

parentContext = null // default parent context is null
       
target(loadApp:"Loads the Grails application object") {
	event("AppLoadStart", ["Loading Grails Application"])
	profile("Loading parent ApplicationContext") {
		def builder = parentContext ? new WebBeanBuilder(parentContext) :  new WebBeanBuilder()
		beanDefinitions = builder.beans {
			resourceHolder(org.codehaus.groovy.grails.commons.spring.GrailsResourceHolder) {
				resources = "file:${basedir}/**/grails-app/**/*.groovy"
			}
			grailsResourceLoader(org.codehaus.groovy.grails.commons.GrailsResourceLoaderFactoryBean) {
				grailsResourceHolder = resourceHolder
			}
			grailsApplication(org.codehaus.groovy.grails.commons.DefaultGrailsApplication.class, ref("grailsResourceLoader"))
			pluginMetaManager(DefaultPluginMetaManager, resolveResources("file:${basedir}/plugins/*/plugin.xml"))
		}		
	}
                                                    
	appCtx = beanDefinitions.createApplicationContext()
	def ctx = appCtx
	ctx.servletContext = new MockServletContext()
	grailsApp = ctx.grailsApplication 
	ApplicationHolder.application = grailsApp
	
	packageApp()
	
	grailsApp.initialise()	 	
	event("AppLoadEnd", ["Loading Grails Application"])	
}                                      
target(configureApp:"Configures the Grails application and builds an ApplicationContext") {
    appCtx.resourceLoader = new  CommandLineResourceLoader()
	profile("Performing runtime Spring configuration") {
	    def config = new org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator(grailsApp,appCtx)
		appCtx = config.configure(new MockServletContext())			
	}
}                                                                                  

target(bootstrap: "The implementation target") {  
	depends(loadApp, configureApp)
}