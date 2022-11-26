package HelperUtils

import CloudSimulation.Simulations
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.Logger
import org.cloudbus.cloudsim.network.topologies.NetworkTopology
import org.cloudbus.cloudsim.vms.VmCost

import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`
//import CloudSimulation.Simulations.NETWORK_TOPOLOGY_FILE
import org.cloudbus.cloudsim.brokers.DatacenterBroker
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
import org.cloudbus.cloudsim.cloudlets.Cloudlet
import org.cloudbus.cloudsim.cloudlets.CloudletSimple
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.Datacenter
import org.cloudbus.cloudsim.datacenters.DatacenterSimple
import org.cloudbus.cloudsim.hosts.Host
import org.cloudbus.cloudsim.hosts.HostSimple
import org.cloudbus.cloudsim.network.topologies.BriteNetworkTopology
import org.cloudbus.cloudsim.resources.Pe
import org.cloudbus.cloudsim.resources.PeSimple
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared
import org.cloudbus.cloudsim.utilizationmodels.{UtilizationModelDynamic, UtilizationModelFull}
import org.cloudbus.cloudsim.vms.Vm
import org.cloudbus.cloudsim.vms.VmSimple
import org.cloudsimplus.builders.tables.CloudletsTableBuilder
import HelperUtils.CreateLogger

import java.util
import java.util.{ArrayList, List}

object DatacenterUtils {
  val logger: Logger = CreateLogger(classOf[Any])

//  val logger: Logger = LoggerFactory.getLogger(classOf[Nothing].getSimpleName)

  def createHost(config: Config) = {
    val HOST_PES : Int = config.getInt("datacenter.Host.HOST_PES")
    val HOST_MIPS : Int = config.getInt("datacenter.Host.HOST_MIPS")
    val HOST_RAM : Int = config.getInt("datacenter.Host.HOST_RAM")
    val HOST_BW : Int = config.getInt("datacenter.Host.HOST_BW")
    val HOST_STORAGE : Int = config.getInt("datacenter.Host.HOST_STORAGE")

    val peList = new util.ArrayList[Pe](HOST_PES)

    //List of Host's CPUs (Processing Elements, PEs)
    for (_ <- 0 until HOST_PES) {
      peList.add(new PeSimple(HOST_MIPS))
    }
    /*
      Uses ResourceProvisionerSimple by default for RAM and BW provisioning
      and VmSchedulerSpaceShared for VM scheduling.
    */
    new HostSimple(HOST_RAM, HOST_BW, HOST_STORAGE, peList)
  }

  def createDatacenter(config: Config, simulation: CloudSim) = {
    val HOSTS : Int = config.getInt("datacenter.Datacenter.HOSTS")
    val hostList = new util.ArrayList[Host](HOSTS)

    (1 to HOSTS).map { _ =>
      val host = createHost(config)
      hostList.add(host)
    }

    val dc = new DatacenterSimple(simulation, hostList)
    dc.getCharacteristics.setCostPerSecond(0.01).setCostPerMem(0.02).setCostPerStorage(0.001).setCostPerBw(0.005)

    //Uses a VmAllocationPolicySimple by default to allocate VMs
    dc
  }
}
