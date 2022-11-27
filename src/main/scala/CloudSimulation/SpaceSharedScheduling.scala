package CloudSimulation

import HelperUtils.CommonUtils._
import com.typesafe.config.ConfigFactory
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.hosts.Host
import org.cloudsimplus.builders.tables.CloudletsTableBuilder

import java.util

class SpaceSharedScheduling() {
  val config = ConfigFactory.load("application.conf")

  // Creates a CloudSim object to initialize the simulation.
  val simulation = new CloudSim
  val hostList = new util.ArrayList[Host]
  val datacenterList = createDatacenters(config, simulation, hostList)
  configureStarTopology(config, datacenterList, simulation)

  //Creates a Broker that will act on behalf of a cloud user (customer).
  val broker0 = new DatacenterBrokerSimple(simulation)
  connectToDatacenter(config, broker0, 1, datacenterList, simulation)


  val vmList = createVMs(config)
  val cloudletList = createCloudlets(config)

  broker0.submitVmList(vmList)
  broker0.submitCloudletList(cloudletList)

  simulation.start

  new CloudletsTableBuilder(broker0.getCloudletFinishedList).build()
  printTotalVmsCost(broker0)
  printHostsCpuPowerConsumption(hostList)
  printVMsPowerConsumption(config, vmList)
}

object SpaceSharedScheduling {
  def main(args: Array[String]): Unit = {
    new SpaceSharedScheduling
  }
}
