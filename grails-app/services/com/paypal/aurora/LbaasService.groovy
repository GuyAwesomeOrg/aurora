package com.paypal.aurora

import com.paypal.aurora.exception.RestClientRequestException
import com.paypal.aurora.model.*

class LbaasService {
    def openStackRESTService
    def sessionStorageService
    def instanceService
    def networkService

    boolean isEnabled() {
        openStackRESTService.isServiceEnabled(openStackRESTService.LBMS)
    }

    List<Pool> getAllPools() {
        def resp = openStackRESTService.get(openStackRESTService.LBMS, addTenant('pools/'))
        List<Pool> result = []
        resp.tenantpools.pools.each {
            if ((it.respondsTo("name") && it.name) ||
                    (it.respondsTo("get") && it.get("name"))) {
                result << new Pool(it)
            } else {
                try {
                    result << getPool(it)
                } catch (RestClientRequestException e) {
                    log.debug(e.getMessage(), e)
                }
            }
        }
        return result
    }

    def Pool getPool(String name) {
        def resp = openStackRESTService.get(openStackRESTService.LBMS, addTenant("pools/$name"))
        return new Pool(resp.pool)
    }

    String addTenant(String path, def tenantName = null) {
        def name = tenantName ?: sessionStorageService.tenant.name
        "tenant/${name}/$path"
    }

    def deletePool(String name) {
        openStackRESTService.delete(openStackRESTService.LBMS, addTenant("pools/$name"))
    }

    def deletePools(List<String> poolNames) {
        def model = ServiceUtils.removeItems(this, "deletePool", poolNames)
        return model
    }

    void createPool(def pool) {
        openStackRESTService.put(openStackRESTService.LBMS, addTenant("pools/${pool.name}"), null, [pool: [
                name: pool.name,
                method: pool.lbMethod,
                monitors: pool.monitors,
                enabled: pool.enabled ? 'true' : 'false'
        ]])
    }

    def updatePool(def pool) {
        openStackRESTService.post(openStackRESTService.LBMS, addTenant("pools/${pool.id}"), [pool: [
                name: pool.name,
                method: pool.lbMethod,
                monitors: pool.monitors,
                enabled: pool.enabled ? 'true' : 'false'
        ]])
    }

    def getVips() {
        def resp = openStackRESTService.get(openStackRESTService.LBMS, addTenant('vips/'))
        def vips = new ArrayList<Vip>();
        for (vip in resp.vip) {
            vips << new Vip(vip)
        }
        vips
    }

    def getVip(def name) {
        def resp = openStackRESTService.get(openStackRESTService.LBMS, addTenant("vips/${name}"))
        new Vip(resp.vip)
    }

    def createVip(def params) {
        def body = ['vip': [
                'ip': params.ip,
                'name': params.name,
                'port': params.port,
                'protocol': params.protocol,
                'enabled': params.enabled == 'on'
        ]
        ]
        openStackRESTService.put(openStackRESTService.LBMS, addTenant("vips/${params.name}"), null, body)
    }

    def deleteVip(def name) {
        openStackRESTService.delete(openStackRESTService.LBMS, addTenant("vips/${name}"))
    }

    def deleteVips(List<String> vipNames) {
        def model = ServiceUtils.removeItems(this, "deleteVip", vipNames)
        return model
    }

    def getPolicies(def tenantName = null) {
        def resp = openStackRESTService.get(openStackRESTService.LBMS, addTenant('policies/', tenantName))
        def policies = []
        for (policy in resp.policy) {
            policies << new Policy(policy)
        }
        policies
    }

    def getPolicy(def policyName, def tenantName = null) {
        def resp = openStackRESTService.get(openStackRESTService.LBMS, addTenant("policies/${policyName}", tenantName))
        new Policy(resp.policy)
    }

    def createPolicy(def params, def tenantName = null) {
        def body = ['policy': ['name': params.name, 'rule': params.rule]]
        openStackRESTService.put(openStackRESTService.LBMS, addTenant("policies/${params.name}", tenantName), null, body)
    }

    def updatePolicy(def params, def tenantName = null) {
        def body = ['policy': ['name': params.name, 'rule': params.rule]]
        openStackRESTService.post(openStackRESTService.LBMS, addTenant("policies/${params.id}", tenantName), body)
    }

    def deletePolicy(def policyName, def tenantName = null) {
        openStackRESTService.delete(openStackRESTService.LBMS, addTenant("policies/${policyName}", tenantName))
    }

    def deletePolicies(List<String> policyNames, def tenantName = null) {
        return ServiceUtils.removeItems(this, "deletePolicy", policyNames, [tenantName])
    }

    List<Job> getJobs() {
        def jobs = []
        openStackRESTService.get(openStackRESTService.LBMS, addTenant('jobs')).Tenant_Job_Details.each { jobs << new Job(it) }
        jobs
    }

    Job getJobById(def jobId) {
        openStackRESTService.get(openStackRESTService.LBMS, addTenant("jobs/$jobId")).Tenant_Job_Details
    }

    List<LBService> getServices(String pool) {
        def lbServices = []
        openStackRESTService.get(openStackRESTService.LBMS, addTenant("pools/$pool/services")).service.each { lbServices << new LBService(it) }
        lbServices
    }

    List<String> getIPs(List<String> instanceIDs, String netInterface) {
        List<String> result = []
        instanceIDs.each {
            Instance instance = instanceService.getById(it)
            instance.networks.find { it.pool == netInterface }.each { result << it.ip }
            instance.floatingIps.find { it.pool == netInterface }.each { result << it.ip }
        }
        return result
    }

    def getServicesByInstance(Instance instance) {
        Set<String> ips = []
        instance.networks.each { ips << it.ip }
        instance.floatingIps.each { ips << it.ip }
        getAllServices().findAll { it.ip in ips }
    }

    def addServices(List<String> instanceIDs, String poolName, String serviceName, String netInterface, String port, String weight, boolean enabled) {
        List<String> ips = getIPs(instanceIDs, netInterface)
        addServiceIps(poolName, serviceName, ips, port, weight, enabled ? 'true' : 'false')
    }

    private def addServiceIps(String pool, String serviceName, List<String> ips, String port, String weight, String enabled) {
        def services = []
        ips.each { services << [name: serviceName ?: "$it:$port".toString(), ip: it, port: port, weight: weight, enabled: enabled] }
        postServices([pool: [[
                name: pool,
                services: services
        ]]])
    }

    private def postServices(def services) {
        openStackRESTService.post(openStackRESTService.LBMS, addTenant('pools'), services)
    }

    def deleteService(String service, String pool) {
        openStackRESTService.delete(openStackRESTService.LBMS, addTenant("pools/$pool/services/$service"))
    }

    def deleteServices(List<String> services, String pool) {
        return ServiceUtils.removeItems(this, "deleteService", services, [pool])
    }

    void changeEnabled(String pool, List<String> serviceNames, boolean enabled) {
        List<LBService> poolServices = getServices(pool)
        List<LBService> changedServices = poolServices.findAll({ it.name in serviceNames })
        def services = []
        changedServices.each {
            services << [name: it.name, ip: it.ip, port: it.port, weight: it.weight, enabled: enabled.toString()]
        }
        postServices([pool: [[
                name: pool,
                services: services
        ]]])
    }

    def getAllServices() {
        def result = []
        getAllPools().each { pool ->
            pool.services?.each {
                result << [pool: pool.name, name: it.name, ip: it.ip, port: it.port, enabled: it.enabled]
            }
        }
        return result
    }

    List<String> getMethods() {
        openStackRESTService.get(openStackRESTService.LBMS, addTenant('methods/')).methods
    }

    List<String> getMonitors() {
        openStackRESTService.get(openStackRESTService.LBMS, addTenant('monitors/')).monitors
    }
}



