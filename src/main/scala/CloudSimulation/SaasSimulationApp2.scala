package CloudSimulation

import HelperUtils.CommonUtils._
import com.typesafe.config.{Config, ConfigFactory}
import org.cloudbus.cloudsim.brokers.DatacenterBroker
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.Datacenter
import org.cloudbus.cloudsim.hosts.Host
import org.cloudbus.cloudsim.vms.Vm
import org.cloudsimplus.builders.tables.CloudletsTableBuilder

import java.util
import java.util.ArrayList
import scala.collection.convert.ImplicitConversions.`list asScalaBuffer`

//  This simulation is tries to simulate a video editing application hosted on the cloud.
class SaasSimulationApp2 {
  val config: Config = ConfigFactory.load("SaaS_VideoEditor.conf")
  val simulation = new CloudSim
  val hostList = new ArrayList[Host]
  val datacenterList: ArrayList[Datacenter] = createDatacenters(config, simulation, hostList)

  val brokerList: ArrayList[DatacenterBroker] = createBrokers(config, simulation)
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

object SaasSimulationApp2 {
  def main(args: Array[String]): Unit = {
    new SaasSimulationApp2
  }
}