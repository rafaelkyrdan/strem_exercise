package stream

import cats.effect.{IO, IOApp, Ref}
import fs2.Stream
import java.time.Instant

case class ChargingData(
                         timestamp: Instant,
                         socketId: String,
                         vehicleId: String,
                         powerInWatts: Int
                       )

case class BatteryData(
                        timestamp: Instant,
                        vehicleId: String,
                        stateOfChargeInPercent: Int
                      )


case class CombinedData(
                         timestamp: Instant,
                         socketId: String,
                         vehicleId: String,
                         powerInWatts: Int,
                         stateOfChargeInPercent: Int
                       )

object StreamProcessor extends IOApp.Simple {

  sealed trait Event {
    def timestamp: Instant
  }
  case class ChargingEvent(data: ChargingData) extends Event {
    def timestamp: Instant = data.timestamp
  }
  case class BatteryEvent(data: BatteryData) extends Event {
    def timestamp: Instant = data.timestamp
  }

  def mergeByTimestamp(
                        s1: Stream[IO, Event],
                        s2: Stream[IO, Event]
                      ): Stream[IO, Event] = {

    def go(
            s1: Stream[IO, Event],
            s2: Stream[IO, Event]
          ): fs2.Pull[IO, Event, Unit] = {
      s1.pull.uncons1.flatMap {
        case None => s2.pull.echo
        case Some((e1, tail1)) =>
          s2.pull.uncons1.flatMap {
            case None => fs2.Pull.output1(e1) >> tail1.pull.echo
            case Some((e2, tail2)) =>
              if (e1.timestamp.toEpochMilli <= e2.timestamp.toEpochMilli) {
                fs2.Pull.output1(e1) >> go(tail1, Stream.emit(e2) ++ tail2)
              } else {
                fs2.Pull.output1(e2) >> go(Stream.emit(e1) ++ tail1, tail2)
              }
          }
      }
    }

    go(s1, s2).stream
  }

  // Main processing logic - assumes streams are already sorted by timestamp
  def combineStreams(
                      chargingStream: Stream[IO, ChargingData],
                      batteryStream: Stream[IO, BatteryData]
                    ): Stream[IO, CombinedData] = {

    Stream.eval(Ref.of[IO, Map[String, Int]](Map.empty)).flatMap { stateRef =>

      val chargingTagged = chargingStream.map(Left(_))
      val batteryTagged = batteryStream.map(Right(_))


      chargingTagged.merge(batteryTagged).evalMap {
        case Left(charging) =>
          // Charging event: read state and emit output
          stateRef.get.map { batteryState =>
            val stateOfCharge = batteryState.getOrElse(charging.vehicleId, 0)
            Some(CombinedData(
              timestamp = charging.timestamp,
              socketId = charging.socketId,
              vehicleId = charging.vehicleId,
              powerInWatts = charging.powerInWatts,
              stateOfChargeInPercent = stateOfCharge
            ))
          }

        case Right(battery) =>

          stateRef.update(state =>
            state.updated(battery.vehicleId, battery.stateOfChargeInPercent)
          ).as(None)
      }.collect { case Some(output) => output }
    }
  }


  def run: IO[Unit] = {

    val chargingData = Stream(
      ChargingData(Instant.parse("2023-11-02T17:08:49Z"), "s123", "v123", 60),
      ChargingData(Instant.parse("2023-11-02T17:08:50Z"), "s124", "v124", 75),
      ChargingData(Instant.parse("2023-11-02T17:08:51Z"), "s123", "v123", 62)
    ).covary[IO]


    val batteryData = Stream(
      BatteryData(Instant.parse("2023-11-02T17:08:48Z"), "v123", 50),
      BatteryData(Instant.parse("2023-11-02T17:08:49Z"), "v123", 51),
      BatteryData(Instant.parse("2023-11-02T17:08:49Z"), "v124", 80)
    ).covary[IO]


    combineStreams(chargingData, batteryData)
      .evalMap(output => IO.println(output))
      .compile
      .drain
  }
}