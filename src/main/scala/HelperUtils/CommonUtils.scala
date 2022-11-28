package HelperUtils

import org.cloudbus.cloudsim.resources.Processor
import org.cloudbus.cloudsim.schedulers.vm.{VmSchedulerSpaceShared, VmSchedulerTimeShared}
import org.cloudbus.cloudsim.vms.{HostResourceStats, Vm, VmCost, VmResourceStats, VmSimple}
import org.cloudsimplus.autoscaling.{VerticalVmScaling, VerticalVmScalingSimple}
import org.cloudsimplus.listeners.EventInfo

import scala.collection.convert.ImplicitConversions.{`collection AsScalaIterable`, `list asScalaBuffer`}
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
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared
import org.cloudbus.cloudsim.utilizationmodels.{UtilizationModelDynamic, UtilizationModelFull}
import org.cloudsimplus.builders.tables.CloudletsTableBuilder

import com.typesafe.config.Config
import org.cloudbus.cloudsim.power.models.PowerModelHostSimple
import org.slf4j.Logger

import java.util.Comparator.comparingLong
import java.util.{ArrayList, List}


object CommonUtils {
  // Creates logger to do logging at different levels.
  val logger: Logger = CreateLogger(classOf[Any])

  // -------------------------------------------------------------------------------------------------------------------
  // Function to create and return a single host based on some scheduling policy.
  def createHost(config: Config, id: Int): Host = {
    val cloudType = config.getString("CloudType")

    val HOST_PES : Int = config.getInt(cloudType + ".Host.HOST_PES")
    val HOST_MIPS : Int = config.getInt(cloudType + ".Host.HOST_MIPS")
    val HOST_RAM : Int = config.getInt(cloudType + ".Host.HOST_RAM")
    val HOST_BW : Int = config.getInt(cloudType + ".Host.HOST_BW")
    val HOST_STORAGE : Int = config.getInt(cloudType + ".Host.HOST_STORAGE")
    val VM_SCHEDULER : String = config.getString(cloudType + ".Host.VM_SCHEDULER")

    val HOST_START_UP_DELAY : Int = config.getInt(cloudType + ".Host.HOST_START_UP_DELAY")
    val HOST_SHUT_DOWN_DELAY : Int = config.getInt(cloudType + ".Host.HOST_SHUT_DOWN_DELAY")
    val HOST_START_UP_POWER : Int = config.getInt(cloudType + ".Host.HOST_START_UP_POWER")
    val HOST_SHUT_DOWN_POWER : Int = config.getInt(cloudType + ".Host.HOST_SHUT_DOWN_POWER")
    val MAX_POWER : Int = config.getInt(cloudType + ".Host.MAX_POWER")
    val STATIC_POWER : Int = config.getInt(cloudType + ".Host.STATIC_POWER")

    val peList = new ArrayList[Pe](HOST_PES)

    (1 to HOST_PES).map(_ => peList.add(new PeSimple(HOST_MIPS)))

    val host: Host = new HostSimple(HOST_RAM, HOST_BW, HOST_STORAGE, peList)

    val powerModel = new PowerModelHostSimple(MAX_POWER, STATIC_POWER)
    powerModel
      .setStartupDelay(HOST_START_UP_DELAY)
      .setShutDownDelay(HOST_SHUT_DOWN_DELAY)
      .setStartupPower(HOST_START_UP_POWER)
      .setShutDownPower(HOST_SHUT_DOWN_POWER)
    host.setPowerModel(powerModel)
    host.setId(id)

    VM_SCHEDULER match {
      case "SpaceShared" =>
        logger.info("Host is using Space Shared Policy for Scheduling VMs.")
        host.setVmScheduler(new VmSchedulerSpaceShared).enableUtilizationStats
        host

      case "TimeShared" =>
        logger.info("Host is using Time Shared Policy for Scheduling VMs.")
        host.setVmScheduler(new VmSchedulerTimeShared).enableUtilizationStats
        host

      case _ =>
        logger.info("Host is using Default Policy for Scheduling VMs.")
        host.enableUtilizationStats
        host
    }


  }

  // -------------------------------------------------------------------------------------------------------------------
  // Function to create a single datacenter
  def createDatacenter(config: Config, simulation: CloudSim, hostList: ArrayList[Host]) = {
    val cloudType = config.getString("CloudType")

    val HOSTS : Int = config.getInt(cloudType + ".Datacenter.HOSTS")
    val COST_PER_SECOND: Double = config.getDouble(cloudType + ".Datacenter.COST_PER_SECOND")
    val COST_PER_MEM: Double = config.getDouble(cloudType + ".Datacenter.COST_PER_MEM")
    val COST_PER_STORAGE: Double = config.getDouble(cloudType + ".Datacenter.COST_PER_STORAGE")
    val COST_PER_BW: Double = config.getDouble(cloudType + ".Datacenter.COST_PER_BW")
    val SCHEDULING_INTERVAL: Int = config.getInt(cloudType + ".Datacenter.SCHEDULING_INTERVAL")

    val dcHostList = new ArrayList[Host](HOSTS)

    (1 to HOSTS).map { i =>
      val host = createHost(config, i)
      dcHostList.add(host)
      hostList.add(host)
    }

    val dc = new DatacenterSimple(simulation, dcHostList)
    dc.setSchedulingInterval(SCHEDULING_INTERVAL)
    dc.getCharacteristics.setCostPerSecond(COST_PER_SECOND).setCostPerMem(COST_PER_MEM).setCostPerStorage(COST_PER_STORAGE).setCostPerBw(COST_PER_BW)

    dc
  }

  // Function to create multiple datacenters and return their list.
  def createDatacenters(config: Config, simulation: CloudSim, hostList: ArrayList[Host]): ArrayList[Datacenter] = {
    val cloudType = config.getString("CloudType")
    val DATACENTERS = config.getInt(cloudType + ".Datacenter.DATACENTERS")
    val datacenterList = new ArrayList[Datacenter]
    (1 to DATACENTERS).map(_ => {
      datacenterList.add(createDatacenter(config, simulation, hostList))
    })

    datacenterList
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Function to create and return a list of VMs
  def createVMs(config: Config): ArrayList[Vm] = {
    val cloudType = config.getString("CloudType")

    val VMS : Int = config.getInt(cloudType + ".VM.VMS")
    val VM_PES : Int = config.getInt(cloudType + ".VM.VM_PES")
    val VM_MIPS : Int = config.getInt(cloudType + ".VM.VM_MIPS")
    val VM_RAM : Int = config.getInt(cloudType + ".VM.VM_RAM")
    val VM_BW : Int = config.getInt(cloudType + ".VM.VM_BW")
    val VM_SIZE : Int = config.getInt(cloudType + ".VM.VM_SIZE")
    
    val vmList = new ArrayList[Vm](VMS)
    (1 to VMS).map(i => {
      val vm = new VmSimple(i, VM_MIPS, VM_PES)
      vm.setRam(VM_RAM).setBw(VM_BW).setSize(VM_SIZE).setCloudletScheduler(new CloudletSchedulerTimeShared).enableUtilizationStats
      vmList.add(vm)
    })
    vmList
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Function to create and return a list of Cloudlets
  def createCloudlets(config: Config): ArrayList[Cloudlet] = {
    val cloudType = config.getString("CloudType")

    val CLOUDLETS : Int = config.getInt(cloudType + ".Cloudlet.CLOUDLETS")
    val CLOUDLET_LENGTH : Int = config.getInt(cloudType + ".Cloudlet.CLOUDLET_LENGTH")
    val CLOUDLET_PES : Int = config.getInt(cloudType + ".Cloudlet.CLOUDLET_PES")
    val CLOUDLET_FILE_SIZE : Int = config.getInt(cloudType + ".Cloudlet.CLOUDLET_FILE_SIZE")

    val cloudletList = new ArrayList[Cloudlet](CLOUDLETS)
    val utilizationModelFull = new UtilizationModelFull

    val utilizationModel = new UtilizationModelDynamic(0.5)
    (1 to CLOUDLETS).map(_ => {
      val cloudlet = new CloudletSimple(CLOUDLET_LENGTH, CLOUDLET_PES, utilizationModel)

      cloudlet
        .setSizes(CLOUDLET_FILE_SIZE)
        .setFileSize(CLOUDLET_FILE_SIZE)
        .setOutputSize(CLOUDLET_FILE_SIZE)
        .setUtilizationModelCpu(utilizationModelFull)
        .setUtilizationModelRam(utilizationModel)
        .setUtilizationModelBw(utilizationModel)

      cloudletList.add(cloudlet)
    })
    cloudletList
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Function to create and return a list of Brokers
  def createBrokers(config: Config, simulation: CloudSim) = {
    val cloudType = config.getString("CloudType")
    val BROKERS: Int = config.getInt(cloudType + ".BROKERS")

    val list = new ArrayList[DatacenterBroker](BROKERS)
    (0 until BROKERS).map(- => {
      val broker = new DatacenterBrokerSimple(simulation)

      broker.setVmDestructionDelayFunction((vm: Vm) => 4.0)
      list.add(broker)
    })

    list
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Function to assign a broker to a datacenter
  def connectToDatacenter(config: Config, broker: DatacenterBroker, dc_number: Int, datacenterList: ArrayList[Datacenter], simulation: CloudSim): Unit = {
    val cloudType = config.getString("CloudType")

    val NETWORK_BW: Int = config.getInt(cloudType + ".NETWORK_BW")
    val NETWORK_LATENCY: Int = config.getInt(cloudType + ".NETWORK_LATENCY")

    val networkTopology = new BriteNetworkTopology
    simulation.setNetworkTopology(networkTopology)
    networkTopology.addLink(broker, datacenterList.get(dc_number), NETWORK_BW, NETWORK_LATENCY)
  }
  // -------------------------------------------------------------------------------------------------------------------
  // Network configurations:
  // Ring Topology:
  def configureNetwork(config: Config, datacenterList: ArrayList[Datacenter], simulation: CloudSim): Unit = {
    val cloudType = config.getString("CloudType")

    val NETWORK_BW : Int = config.getInt(cloudType + ".NETWORK_BW")
    val NETWORK_LATENCY : Int = config.getInt(cloudType + ".NETWORK_LATENCY")

    val DATACENTERS = datacenterList.length
    val networkTopology = new BriteNetworkTopology
    simulation.setNetworkTopology(networkTopology)

    for (i <- 0 until DATACENTERS - 1) {
      networkTopology.addLink(datacenterList.get(i), datacenterList.get(i + 1), NETWORK_BW, NETWORK_LATENCY)
    }
    networkTopology.addLink(datacenterList.get(DATACENTERS - 1), datacenterList.get(0), NETWORK_BW, NETWORK_LATENCY)
  }

  // Star Topology:
  def configureStarTopology(config: Config, datacenterList: ArrayList[Datacenter], simulation: CloudSim): Unit = {
    val cloudType = config.getString("CloudType")

    val NETWORK_BW: Int = config.getInt(cloudType + ".NETWORK_BW")
    val NETWORK_LATENCY: Int = config.getInt(cloudType + ".NETWORK_LATENCY")

    val DATACENTERS = datacenterList.length
    val networkTopology = new BriteNetworkTopology
    simulation.setNetworkTopology(networkTopology)


    (1 to DATACENTERS-1).foreach(i =>
      networkTopology.addLink(datacenterList.get(0), datacenterList.get(i), NETWORK_BW, NETWORK_LATENCY)
    )
  }
  // -------------------------------------------------------------------------------------------------------------------
  //                                    Load balancing: Scalable VMs
  // A clock listener event that is attached to each VM.
  def onClockTickListener(evt: EventInfo, vmList: List[Vm]): Unit = {
    vmList.forEach((vm: Vm) =>
      System.out.printf("\t\tTime %6.1f: Vm %d CPU Usage: %6.2f%% (%2d vCPUs. Running Cloudlets: #%d). RAM usage: %.2f%% (%d MB)%n",
        evt.getTime, vm.getId, vm.getCpuPercentUtilization * 100.0, vm.getNumberOfPes,
        vm.getCloudletScheduler.getCloudletExecList.size, vm.getRam.getPercentUtilization * 100,
        vm.getRam.getAllocatedResource))
  }

  // Function to create and return a list of Scalable VMs
  def createListOfScalableVms(config: Config): List[Vm] = {
    val cloudType = config.getString("CloudType")

    val VMS: Int = config.getInt(cloudType + ".VM.VMS")
    val VM_PES: Int = config.getInt(cloudType + ".VM.VM_PES")
    val VM_MIPS: Int = config.getInt(cloudType + ".VM.VM_MIPS")
    val VM_RAM: Int = config.getInt(cloudType + ".VM.VM_RAM")
    val VM_BW: Int = config.getInt(cloudType + ".VM.VM_BW")
    val VM_SIZE: Int = config.getInt(cloudType + ".VM.VM_SIZE")

    val newVmList = new ArrayList[Vm](VMS)
    (1 to VMS).map (_ => {
      val vm = new VmSimple(VM_MIPS, VM_PES).setRam(VM_RAM).setBw(VM_BW).setSize(VM_SIZE)
      vm.setPeVerticalScaling(createVerticalPeScaling)
      newVmList.add(vm)
    })
    newVmList
  }

  def createVerticalPeScaling (): VerticalVmScaling = {
    //The percentage in which the number of PEs has to be scaled
    val scalingFactor: Double = 0.1
    val verticalCpuScaling: VerticalVmScalingSimple = new VerticalVmScalingSimple(classOf[Processor], scalingFactor)

    val multiplier = 2
//    val lowerCpuUtilizationThreshold = 0.4
//    val upperCpuUtilizationThreshold = 0.8

    verticalCpuScaling.setResourceScaling((vs: VerticalVmScaling) => multiplier * vs.getScalingFactor * vs.getAllocatedResource)

    verticalCpuScaling.setLowerThresholdFunction(this.lowerCpuUtilizationThreshold)
    verticalCpuScaling.setUpperThresholdFunction(this.upperCpuUtilizationThreshold)

    verticalCpuScaling
    }

    def lowerCpuUtilizationThreshold(vm: Vm) = 0.4

    def upperCpuUtilizationThreshold(vm: Vm) = 0.8

//  def createCloudletListsWithDifferentDelays(config: Config): Unit = {
//    val CLOUDLETS : Int = config.getInt("CloudSimConfig.Cloudlet.CLOUDLETS")
//    val CLOUDLET_LENGTH : Int = config.getInt("CloudSimConfig.Cloudlet.CLOUDLET_LENGTH")
//
//    val pesNumber = 2
//    val cloudlets = (CLOUDLETS * 1.5).round
//    var i = 1
//
//    (0 to cloudlets).map(i => {
//      val delay = i*2
//      val length = CLOUDLET_LENGTH * i
//      cloudletList.add(createCloudlet(length, pesNumber, delay))
//    })
//    }

  // -------------------------------------------------------------------------------------------------------------------
  // Printing Costs
  def printTotalVmsCost(broker: DatacenterBroker): Unit = {
    System.out.println()
    var totalCost: Double = 0
    var processingTotalCost: Double = 0
    var memoryTotalCost: Double = 0
    var storageTotalCost: Double = 0
    var bwTotalCost: Double = 0

    (broker.getVmCreatedList: List[Vm]).foreach(vm => {
      val cost = new VmCost(vm)
      processingTotalCost += cost.getProcessingCost
      memoryTotalCost += cost.getMemoryCost
      storageTotalCost += cost.getStorageCost
      bwTotalCost += cost.getBwCost
      totalCost += cost.getTotalCost

      System.out.println(cost)
    })

    System.out.printf("Total cost ($): processingTotalCost: %8.2f$    memoryTotalCost: %8.2f$   storageTotalCost: %8.2f$    bwTotalCost: %8.2f$   totalCost: %8.2f$%n",
      processingTotalCost, memoryTotalCost, storageTotalCost, bwTotalCost, totalCost)
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Power consumption
  def printHostsCpuPowerConsumption(hostList: ArrayList[Host]): Unit = {
    System.out.println()

    hostList.map(host =>  {
      printHostCpuPowerConsumption(host)
    })
    System.out.println()
  }

  def printHostCpuPowerConsumption(host: Host): Unit = {
    val cpuStats: HostResourceStats = host.getCpuUtilizationStats

    // The total Host's CPU utilization for the time specified by the map key
    val utilizationPercentMean: Double = cpuStats.getMean
    val watts: Double = host.getPowerModel.getPower(utilizationPercentMean)
    System.out.printf("Host %2d CPU Usage mean: %6.1f%% | Power Consumption mean: %8.0f W%n", host.getId, utilizationPercentMean * 100, watts)
  }


  def printVMsPowerConsumption(config: Config, vmList: ArrayList[Vm]): Unit = {
    vmList.sort(comparingLong((vm: Vm) => vm.getHost.getId))
    val cloudType = config.getString("CloudType")
    val STATIC_POWER: Int = config.getInt(cloudType + ".Host.STATIC_POWER")

    vmList.map(vm => {
      val powerModel = vm.getHost.getPowerModel

      val hostStaticPower =
      if (powerModel.isInstanceOf[PowerModelHostSimple]) STATIC_POWER
      else 0

      val hostStaticPowerByVm = hostStaticPower / vm.getHost.getVmCreatedList.size
      // VM CPU utilization relative to the host capacity
      val vmRelativeCpuUtilization = vm.getCpuUtilizationStats.getMean / vm.getHost.getVmCreatedList.size
      val vmPower = powerModel.getPower(vmRelativeCpuUtilization) - hostStaticPower + hostStaticPowerByVm // Watts
      val cpuStats = vm.getCpuUtilizationStats
      System.out.printf("VM   %2d CPU Usage Mean: %6.1f%% | Power Consumption Mean: %8.0f W%n", vm.getId, cpuStats.getMean * 100, vmPower)
    })
  }
}
