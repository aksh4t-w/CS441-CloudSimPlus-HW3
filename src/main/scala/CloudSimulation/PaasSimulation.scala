package CloudSimulation

import HelperUtils.CommonUtils._
import HelperUtils.CreateLogger
import com.typesafe.config.ConfigFactory
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.Datacenter
import org.cloudbus.cloudsim.hosts.Host
import org.cloudbus.cloudsim.schedulers.vm.{VmSchedulerSpaceShared, VmSchedulerTimeShared}
import org.cloudbus.cloudsim.vms.Vm
import org.cloudsimplus.builders.tables.CloudletsTableBuilder
import org.slf4j.Logger

import java.util
import java.util.ArrayList
import scala.collection.convert.ImplicitConversions.`list asScalaBuffer`

//  This simulation tries to simulate an WebsiteHostingService application hosted on the cloud, similar to Heroku.
class PaasSimulation {
  val logger: Logger = CreateLogger(classOf[Any])
  val config = ConfigFactory.load("PaaS.conf")

  val simulation = new CloudSim
  val hostList = new ArrayList[Host]
  val datacenterList: util.ArrayList[Datacenter] = createDatacenters(config, simulation, hostList)

  logger.info("Creating a Star Topology Network")
  configureStarTopology(config, datacenterList, simulation)

  logger.info("Creating brokers:")
  val brokerList = createBrokers(config, simulation)
  val brokers = brokerList.length
  val vmLists = new ArrayList[ArrayList[Vm]](brokers)

  // Assignment of brokers to datacenter 5 as it is the PaaS datacenter
  logger.info("Starting VMs to be used, for the brokers")

  (0 until brokers).foreach(i => {
    connectToDatacenter(config, brokerList.get(i), 4, datacenterList, simulation)
    vmLists.add(createVMs(config))
    brokerList.get(i).submitVmList(vmLists.get(i))
  })

  simulation.start

  // Printing costs and power consumption details.
  (0 until brokers).foreach(i => {
    new CloudletsTableBuilder(brokerList.get(i).getCloudletFinishedList).build()
    printVMsPowerConsumption(config, vmLists.get(i))
  })
  printHostsCpuPowerConsumption(hostList)
}

object PaasSimulation {
  def main(args: Array[String]): Unit = {
    new PaasSimulation
  }
}