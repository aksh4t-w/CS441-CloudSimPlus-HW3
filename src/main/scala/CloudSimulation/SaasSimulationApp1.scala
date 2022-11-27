package CloudSimulation

import HelperUtils.CommonUtils._
import com.typesafe.config.{Config, ConfigFactory}
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.Datacenter
import org.cloudbus.cloudsim.hosts.Host

import org.cloudbus.cloudsim.vms.Vm
import org.cloudsimplus.builders.tables.CloudletsTableBuilder

import java.util
import java.util.ArrayList
import scala.collection.convert.ImplicitConversions.`list asScalaBuffer`

//  This simulation is tries to simulate an Email application hosted on the cloud, similar to Gmail.
class SaasSimulationApp1 {
  val config: Config = ConfigFactory.load("SaaS_EMailApp.conf")
  val simulation = new CloudSim
  val hostList = new ArrayList[Host]
  val datacenterList: util.ArrayList[Datacenter] = createDatacenters(config, simulation, hostList)

  val brokerList = createBrokers(config, simulation)
  val brokers = brokerList.length
  val vmLists = new ArrayList[ArrayList[Vm]](brokers)

  // Round robin assignment of brokers to datacenters
  (0 until brokers).foreach(i => {
    connectToDatacenter(config, brokerList.get(i), i % datacenterList.length, datacenterList, simulation)
    vmLists.add(createVMs(config))
    val cloudletList = createCloudlets(config)
    brokerList.get(i).submitVmList(vmLists.get(i))
    brokerList.get(i).submitCloudletList(cloudletList)
  })

  simulation.start
  brokerList.forEach(broker => {
    new CloudletsTableBuilder(broker.getCloudletFinishedList).build()
    printTotalVmsCost(broker)
//    val brokerVmList : ArrayList[Vm] = broker.getVmCreatedList
    printVMsPowerConsumption(config, vmLists.get(0))
  })
  printHostsCpuPowerConsumption(hostList)
}

object SaasSimulationApp1 {
  def main(args: Array[String]): Unit = {
    new SaasSimulationApp1
  }
}