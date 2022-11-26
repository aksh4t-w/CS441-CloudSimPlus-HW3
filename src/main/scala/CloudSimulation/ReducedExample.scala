package CloudSimulation

import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
import org.cloudbus.cloudsim.cloudlets.CloudletSimple
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.DatacenterSimple
import org.cloudbus.cloudsim.hosts.HostSimple
import org.cloudbus.cloudsim.resources.PeSimple
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic
import org.cloudbus.cloudsim.vms.VmSimple
import org.cloudsimplus.builders.tables.CloudletsTableBuilder

import java.util

class ReducedExample { }

object ReducedExample {
  def main(args: Array[String]): Unit = { //tag::cloudsim-plus-reduced-example[]
    //Enables just some level of log messages
    //Log.setLevel(ch.qos.logback.classic.Level.WARN);

    //Creates a CloudSim object to initialize the simulation.
    val simulation = new CloudSim

    //Creates a Broker that will act on behalf of a cloud user (customer).
    val broker0 = new DatacenterBrokerSimple(simulation)

    //Host configuration
    val ram = 10000 //in Megabytes
    val storage = 100000
    val bw = 100000 //in Megabits/s
    val vmScheduler = new VmSchedulerTimeShared

    //Creates one Hosts with a specific list of CPU cores (PEs).
    //Uses a PeProvisionerSimple by default to provision PEs for VMs.
    //Uses ResourceProvisionerSimple by default for RAM and BW provisioning
    //Uses VmSchedulerSpaceShared by default for VM scheduling
    val host0 = new HostSimple(ram, bw, storage, util.List.of(new PeSimple(20000)))
    host0.setVmScheduler(vmScheduler)

    val dc0 = new DatacenterSimple(simulation, util.List.of(host0))

    val vm0 = new VmSimple(1000, 1)
    vm0.setRam(1000).setBw(1000).setSize(1000)

    // Test
    val vm1 = new VmSimple(1000, 1)
    vm1.setRam(2000).setBw(1000).setSize(2000)

    val vmList = util.List.of(vm0, vm1)

  val utilizationModel = new UtilizationModelDynamic(0.5)
    val cloudlet0 = new CloudletSimple(10000, 1, utilizationModel)
    val cloudlet1 = new CloudletSimple(10000, 1, utilizationModel)
    val cloudletList = util.List.of(cloudlet0, cloudlet1)

    broker0.submitVmList(vmList)
    broker0.submitCloudletList(cloudletList)


    simulation.start


    new CloudletsTableBuilder(broker0.getCloudletFinishedList).build()

  }
}
