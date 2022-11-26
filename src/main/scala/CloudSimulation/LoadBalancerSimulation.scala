//package CloudSimulation
//
//import org.cloudbus.cloudsim.brokers.DatacenterBroker
//import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
//import org.cloudbus.cloudsim.cloudlets.Cloudlet
//import org.cloudbus.cloudsim.cloudlets.CloudletSimple
//import org.cloudbus.cloudsim.core.CloudSim
//import org.cloudbus.cloudsim.core.Simulation
//import org.cloudbus.cloudsim.datacenters.Datacenter
//import org.cloudbus.cloudsim.datacenters.DatacenterSimple
//import org.cloudbus.cloudsim.distributions.ContinuousDistribution
//import org.cloudbus.cloudsim.distributions.UniformDistr
//import org.cloudbus.cloudsim.hosts.Host
//import org.cloudbus.cloudsim.hosts.HostSimple
//import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple
//import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple
//import org.cloudbus.cloudsim.resources.Pe
//import org.cloudbus.cloudsim.resources.PeSimple
//import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared
//import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared
//import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel
//import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic
//import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull
//import org.cloudbus.cloudsim.vms.Vm
//import org.cloudbus.cloudsim.vms.VmSimple
//import org.cloudsimplus.autoscaling.HorizontalVmScaling
//import org.cloudsimplus.autoscaling.HorizontalVmScalingSimple
//import org.cloudsimplus.builders.tables.CloudletsTableBuilder
//import org.cloudsimplus.listeners.EventInfo
//import org.cloudsimplus.listeners.EventListener
//
//import java.util._
//import java.util.Comparator
//import java.util.function.Function
//import java.util.function.Predicate
//import java.util.Comparator.comparingDouble
//
//
//object LoadBalancerSimulation {
//  /**
//   * The interval in which the Datacenter will schedule events.
//   * As lower is this interval, sooner the processing of VMs and Cloudlets
//   * is updated and you will get more notifications about the simulation execution.
//   * However, that also affect the simulation performance.
//   *
//   * <p>A large schedule interval, such as 15, will make that just
//   * at every 15 seconds the processing of VMs is updated. If a VM is overloaded, just
//   * after this time the creation of a new one will be requested
//   * by the VM's {@link HorizontalVmScaling Horizontal Scaling} mechanism.</p>
//   *
//   * <p>If this interval is defined using a small value, you may get
//   * more dynamically created VMs than expected.
//   * Accordingly, this value has to be trade-off.
//   * For more details, see {@link Datacenter# getSchedulingInterval ( )}.</p>
//   */
//  private val SCHEDULING_INTERVAL = 5
//  /**
//   * The interval to request the creation of new Cloudlets.
//   */
//  private val CLOUDLETS_CREATION_INTERVAL = SCHEDULING_INTERVAL * 2
//  private val HOSTS = 50
//  private val HOST_PES = 32
//  private val VMS = 4
//  private val CLOUDLETS = 6
//  /**
//   * Different lengths that will be randomly assigned to created Cloudlets.
//   */
//  private val CLOUDLET_LENGTHS = Array(2000, 4000, 10000, 16000, 2000, 30000, 20000)
//
//  def main(args: Array[String]): Unit = {
//    new LoadBalancerSimulation
//  }
//}
//
//class LoadBalancerSimulation private()
//{
//
//  val seed: Long = 1
//  val rand = new UniformDistr(0, LoadBalancerSimulation.CLOUDLET_LENGTHS.length, seed)
//  val hostList = new ArrayList[Host](LoadBalancerSimulation.HOSTS)
//  val vmList = new ArrayList[Vm](LoadBalancerSimulation.VMS)
//  val cloudletList = new ArrayList[Cloudlet](LoadBalancerSimulation.CLOUDLETS)
//  val simulation = new CloudSim
//  simulation.addOnClockTickListener(this.createNewCloudlets)
//  val dc0 = createDatacenter
//  val broker0 = new DatacenterBrokerSimple(simulation)
//
//  /**
//   * Defines the Vm Destruction Delay Function as a lambda expression
//   * so that the broker will wait 10 seconds before destroying any idle VM.
//   * By commenting this line, no down scaling will be performed
//   * and idle VMs will be destroyed just after all running Cloudlets
//   * are finished and there is no waiting Cloudlet.
//   *
//   * @see DatacenterBroker#setVmDestructionDelayFunction(Function)
//   * */
//  broker0.setVmDestructionDelay(10.0)
//  vmList.addAll(createListOfScalableVms(LoadBalancerSimulation.VMS))
//  createCloudletList()
//  broker0.submitVmList(vmList)
//  broker0.submitCloudletList(cloudletList)
//  simulation.start
//  printSimulationResults()
//
//  private var createdCloudlets: Int = 0
//  private var createsVms: Int = 0
//
//  private def printSimulationResults(): Unit = {
//    val finishedCloudlets: List[Cloudlet] = broker0.getCloudletFinishedList
//    val sortByVmId: Comparator[Cloudlet] = comparingDouble((c: Cloudlet) => c.getVm.getId)
//    val sortByStartTime: Comparator[Cloudlet] = comparingDouble(Cloudlet.getExecStartTime)
//    finishedCloudlets.sort(sortByVmId.thenComparing(sortByStartTime))
//    new CloudletsTableBuilder(finishedCloudlets).build()
//}
//
//  private def createCloudletList(): Unit = {
//  for (i <- 0 until LoadBalancerSimulation.CLOUDLETS) {
//  cloudletList.add(createCloudlet)
//}
//}
//
//  /**
//   * Creates new Cloudlets at every {@link # CLOUDLETS_CREATION_INTERVAL} seconds, up to the 50th simulation second.
//   * A reference to this method is set as the {@link EventListener}
//   * to the {@link Simulation# addOnClockTickListener ( EventListener )}.
//   * The method is called every time the simulation clock advances.
//   *
//   * @param info the information about the OnClockTick event that has happened
//   */
//  private def createNewCloudlets(info: EventInfo): Unit = {
//  val time: Long = info.getTime.toLong
//  if (time % LoadBalancerSimulation.CLOUDLETS_CREATION_INTERVAL == 0 && time <= 50) {
//  val cloudletsNumber: Int = 4
//  System.out.printf("\t#Creating %d Cloudlets at time %d.%n", cloudletsNumber, time)
//  val newCloudlets: List[Cloudlet] = new ArrayList[Cloudlet](cloudletsNumber)
//  for (i <- 0 until cloudletsNumber) {
//  val cloudlet: Cloudlet = createCloudlet
//  cloudletList.add(cloudlet)
//  newCloudlets.add(cloudlet)
//}
//  broker0.submitCloudletList(newCloudlets)
//}
//}
//
//  /**
//   * Creates a Datacenter and its Hosts.
//   */
//  private def createDatacenter: Datacenter = {
//  for (i <- 0 until LoadBalancerSimulation.HOSTS) {
//  hostList.add(createHost)
//}
//  return new DatacenterSimple(simulation, hostList).setSchedulingInterval(LoadBalancerSimulation.SCHEDULING_INTERVAL)
//}
//
//  private def createHost: Host = {
//  val peList: List[Pe] = new ArrayList[Pe](LoadBalancerSimulation.HOST_PES)
//  for (i <- 0 until LoadBalancerSimulation.HOST_PES) {
//  peList.add(new PeSimple(1000, new PeProvisionerSimple))
//}
//  val ram: Long = 2048 // in Megabytes
//  val storage: Long = 1000000
//  val bw: Long = 10000 //in Megabits/s
//  return new HostSimple(ram, bw, storage, peList).setRamProvisioner(new ResourceProvisionerSimple).setBwProvisioner(new ResourceProvisionerSimple).setVmScheduler(new VmSchedulerTimeShared)
//}
//
//  /**
//   * Creates a list of initial VMs in which each VM is able to scale horizontally
//   * when it is overloaded.
//   *
//   * @param vmsNumber number of VMs to create
//   * @return the list of scalable VMs
//   * @see #createHorizontalVmScaling(Vm)
//   */
//  private def createListOfScalableVms(vmsNumber: Int): List[Vm] = {
//  val newList: List[Vm] = new ArrayList[Vm](vmsNumber)
//  for (i <- 0 until vmsNumber) {
//  val vm: Vm = createVm
//  createHorizontalVmScaling(vm)
//  newList.add(vm)
//}
//  return newList
//}
//
//  /**
//   * Creates a {@link HorizontalVmScaling} object for a given VM.
//   *
//   * @param vm the VM for which the Horizontal Scaling will be created
//   * @see #createListOfScalableVms(int)
//   */
//  private def createHorizontalVmScaling(vm: Vm): Unit = {
//  val horizontalScaling: HorizontalVmScaling = new HorizontalVmScalingSimple
//  horizontalScaling.setVmSupplier(this.createVm).setOverloadPredicate(this.isVmOverloaded)
//  vm.setHorizontalScaling(horizontalScaling)
//}
//
//  /**
//   * A {@link Predicate} that checks if a given VM is overloaded or not,
//   * based on upper CPU utilization threshold.
//   * A reference to this method is assigned to each {@link HorizontalVmScaling} created.
//   *
//   * @param vm the VM to check if it is overloaded
//   * @return true if the VM is overloaded, false otherwise
//   * @see #createHorizontalVmScaling(Vm)
//   */
//  private def isVmOverloaded(vm: Vm): Boolean = {
//  return vm.getCpuPercentUtilization > 0.7
//}
//
//  /**
//   * Creates a Vm object.
//   *
//   * @return the created Vm
//   */
//  private def createVm: Vm = {
//  val id: Int = {
//  createsVms += 1; createsVms - 1
//}
//  return new VmSimple(id, 1000, 2).setRam(512).setBw(1000).setSize(10000).setCloudletScheduler(new CloudletSchedulerTimeShared)
//}
//
//  private def createCloudlet: Cloudlet = {
//  val id: Int = {
//    createdCloudlets += 1; createdCloudlets - 1
//  }
//  val utilizadionModelDynamic: UtilizationModelDynamic = new UtilizationModelDynamic(0.1)
//  //randomly selects a length for the cloudlet
//  val length: Long = LoadBalancerSimulation.CLOUDLET_LENGTHS(rand.sample.toInt)
//  return new CloudletSimple(id, length, 2).setFileSize(1024).setOutputSize(1024).setUtilizationModelBw(utilizadionModelDynamic).setUtilizationModelRam(utilizadionModelDynamic).setUtilizationModelCpu(new UtilizationModelFull)
//}
//}
