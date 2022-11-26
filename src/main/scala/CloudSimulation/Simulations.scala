/*
 * CloudSim Plus: A modern, highly-extensible and easier-to-use Framework for
 * Modeling and Simulation of Cloud Computing Infrastructures and Services.
 * http://cloudsimplus.org
 *
 *     Copyright (C) 2015-2021 Universidade da Beira Interior (UBI, Portugal) and
 *     the Instituto Federal de Educação Ciência e Tecnologia do Tocantins (IFTO, Brazil).
 *
 *     This file is part of CloudSim Plus.
 *
 *     CloudSim Plus is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     CloudSim Plus is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with CloudSim Plus. If not, see <http://www.gnu.org/licenses/>.
 */
package CloudSimulation

import com.typesafe.config.{Config, ConfigFactory}
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

import java.util
import java.util.{ArrayList, List}


/**
 * A minimal but organized, structured and re-usable CloudSim Plus example
 * which shows good coding practices for creating simulation scenarios.
 *
 * <p>It defines a set of constants that enables a developer
 * to change the number of Hosts, VMs and Cloudlets to create
 * and the number of {@link Pe}s for Hosts, VMs and Cloudlets.</p>
 *
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Plus 1.0
 */
object Simulations {

  private val HOSTS = 10
  private val HOST_PES = 4
  private val HOST_MIPS = 1000
  private val HOST_RAM = 2048 //in Megabytes
  private val HOST_BW = 10_000 //in Megabits/s
  private val HOST_STORAGE = 1_000_000

  private val VMS = 10
  private val VM_PES = 4

  private val CLOUDLETS = 21
  private val CLOUDLET_PES = 2
  private val CLOUDLET_LENGTH = 100_000

  /** In Megabits/s. */
  private val NETWORK_BW = 10.0

  /** In seconds. */
  private val NETWORK_LATENCY = 10.0

  def main(args: Array[String]): Unit = {
    new Simulations
  }
}

class Simulations private() {
  val config = ConfigFactory.load("application.conf")
  val DATACENTERS: Int = config.getInt("CloudSimConfig.DATACENTERS")
  val BROKERS: Int = config.getInt("CloudSimConfig.BROKERS")

  //  println(DATACENTERS)
  /*Enables just some level of log messages.
    Make sure to import org.cloudsimplus.util.Log;
  */
  //Log.setLevel(ch.qos.logback.classic.Level.WARN);
  val simulation = new CloudSim()
  val datacenterList: List[Datacenter] = createDatacenters
  val brokerList: List[DatacenterBroker] = createBrokers


  //Creates a broker that is a software acting on behalf a cloud customer to manage his/her VMs and Cloudlets
//  val broker0 = new DatacenterBrokerSimple(simulation)
  configureNetwork()

//  val vmList: util.ArrayList[Vm] = createVms
//  val cloudletList: util.ArrayList[Cloudlet] = createCloudlets

  brokerList.map(broker => {
      createAndSubmitVms(broker)
      createAndSubmitCloudlets(broker)
    }
  )
  //  broker0.submitVmList(vmList)
//  broker0.submitCloudletList(cloudletList)
  simulation.start
//  val finishedCloudlets = broker0.getCloudletFinishedList
//  new CloudletsTableBuilder(finishedCloudlets).build()

  brokerList.map(broker => {
    val finishedCloudlets = broker.getCloudletFinishedList
    new CloudletsTableBuilder(finishedCloudlets).build()
    }
  )
  printTotalVmsCost()
  /**
   * Creates a Datacenter and its Hosts.
   */

  /**
   * Creates a List of Datacenters, each Datacenter having
   * Hosts with a number of PEs higher than the previous Datacenter.
   *
   * @return
   */
  private def createDatacenters = {
    val list = new util.ArrayList[Datacenter](DATACENTERS)
    for (i <- 1 to DATACENTERS) {
      list.add(createDatacenter)
    }
    list
  }

  /**
   * Creates a Datacenter and its Hosts.
   *
   * @param hostsPes the number of PEs for the Hosts in the Datacenter created
   */
  private def createDatacenter = {
    val hostList = new util.ArrayList[Host](Simulations.HOSTS)
    for (i <- 0 until Simulations.HOSTS) {
      val host = createHost
      hostList.add(host)
    }

    val dc = new DatacenterSimple(simulation, hostList)
    dc.getCharacteristics.setCostPerSecond(0.01).setCostPerMem(0.02).setCostPerStorage(0.001).setCostPerBw(0.005)

    //Uses a VmAllocationPolicySimple by default to allocate VMs
    dc
  }


  private def configureNetwork(): Unit = {
    val networkTopology = new BriteNetworkTopology
    simulation.setNetworkTopology(networkTopology)

    for (i <- 0 until DATACENTERS-1) {
      networkTopology.addLink(datacenterList.get(i), datacenterList.get(i+1), Simulations.NETWORK_BW, Simulations.NETWORK_LATENCY)
    }
    networkTopology.addLink(datacenterList.get(DATACENTERS-1), datacenterList.get(0), Simulations.NETWORK_BW, Simulations.NETWORK_LATENCY)
//    networkTopology.addLink(datacenter1, datacenter2, Simulations.NETWORK_BW, Simulations.NETWORK_LATENCY)
//    networkTopology.addLink(datacenter2, broker0, Simulations.NETWORK_BW, Simulations.NETWORK_LATENCY)

  }

  private def createBrokers = {
    val list = new util.ArrayList[DatacenterBroker](BROKERS)
    for (i <- 0 until BROKERS) {
      val broker = new DatacenterBrokerSimple(simulation)

      //broker.setVmDestructionDelayFunction(vm -> 0.0);
      broker.setVmDestructionDelayFunction((vm: Vm) => 4.0)
      list.add(broker)
    }
    list
  }

  private def createHost = {
    val peList = new util.ArrayList[Pe](Simulations.HOST_PES)
    //List of Host's CPUs (Processing Elements, PEs)
    for (_ <- 0 until Simulations.HOST_PES) { //Uses a PeProvisionerSimple by default to provision PEs for VMs
      peList.add(new PeSimple(Simulations.HOST_MIPS))
    }
    /*
      Uses ResourceProvisionerSimple by default for RAM and BW provisioning
      and VmSchedulerSpaceShared for VM scheduling.
    */
    new HostSimple(Simulations.HOST_RAM, Simulations.HOST_BW, Simulations.HOST_STORAGE, peList)
  }

  /**
   * Creates a list of VMs.
   */
  private def createAndSubmitVms(broker: DatacenterBroker) = {
    val vmList = new util.ArrayList[Vm](Simulations.VMS)
    for (_ <- 0 until Simulations.VMS) { //Uses a CloudletSchedulerTimeShared by default to schedule Cloudlets
      val vm = new VmSimple(Simulations.HOST_MIPS, Simulations.VM_PES)
      vm.setRam(512).setBw(1000).setSize(10_000).setCloudletScheduler(new CloudletSchedulerSpaceShared)
      vmList.add(vm)
    }
    broker.submitVmList(vmList)
    vmList
  }

  /**
   * Creates a list of Cloudlets.
   */
  private def createAndSubmitCloudlets(broker: DatacenterBroker) = {
    val cloudletList = new util.ArrayList[Cloudlet](Simulations.CLOUDLETS)
    val utilizationModelFull = new UtilizationModelFull
//    /* A utilization model for RAM and BW that uses only 50% of the resource capacity all the time. */
//    val utilizationModelDynamic = new UtilizationModelDynamic(0.5)
    //UtilizationModel defining the Cloudlets use only 50% of any resource all the time
    val utilizationModel = new UtilizationModelDynamic(0.5)
    for (i <- 0 until Simulations.CLOUDLETS) {
      val cloudlet = new CloudletSimple(Simulations.CLOUDLET_LENGTH, Simulations.CLOUDLET_PES, utilizationModel)
      cloudlet.setSizes(1024)
        .setFileSize(1024)
        .setOutputSize(1024)
        .setUtilizationModelCpu(utilizationModelFull)
        .setUtilizationModelRam(utilizationModel)
        .setUtilizationModelBw(utilizationModel)
      cloudletList.add(cloudlet)
    }
    broker.submitCloudletList(cloudletList)
    cloudletList
  }

  /**
   * Computes and print the cost ($) of resources (processing, bw, memory, storage)
   * for each VM inside the datacenter.
   */
  private def printTotalVmsCost(): Unit = {
    System.out.println()
    var totalCost: Double = 0
    var totalNonIdleVms: Double = 0
    var processingTotalCost: Double = 0
    var memoryTotalCost: Double = 0
    var storageTotalCost: Double = 0
    var bwTotalCost: Double = 0


    for (vm: Vm <- brokerList.get(0).getVmCreatedList: java.util.List[Vm]) {
      val cost = new VmCost(vm)
      processingTotalCost += cost.getProcessingCost
      memoryTotalCost += cost.getMemoryCost
      storageTotalCost += cost.getStorageCost
      bwTotalCost += cost.getBwCost
      totalCost += cost.getTotalCost

//      if (vm.getTotalExecutionTime > 0) totalNonIdleVms +=1
//      else totalNonIdleVms += 0
      System.out.println(cost)
    }
//    System.out.printf("Total cost ($): %8.2f$ %13.2f$ %17.2f$ %12.2f$ %15.2f$%n",  processingTotalCost, memoryTotalCost, storageTotalCost, bwTotalCost, totalCost)
  }
}
