/*******************************************************************************
* Copyright (c) 2014 GigaSpaces Technologies Ltd. All rights reserved
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
import java.util.concurrent.TimeUnit
import groovy.util.ConfigSlurper
import org.cloudifysource.utilitydomain.context.ServiceContextFactory
import util
import org.openspaces.admin.AdminFactory

context=ServiceContextFactory.serviceContext
config = new ConfigSlurper().parse(new File(context.serviceName+"-service.properties").toURL())
ip=context.getPrivateAddress()
lusPort=config.lusPort

uuid=context.attributes.thisInstance.uuid
if(uuid==null){
    uuid=UUID.randomUUID().toString()
    context.attributes.thisInstance.uuid=uuid
}

thisService=util.getThisService(context)

//Get locator(s)
mgmt=context.waitForService(config.managementService,5,TimeUnit.MINUTES)
assert (mgmt!=null && mgmt.instances.size()),"No management services found"
locators=""
lusnum=0

mgmt.instances.each{
    def lusname="lus${it.instanceId}"
    println "invoking update-hosts with ${it.hostAddress} ${lusname}"
    thisService.invoke("update-hosts",it.hostAddress,lusname as String)
    locators+="${lusname}:${config.lusPort},"
}
context.attributes.thisInstance["xaplocators"] = locators
println "locators = ${locators}"

new AntBuilder().sequential {
    exec(executable:"runxap.bat", osfamily:"windows",
            output:"runxap.${System.currentTimeMillis()}.out",
            error:"runxap.${System.currentTimeMillis()}.err"
    ){
        env(key:"XAPDIR", value:"${config.installDir}/${config.xapDir}")
        env(key:"GSA_JAVA_OPTIONS",value:"${config.gsa_jvm_options} -Dcom.gs.multicast.enabled=false -DUUID=${uuid} -Dcom.gs.multicast.discoveryPort=${config.lusPort} -Dcom.sun.jini.reggie.initialUnicastDiscoveryPort=${config.lusPort} -Dcom.gigaspaces.start.httpPort=${config.httpPort} -Dcom.gigaspaces.system.registryPort=${config.registryPort} -Dcom.gs.transport_protocol.lrmi.bind-port=${config.bindPort}")
        env(key:"GSC_JAVA_OPTIONS",value:"${config.gsc_jvm_options} -Dcom.gs.multicast.enabled=false -DUUID=${uuid} -Dcom.gs.multicast.discoveryPort=${config.lusPort} -Dcom.sun.jini.reggie.initialUnicastDiscoveryPort=${config.lusPort} -Dcom.gigaspaces.start.httpPort=${config.httpPort} -Dcom.gigaspaces.system.registryPort=${config.registryPort} -Dcom.gs.transport_protocol.lrmi.bind-port=${config.bindPort}")
        env(key:"LOOKUPLOCATORS",value:locators)
        env(key:"NIC_ADDR",value:"${ip}")
    }

    chmod(dir:"${context.serviceDirectory}",perm:"+x",includes:"*.sh")
    chmod(dir:"${config.installDir}/${config.xapDir}",perm:"+x",includes:"*.sh")
    exec(executable:"./runxap.sh", osfamily:"unix",
            output:"runxap.${System.currentTimeMillis()}.out",
            error:"runxap.${System.currentTimeMillis()}.err"
    ){
        env(key:"GSA_JAVA_OPTIONS",value:"${config.gsa_jvm_options} -Dcom.gs.multicast.enabled=false -DUUID=${uuid} -Dcom.gs.multicast.discoveryPort=${config.lusPort} -Dcom.sun.jini.reggie.initialUnicastDiscoveryPort=${config.lusPort} -Dcom.gigaspaces.start.httpPort=${config.httpPort} -Dcom.gigaspaces.system.registryPort=${config.registryPort} -Dcom.gs.transport_protocol.lrmi.bind-port=${config.bindPort}")
        env(key:"GSC_JAVA_OPTIONS",value:"${config.gsc_jvm_options} -Dcom.gs.multicast.enabled=false -DUUID=${uuid} -Dcom.gs.multicast.discoveryPort=${config.lusPort} -Dcom.sun.jini.reggie.initialUnicastDiscoveryPort=${config.lusPort} -Dcom.gigaspaces.start.httpPort=${config.httpPort} -Dcom.gigaspaces.system.registryPort=${config.registryPort} -Dcom.gs.transport_protocol.lrmi.bind-port=${config.bindPort}")
        env(key:"XAPDIR", value:"${context.serviceDirectory}/${config.installDir}/${config.xapDir}")
        env(key:"LOOKUPLOCATORS",value:"${locators}")
        env(key:"NIC_ADDR",value:"${ip}")
    }
}

println "XAP-Container started"
