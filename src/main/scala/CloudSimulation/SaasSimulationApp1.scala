package CloudSimulation

import HelperUtils.CommonUtils._
import HelperUtils.CreateLogger
import com.typesafe.config.{Config, ConfigFactory}
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.Datacenter
import org.cloudbus.cloudsim.hosts.Host
import org.cloudbus.cloudsim.vms.Vm
import org.cloudsimplus.builders.tables.CloudletsTableBuilder
import org.slf4j.Logger

import java.util
import java.util.ArrayList
import scala.collection.convert.ImplicitConversions.`list asScalaBuffer`

//  This simulation tries to simulate an Email application hosted on the cloud, similar to Gmail.
class SaasSimulationApp1 {
  val logger: Logger = CreateLogger(classOf[Any])
  val config: Config = ConfigFactory.load("SaaS_EMailApp.conf")
  val simulation = new CloudSim
  val hostList = new ArrayList[Host]
  val datacenterList: util.ArrayList[Datacenter] = createDatacenters(config, simulation, hostList)

  logger.info("Creating a Star Topology Network")
  configureStarTopology(config, datacenterList, simulation)

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

  // Printing costs and power consumption details.
  (0 until brokers).foreach(i => {
    new CloudletsTableBuilder(brokerList.get(i).getCloudletFinishedList).build()
    printTotalVmsCost(brokerList.get(i))
    printVMsPowerConsumption(config, vmLists.get(i))
  })
  printHostsCpuPowerConsumption(hostList)
}

object SaasSimulationApp1 {
  def main(args: Array[String]): Unit = {
    new SaasSimulationApp1
  }
}