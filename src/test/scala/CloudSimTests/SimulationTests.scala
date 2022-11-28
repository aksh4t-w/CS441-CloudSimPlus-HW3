package CloudSimTests

import com.typesafe.config.{Config, ConfigFactory}
import HelperUtils.CommonUtils.{createDatacenter, _}
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.DatacenterSimple
import org.cloudbus.cloudsim.hosts.Host
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util

class SimulationTests extends AnyFlatSpec with Matchers {

  val config = ConfigFactory.load("application.conf")
  val simulation = new CloudSim
  val hostList = new util.ArrayList[Host]

  behavior of "Datacenter creation type"


  it should "Check the created datacenter type" in {
    val dc = createDatacenter(config, simulation, hostList)
    dc shouldBe a [DatacenterSimple]
  }

  it should "Check the created host's type in the datacenter" in {
    val id = 1
    val host = createHost(config, id)
    host shouldBe a [Host]
  }
}

