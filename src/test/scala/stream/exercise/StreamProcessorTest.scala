package stream.exercise

import munit.CatsEffectSuite
import fs2.Stream
import cats.effect.IO
import stream.{BatteryData, ChargingData, StreamProcessor}
import java.time.Instant

class StreamProcessorTest extends CatsEffectSuite {

  test("should combine charging data with latest battery state") {
    val chargingData = Stream(
      ChargingData(Instant.parse("2023-11-02T17:08:49Z"), "s123", "v123", 60)
    ).covary[IO]

    val batteryData = Stream(
      BatteryData(Instant.parse("2023-11-02T17:08:48Z"), "v123", 51)
    ).covary[IO]

    val result = StreamProcessor.combineStreams(chargingData, batteryData)
      .compile
      .toList

    result.map { outputs =>
      assertEquals(outputs.length, 1)
      assertEquals(outputs.head.vehicleId, "v123")
      assertEquals(outputs.head.socketId, "s123")
      assertEquals(outputs.head.powerInWatts, 60)
      assertEquals(outputs.head.stateOfChargeInPercent, 51)
    }
  }

  test("should only emit output when charging data arrives") {
    val chargingData = Stream(
      ChargingData(Instant.parse("2023-11-02T17:08:50Z"), "s123", "v123", 60)
    ).covary[IO]

    val batteryData = Stream(
      BatteryData(Instant.parse("2023-11-02T17:08:48Z"), "v123", 50),
      BatteryData(Instant.parse("2023-11-02T17:08:49Z"), "v123", 51),
      BatteryData(Instant.parse("2023-11-02T17:08:51Z"), "v123", 52)
    ).covary[IO]

    val result = StreamProcessor.combineStreams(chargingData, batteryData)
      .compile
      .toList

    result.map { outputs =>
      // Should emit exactly 1 output (only for the 1 charging event)
      assertEquals(outputs.length, 1)
    }
  }

  test("should use latest battery state for each vehicle") {
    val chargingData = Stream(
      ChargingData(Instant.parse("2023-11-02T17:08:50Z"), "s123", "v123", 60)
    ).covary[IO]

    val batteryData = Stream(
      BatteryData(Instant.parse("2023-11-02T17:08:48Z"), "v123", 50),
      BatteryData(Instant.parse("2023-11-02T17:08:49Z"), "v123", 75)
    ).covary[IO]

    val result = StreamProcessor.combineStreams(chargingData, batteryData)
      .compile
      .toList

    result.map { outputs =>
      // Should use the latest battery state (75, not 50)
      assertEquals(outputs.head.stateOfChargeInPercent, 75)
    }
  }

  test("should handle multiple vehicles independently") {
    val chargingData = Stream(
      ChargingData(Instant.parse("2023-11-02T17:08:50Z"), "s123", "v123", 60),
      ChargingData(Instant.parse("2023-11-02T17:08:51Z"), "s124", "v456", 75)
    )

    val batteryData = Stream(
      BatteryData(Instant.parse("2023-11-02T17:08:49Z"), "v123", 51),
      BatteryData(Instant.parse("2023-11-02T17:08:49Z"), "v456", 80)
    )

    val result = StreamProcessor.combineStreams(chargingData, batteryData)
      .compile
      .toList

    result.map { outputs =>
      assertEquals(outputs.length, 2)

      val v123Output = outputs.find(_.vehicleId == "v123").get
      assertEquals(v123Output.stateOfChargeInPercent, 51)
      assertEquals(v123Output.powerInWatts, 60)

      val v456Output = outputs.find(_.vehicleId == "v456").get
      assertEquals(v456Output.stateOfChargeInPercent, 80)
      assertEquals(v456Output.powerInWatts, 75)
    }
  }

  test("should use 0 as default when no battery data exists for vehicle") {
    val chargingData = Stream(
      ChargingData(Instant.parse("2023-11-02T17:08:50Z"), "s123", "v999", 60)
    ).covary[IO]

    val batteryData = Stream(
      BatteryData(Instant.parse("2023-11-02T17:08:49Z"), "v123", 51)
    ).covary[IO]

    val result = StreamProcessor.combineStreams(chargingData, batteryData)
      .compile
      .toList

    result.map { outputs =>
      assertEquals(outputs.head.vehicleId, "v999")
      assertEquals(outputs.head.stateOfChargeInPercent, 0)
    }
  }

  test("should handle multiple charging events for same vehicle") {
    val chargingData = Stream(
      ChargingData(Instant.parse("2023-11-02T17:08:50Z"), "s123", "v123", 60),
      ChargingData(Instant.parse("2023-11-02T17:08:52Z"), "s123", "v123", 65),
      ChargingData(Instant.parse("2023-11-02T17:08:54Z"), "s123", "v123", 70)
    )

    val batteryData = Stream(
      BatteryData(Instant.parse("2023-11-02T17:08:49Z"), "v123", 50),
      BatteryData(Instant.parse("2023-11-02T17:08:51Z"), "v123", 55),
      BatteryData(Instant.parse("2023-11-02T17:08:53Z"), "v123", 60)
    )

    val result = StreamProcessor.combineStreams(chargingData, batteryData)
      .compile
      .toList

    result.map { outputs =>
      assertEquals(outputs.length, 3)
      assertEquals(outputs(0).stateOfChargeInPercent, 50)
      assertEquals(outputs(1).stateOfChargeInPercent, 55)
      assertEquals(outputs(2).stateOfChargeInPercent, 60)
    }
  }
}