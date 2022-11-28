package CloudSimulation

import HelperUtils.CreateLogger
import HelperUtils.NetworkUtils._
import com.typesafe.config.{Config, ConfigFactory}
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.hosts.Host
import org.slf4j.Logger

import java.util

class NetworkSimulation {
  val logger: Logger = CreateLogger(classOf[Any])
  val config = ConfigFactory.load("NetworkSim.conf")
  //Creates a CloudSim object to initialize the simulation.
  val simulation = new CloudSim
  val hostList = new util.ArrayList[Host]
  val datacenter = createNetworkDatacenter(config, simulation)
  val broker = new DatacenterBrokerSimple(simulation)
  val vmList = createAndSubmitNetworkVMs(config, broker)
  val cloudletList = createNetworkCloudlets(config, vmList)
  broker.submitCloudletList(cloudletList)

  simulation.start
  showSimulationResults(broker, datacenter)

}

object NetworkSimulation {
  def main(args: Array[String]): Unit = {
    new NetworkSimulation
  }
}