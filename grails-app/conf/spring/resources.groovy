import au.org.ala.doi.util.DoiProviderMapping
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.config.BeanDefinition

// Place your Spring DSL code here
beans = {

    if (application.config.flyway.enabled) {

        flyway(Flyway) { bean ->
            bean.initMethod = 'migrate'
            dataSource = ref('dataSource')
            baselineOnMigrate = application.config.flyway.baselineOnMigrate ?: false
            locations = application.config.flyway.locations ?: 'classpath:db/migration'
            if (application.config.flyway.baselineVersion) baselineVersionAsString = application.config.flyway.baselineVersion.toString()
        }

        BeanDefinition sessionFactoryBeanDef = getBeanDefinition('sessionFactory')

        if (sessionFactoryBeanDef) {
            addDependency(sessionFactoryBeanDef, 'flyway')
        }

        BeanDefinition hibernateDatastoreBeanDef = getBeanDefinition('hibernateDatastore')
        if (hibernateDatastoreBeanDef) {
            addDependency(hibernateDatastoreBeanDef, 'flyway')
        }

    }
    else {
        log.info "Grails Flyway plugin has been disabled"
    }

    // the configuration of the DOI mapping was not working Spring @Component injecting via @Value
    doiProviderMapping(DoiProviderMapping) {
        doiProviderMapping = application.config?.doi?.service?.provider?.mapping
    }
}
def addDependency(BeanDefinition beanDef, String dependencyName) {
    def dependsOnList = [ dependencyName ] as Set
    if (beanDef.dependsOn?.length > 0) {
        dependsOnList.addAll(beanDef.dependsOn)
    }
    beanDef.dependsOn = dependsOnList as String[]
}