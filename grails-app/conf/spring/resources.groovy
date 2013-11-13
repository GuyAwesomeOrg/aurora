import com.google.common.base.CaseFormat
import com.paypal.aurora.ServiceInitLoggingBeanPostProcessor
import com.paypal.aurora.model.SessionStorage
import groovy.io.FileType
import groovy.json.JsonSlurper
import org.codehaus.jackson.map.DeserializationConfig
import org.codehaus.jackson.map.ObjectMapper
import org.springframework.aop.scope.ScopedProxyFactoryBean

beans = {
    serviceInitLoggingBeanPostProcessor(ServiceInitLoggingBeanPostProcessor)

    //**** Plugin behavior

    xmlns lang:'http://www.springframework.org/schema/lang'

    File pluginDir = new File("${application.config.auroraHome}/plugins/")
    if (pluginDir.exists()) {
        pluginDir.eachFileMatch(FileType.FILES, ~/.*\.groovy/) { File plugin ->
            String beanName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, plugin.name.replace('.groovy', ''))
            lang.groovy(id: beanName, 'script-source': "file:${application.config.auroraHome}/plugins/${plugin.name}",
                    'refresh-check-delay': application.config.plugin.refreshDelay?: -1)
        }
    }

    def log4jPath = "${application.config.auroraHome}/log4j.properties"
    def exists = new File(log4jPath).exists()

    println "${log4jPath} exists:" + exists

    if (!exists) {
        log4jPath = "${application.config.auroraHome}/log4j.xml"
        exists = new File(log4jPath).exists()
        println "${log4jPath} exists:" + exists
    }

    if (exists) {
        println "Using logging configuration from ${log4jPath}"

        log4jConfigurer(org.springframework.beans.factory.config.MethodInvokingFactoryBean) {
            targetClass = "org.springframework.util.Log4jConfigurer"
            targetMethod = "initLogging"
            arguments = log4jPath
        }
    } else {
        println "Using default logging configuration"
    }

    objectMapper(ObjectMapper)
    objectMapperConfig(objectMapper:'configure', DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    jsonSlurper(JsonSlurper)

    sessionStorage(SessionStorage) { bean ->
        bean.scope = 'session'
    }

    sessionStorageService(ScopedProxyFactoryBean) {
        targetBeanName = 'sessionStorage'
        proxyTargetClass = true
    }


}
