package com.paypal.aurora

import com.paypal.aurora.exception.RestClientRequestException
import com.paypal.aurora.model.*
import grails.converters.JSON
import grails.converters.XML
import org.apache.shiro.grails.annotations.RoleRequired

import static org.apache.commons.lang.StringUtils.isNotEmpty

class RouterController {

    def sessionStorageService
    def quantumService

    def index() { redirect(action: 'list', params: params) }

    def list = {
        List<Router> routers = quantumService.routersList
        Map details = [routers: routers]
        withFormat {
            html { details }
            xml { new XML(details).render(response) }
            json { new JSON(details).render(response) }
        }
    }

    def show = {
        try {
            String id = params.id;
            Router router = quantumService.getRouterById(id)
            if (router != null) {
                Map details = [parent: '/router', router: router]
                withFormat {
                    html { details }
                    xml { new XML(details).render(response) }
                    json { new JSON(details).render(response) }
                }
            } else {
                redirect(action: 'list')
            }
        } catch (RestClientRequestException e) {
            response.status = ExceptionUtils.getExceptionCode(e)
            flash.message = ExceptionUtils.getExceptionMessage(e)
            chain(action: 'list')
        }
    }

    @RoleRequired('admin')
    def create = {
        [parent: "/router"]
    }

    @RoleRequired('admin')
    def save = {
        Router router = new Router()
        router.name = params.name
        try {
            quantumService.createRouter(router)
            redirect(action: 'list')
        } catch (RestClientRequestException e) {
            response.status = ExceptionUtils.getExceptionCode(e)
            flash.message = ExceptionUtils.getExceptionMessage(e)
            chain(action: 'create', params: params)
        }
    }

    def clearGateway = {
        String id = params.id
        Router router = new Router()
        router.id = id
        router.externalGatewayInfo = null
        try {
            quantumService.updateRouter(router)
            redirect(action: 'show', params: [id: params.id])
        } catch (RestClientRequestException e) {
            response.status = ExceptionUtils.getExceptionCode(e)
            flash.message = ExceptionUtils.getExceptionMessage(e)
            chain(action: 'show', params: [id: params.id])
        }
    }

    @RoleRequired('admin')
    def delete = {
        List<String> routerIds = Requests.ensureList(params.selectedRouters ?: params.routerId)
        def model = quantumService.deleteRouters(routerIds)
        def flashMessage = ResponseUtils.defineMessageByList("Could not delete routers: ", model.notRemovedItems)
        response.status = ResponseUtils.defineResponseStatus(model, flashMessage)
        withFormat {
            html{
                flash.message = flashMessage
                redirect(action : 'list')
            }
            xml { new XML(model).render(response)}
            json { new JSON(model).render(response)}
        }
    }

    def setGateway = {
        try {
            params.router = quantumService.getRouterById(params.id)
            params.networks = getExternalNetworks(quantumService.networkList)
            [parent: "/router/show/$params.id"]
        } catch (RestClientRequestException e) {
            response.status = ExceptionUtils.getExceptionCode(e)
            flash.message = ExceptionUtils.getExceptionMessage(e)
            chain(action: 'create', params: params, parent: "/router/show/$params.id")
        }
    }

    def saveGateway = {
        String id = params.id
        Router router = new Router()
        router.id = id
        ExternalGatewayInfo externalGatewayInfo = new ExternalGatewayInfo()
        externalGatewayInfo.networkId = params.network
        externalGatewayInfo.enableSnat = true
        router.externalGatewayInfo = externalGatewayInfo
        try {
            quantumService.updateRouter(router)
            redirect(action: 'show', params: [id: params.id])
        } catch (RestClientRequestException e) {
            response.status = ExceptionUtils.getExceptionCode(e)
            flash.message = ExceptionUtils.getExceptionMessage(e)
            chain(action: 'show', params: params)
        }
    }

    def addInterface = {
        try {
            params.router = quantumService.getRouterById(params.id)
            params.networks = getInternalNetworks(quantumService.networkList)
            [parent: "/router/show/$params.id"]
        } catch (RestClientRequestException e) {
            response.status = ExceptionUtils.getExceptionCode(e)
            flash.message = ExceptionUtils.getExceptionMessage(e)
            chain(action: 'create', params: params, parent: "/router/show/$params.id")
        }
    }

    def saveInterface = {
        String routerId = params.id
        String subnetId = params.subnet
        String ip = params.ip
        try {
            if(isNotEmpty(ip)) {
                Subnet subnet = quantumService.getSubnetById(subnetId)
                Port port = new Port()
                port.networkId = subnet.networkId
                FixedIp fixedIp = new FixedIp()
                fixedIp.ipAddress = ip
                fixedIp.subnetId = subnetId
                port.fixedIps = new ArrayList<FixedIp>()
                port.fixedIps << fixedIp
                Port createdPort = quantumService.createPort(port)
                quantumService.addRouterInterfaceByPort(routerId, createdPort.id)
            } else {
                quantumService.addRouterInterfaceBySubnet(routerId, subnetId)
            }

            redirect(action: 'show', params: [id: params.id])
        } catch (RestClientRequestException e) {
            response.status = ExceptionUtils.getExceptionCode(e)
            flash.message = ExceptionUtils.getExceptionMessage(e)
            chain(action: 'addInterface', params: params)
        }
    }

    def deleteInterface = {
        String routerId = params.id
        List<String> portIds = Requests.ensureList(params.selectedPorts ?: params.portId)
        def model = quantumService.removeRouterInterfaces(routerId, portIds)
        def flashMessage = ResponseUtils.defineMessageByList("Could not delete interface with id: ", model.notRemovedItems)
        response.status = ResponseUtils.defineResponseStatus(model, flashMessage)
        withFormat {
            html{
                flash.message = flashMessage
                redirect(action: 'show', params: [id: params.id])
            }
            xml {new XML(model).render(response)}
            json{new JSON(model).render(response)}
        }
    }

    List<Network> getExternalNetworks(networks) {
        List<Network> externalNetworks = []
        for (Network network : networks) {
            if(network.external) {
                externalNetworks << network
            }

        }
        return externalNetworks
    }

    List<Network> getInternalNetworks(networks) {
        List<Network> internalNetworks = []
        for (Network network : networks) {
            if(!network.external) {
                internalNetworks << network
            }

        }
        return internalNetworks
    }
}
