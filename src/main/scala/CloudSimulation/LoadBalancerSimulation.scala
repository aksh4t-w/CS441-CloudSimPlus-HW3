package CloudSimulation

import com.typesafe.config.ConfigFactory
import org.cloudbus.cloudsim.brokers.DatacenterBroker
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.Datacenter
import org.cloudsimplus.autoscaling.HorizontalVmScaling
import org.cloudsimplus.builders.tables.CloudletsTableBuilder

import java.util._
import java.util.Comparator
import java.util.function.Function
import java.util.function.Predicate
import java.util.Comparator.comparingDouble
import HelperUtils.CommonUtils._
import org.cloudbus.cloudsim.hosts.Host

import java.util

object LoadBalancerSimulation {
  /**
   * The interval in which the Datacenter will schedule events.
   * As lower is this interval, sooner the processing of VMs and Cloudlets
   * is updated and you will get more notifications about the simulation execution.
   * However, that also affect the simulation performance.
   *
   * <p>A large schedule interval, such as 15, will make that just
   * at every 15 seconds the processing of VMs is updated. If a VM is overloaded, just
   * after this time the creation of a new one will be requested
   * by the VM's {@link HorizontalVmScaling Horizontal Scaling} mechanism.</p>
   *
   * <p>If this interval is defined using a small value, you may get
   * more dynamically created VMs than expected.
   * Accordingly, this value has to be trade-off.
   * For more details, see {@link Datacenter# getSchedulingInterval ( )}.</p>
   */
  private val SCHEDULING_INTERVAL = 5
  /**
   * The interval to request the creation of new Cloudlets.
   */
  private val CLOUDLETS_CREATION_INTERVAL = SCHEDULING_INTERVAL * 2
  private val HOSTS = 50
  private val HOST_PES = 32
  private val VMS = 4
  private val CLOUDLETS = 6
  /**
   * Different lengths that will be randomly assigned to created Cloudlets.
   */
  private val CLOUDLET_LENGTHS = Array(2000, 4000, 10000, 16000, 2000, 30000, 20000)

  def main(args: Array[String]): Unit = {
    new LoadBalancerSimulation
  }
}


class LoadBalancerSimulation() {
  val config = ConfigFactory.load("application.conf")
  //Creates a CloudSim object to initialize the simulation.
  val hostList = new util.ArrayList[Host]

  val simulation = new CloudSim

  val dcTest = createDatacenters(config, simulation, hostList)

  //Creates a Broker that will act on behalf of a cloud user (customer).
  val broker0 = new DatacenterBrokerSimple(simulation)

  val vmList = createListOfScalableVms(config)
  val cloudletList = createCloudlets(config)
  simulation.addOnClockTickListener(evt => onClockTickListener(evt, vmList))

  broker0.submitVmList(vmList)
  broker0.submitCloudletList(cloudletList)

  simulation.start

  new CloudletsTableBuilder(broker0.getCloudletFinishedList).build()
}
