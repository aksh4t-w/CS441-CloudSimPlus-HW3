package HelperUtils

import org.cloudbus.cloudsim.brokers.DatacenterBroker
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
import org.cloudbus.cloudsim.cloudlets.network.CloudletExecutionTask
import org.cloudbus.cloudsim.cloudlets.network.CloudletReceiveTask
import org.cloudbus.cloudsim.cloudlets.network.CloudletSendTask
import org.cloudbus.cloudsim.cloudlets.network.NetworkCloudlet
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.network.NetworkDatacenter
import org.cloudbus.cloudsim.hosts.network.NetworkHost
import org.cloudbus.cloudsim.network.switches.EdgeSwitch
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple
import org.cloudbus.cloudsim.resources.Pe
import org.cloudbus.cloudsim.resources.PeSimple
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull
import org.cloudbus.cloudsim.vms.network.NetworkVm
import org.cloudsimplus.builders.tables.CloudletsTableBuilder
import com.typesafe.config.Config
import org.cloudbus.cloudsim.vms.Vm
import org.slf4j.Logger

import java.util
import java.util.{ArrayList, List}
import scala.collection.convert.ImplicitConversions.{`collection AsScalaIterable`, `iterable AsScalaIterable`}


object NetworkUtils {
  def createNetworkDatacenter(config: Config, simulation: CloudSim) = {
    val cloudType = config.getString("CloudType")

    val HOSTS: Int = config.getInt(cloudType + ".Datacenter.HOSTS")
    val hostList = new util.ArrayList[NetworkHost]

    (1 to HOSTS).map(_ => {
      val host = createNetworkHost(config)
      hostList.add(host)
    })

    val dc = new NetworkDatacenter(simulation, hostList)
    dc.setSchedulingInterval(5)
    createNetwork(dc, simulation)
    dc
  }

  def createNetworkHost(config: Config) = {
    val cloudType = config.getString("CloudType")

    val HOST_PES: Int = config.getInt(cloudType + ".Host.HOST_PES")
    val HOST_MIPS: Int = config.getInt(cloudType + ".Host.HOST_MIPS")
    val HOST_RAM: Int = config.getInt(cloudType + ".Host.HOST_RAM")
    val HOST_BW: Int = config.getInt(cloudType + ".Host.HOST_BW")
    val HOST_STORAGE: Int = config.getInt(cloudType + ".Host.HOST_STORAGE")

    val peList = createPEs(HOST_PES, HOST_MIPS)
    val host = new NetworkHost(HOST_RAM, HOST_BW, HOST_STORAGE, peList)
    host.setRamProvisioner(new ResourceProvisionerSimple).setBwProvisioner(new ResourceProvisionerSimple).setVmScheduler(new VmSchedulerTimeShared)
    host
  }

  def createPEs(pesNumber: Int, mips: Long) = {
    val peList = new util.ArrayList[Pe]
    (1 to pesNumber).map(_ =>  {
      peList.add(new PeSimple(mips, new PeProvisionerSimple))
    })
    peList
  }

  def createNetwork(datacenter: NetworkDatacenter, simulation: CloudSim): Unit = {
    val edgeSwitches = new Array[EdgeSwitch](5)
    (0 to edgeSwitches.length-1).foreach(i => {
      edgeSwitches(i) = new EdgeSwitch(simulation, datacenter)
      datacenter.addSwitch(edgeSwitches(i))
    })

    println(edgeSwitches(0).getPorts)

    datacenter.getHostList.map(host => {
      val switchNum = getSwitchIndex(host, edgeSwitches(0).getPorts)
      edgeSwitches(switchNum).connectHost(host)
    })
  }

  def getSwitchIndex(host: NetworkHost, switchPorts: Int): Int =
    Math.round(host.getId % Integer.MAX_VALUE) / switchPorts

  //------------------------------------------------------------------------------------------------------------
  // VM definitions and functions:

  def createNetworkVm(config: Config, id: Int) = {
    val cloudType = config.getString("CloudType")

    val VMS: Int = config.getInt(cloudType + ".VM.VMS")
    val VM_PES: Int = config.getInt(cloudType + ".VM.VM_PES")
    val VM_MIPS: Int = config.getInt(cloudType + ".VM.VM_MIPS")
    val VM_RAM: Int = config.getInt(cloudType + ".VM.VM_RAM")
    val VM_BW: Int = config.getInt(cloudType + ".VM.VM_BW")
    val VM_SIZE: Int = config.getInt(cloudType + ".VM.VM_SIZE")

    val vm = new NetworkVm(id, VM_MIPS, VM_PES)
    vm.setRam(VM_RAM).setBw(VM_BW).setSize(VM_SIZE).setCloudletScheduler(new CloudletSchedulerTimeShared)
    vm
  }

  def createAndSubmitNetworkVMs(config:Config, broker: DatacenterBroker) = {
    val cloudType = config.getString("CloudType")

    val HOSTS: Int = config.getInt(cloudType + ".Datacenter.HOSTS")

    val list = new util.ArrayList[NetworkVm]
    (1 to HOSTS).map(i => {
      val vm = createNetworkVm(config, i)
      list.add(vm)
    })

    broker.submitVmList(list)
    list
  }

  //------------------------------------------------------------------------------------------------------------
  // Cloudlet definitions:
  def createNetworkCloudlets(config: Config, vmList: List[NetworkVm]) = {
    val cloudType = config.getString("CloudType")

    val CLOUDLETS: Int = config.getInt(cloudType + ".Cloudlet.CLOUDLETS")

    val networkCloudletList1 = new util.ArrayList[NetworkCloudlet](CLOUDLETS)
    val networkCloudletList2 = new util.ArrayList[NetworkCloudlet](CLOUDLETS)

    (1 to (CLOUDLETS/2)).map(i => {
      networkCloudletList1.add(createNetworkCloudlet(config, vmList.get(i-1)))
    })

    ((CLOUDLETS / 2)+1 to CLOUDLETS).map(i => {
      networkCloudletList2.add(createNetworkCloudlet(config, vmList.get(i-1)))
    })

    // Task assignments
    (1 to CLOUDLETS / 2).foreach(i => {
      addExecutionTask(config, networkCloudletList1.get(i-1))
      addSendTask(networkCloudletList1.get(i-1), networkCloudletList2.get(i-1))

      addReceiveTask(networkCloudletList2.get(i-1), networkCloudletList1.get(i-1))
      addExecutionTask(config, networkCloudletList2.get(i-1))
    })

    (1 to CLOUDLETS / 2).foreach(i => {
      networkCloudletList1.add(networkCloudletList2.get(i-1))
    })

    networkCloudletList1
  }

  def createNetworkCloudlet(config: Config, vm: NetworkVm) = {
    val cloudType = config.getString("CloudType")

    val CLOUDLET_PES: Int = config.getInt(cloudType + ".Cloudlet.CLOUDLET_PES")

    val CLOUDLET_FILE_SIZE: Int = config.getInt(cloudType + ".Cloudlet.CLOUDLET_FILE_SIZE")
    val CLOUDLET_OUTPUT_SIZE = CLOUDLET_FILE_SIZE

    val netCloudlet = new NetworkCloudlet(4000, CLOUDLET_PES)
    netCloudlet
      .setFileSize(CLOUDLET_FILE_SIZE)
      .setOutputSize(CLOUDLET_OUTPUT_SIZE)
      .setUtilizationModel(new UtilizationModelFull).setVm(vm).setBroker(vm.getBroker)
    netCloudlet
  }

  def addExecutionTask(config: Config, cloudlet: NetworkCloudlet): Unit = {
    val cloudType = config.getString("CloudType")
    val TASK_LENGTH: Int = config.getInt(cloudType + ".Cloudlet.CLOUDLET_LENGTH")
    val TASK_RAM = 400

    val task = new CloudletExecutionTask(cloudlet.getTasks.size, TASK_LENGTH)
    task.setMemory(TASK_RAM)
    cloudlet.addTask(task)
  }

  def addSendTask(sourceCloudlet: NetworkCloudlet, destinationCloudlet: NetworkCloudlet): Unit = {
    val task = new CloudletSendTask(sourceCloudlet.getTasks.size)
    val TASK_RAM = 400
    val NUMBER_OF_PACKETS_TO_SEND = 5
    val PACKET_DATA_LENGTH_IN_BYTES = 200

    task.setMemory(TASK_RAM)
    sourceCloudlet.addTask(task)

    (1 to NUMBER_OF_PACKETS_TO_SEND).map(_ => task.addPacket(destinationCloudlet, PACKET_DATA_LENGTH_IN_BYTES))

  }

  private def addReceiveTask(cloudlet: NetworkCloudlet, sourceCloudlet: NetworkCloudlet): Unit = {
    val task = new CloudletReceiveTask(cloudlet.getTasks.size, sourceCloudlet.getVm)
    val TASK_RAM = 400
    val NUMBER_OF_PACKETS_TO_SEND = 5
    task.setMemory(TASK_RAM)
    task.setExpectedPacketsToReceive(NUMBER_OF_PACKETS_TO_SEND)
    cloudlet.addTask(task)
  }

  //------------------------------------------------------------------------------------------------------------
  // Display Simulation Results:
  def showSimulationResults(broker: DatacenterBrokerSimple, datacenter: NetworkDatacenter): Unit = {
    val newList = broker.getCloudletFinishedList
    new CloudletsTableBuilder(newList).build()
    System.out.println()

    datacenter.getHostList.map(host =>
      System.out.printf("Host %d data transferred: %d bytes%n", host.getId, host.getTotalDataTransferBytes)
    )

    System.out.println(getClass.getSimpleName + " finished!")
  }
}
