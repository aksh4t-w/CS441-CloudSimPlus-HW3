package CloudSimTests

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.typesafe.config.{Config, ConfigFactory}

class ConfigParamsTest extends AnyFlatSpec with Matchers {

  val config = ConfigFactory.load("application.conf")
  val DATACENTERS = config.getInt("SpaceShared.Datacenter.DATACENTERS")

  behavior of "configuration parameters module"

  it should "check that the number of Datacenters" in {
    config.getInt("SpaceShared.Datacenter.DATACENTERS") shouldBe 6
  }

  it should "check that the number of Hosts" in {
    config.getInt("SpaceShared.Datacenter.HOSTS") shouldBe 10
  }

  it should "check that the Scheduling Interval" in {
    config.getInt("SpaceShared.Datacenter.SCHEDULING_INTERVAL") shouldBe 10
  }

  it should "check that the number of Brokers" in {
    config.getInt("SpaceShared.BROKERS") shouldBe 3
  }

  it should "check that the cost of storage" in {
    config.getDouble("SpaceShared.Datacenter.COST_PER_STORAGE") shouldBe 0.0001
  }
}