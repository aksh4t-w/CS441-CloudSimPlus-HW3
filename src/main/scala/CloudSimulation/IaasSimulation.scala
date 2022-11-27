package CloudSimulation

import HelperUtils.CommonUtils._
import HelperUtils.CreateLogger
import com.typesafe.config.ConfigFactory
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.hosts.Host
import org.cloudbus.cloudsim.schedulers.vm.{VmSchedulerSpaceShared, VmSchedulerTimeShared}
import org.cloudsimplus.builders.tables.CloudletsTableBuilder
import org.slf4j.Logger

import java.util
import scala.collection.convert.ImplicitConversions.{`collection AsScalaIterable`, `list asScalaBuffer`}

class IaasSimulation {
  val logger: Logger = CreateLogger(classOf[Any])
  val config = ConfigFactory.load("IaaS.conf")
  val simulation = new CloudSim
  val hostList = new util.ArrayList[Host]
  val datacenterList = createDatacenters(config, simulation, hostList)

  val brokerList = createBrokers(config, simulation)
  val brokers = brokerList.length

  // Round robin assignment of brokers to datacenters
  (0 until brokers).foreach(i => {
    logger.info("Broker" + brokerList.get(i) + " connecting to Datacenter 6")
    connectToDatacenter(config, brokerList.get(i), 6, datacenterList, simulation)
//    val vmList = createVMs(config)
//    val cloudletList = createCloudlets(config)
//    brokerList.get(i).submitVmList(vmList)
//    brokerList.get(i).submitCloudletList(cloudletList)
  })

  simulation.start
  brokerList.forEach(broker => {
    new CloudletsTableBuilder(broker.getCloudletFinishedList).build()
  })
  printHostsCpuPowerConsumption(hostList)
}

object IaasSimulation {
def main(args: Array[String]): Unit = {
  new IaasSimulation
  }
}
