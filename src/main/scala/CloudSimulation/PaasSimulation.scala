package CloudSimulation

import HelperUtils.CommonUtils._
import com.typesafe.config.ConfigFactory
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.schedulers.vm.{VmSchedulerSpaceShared, VmSchedulerTimeShared}
import org.cloudsimplus.builders.tables.CloudletsTableBuilder

import java.util

class PaasSimulation { }

object PaasSimulation {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load("PaaS.conf")
    val task = "WebsiteHostingService"

    val simulation = new CloudSim

  }
}