package com.knoldus.dbTest

import cats.effect.IO
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import com.knoldus.common.DatabaseConfig
import com.typesafe.scalalogging.StrictLogging
import doobie.hikari.HikariTransactor
import doobie.implicits.{toSqlInterpolator, _}
import doobie.util.transactor.Transactor
import org.flywaydb.core.Flyway

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/** A work-around to use the `xaResource` imperatively.
  */
class TestDB(config: DatabaseConfig) extends StrictLogging {

  var xa: Transactor[IO] = _
  private val xaReady: Queue[IO, Transactor[IO]] = Queue.unbounded[IO, Transactor[IO]].unsafeRunSync()
  private val done: Queue[IO, Unit] = Queue.unbounded[IO, Unit].unsafeRunSync()

  {
    val xaResource = for {
      connectEC <- doobie.util.ExecutionContexts.fixedThreadPool[IO](10)
      xa <- HikariTransactor.newHikariTransactor[IO](
        config.driver,
        config.url,
        config.user,
        config.password,
        connectEC
      )
    } yield xa

    // first extracting it from the use method, then stopping when the `done` mvar is filled (when `close()` is invoked)
    xaResource
      .use { _xa =>
        xaReady.offer(_xa) >> done.take
      }
      .start
      .unsafeRunSync()

    xa = xaReady.take.unsafeRunSync()
  }

  private val flyway = {
    Flyway
      .configure()
      .dataSource(config.url, config.user, config.password)
      .load()
  }

  @tailrec
  final def connectAndMigrate(): Unit = {
    try {
      migrate()
      testConnection()
      logger.info("Database migration & connection test complete")
    } catch {
      case e: Exception =>
        logger.warn("Database not available, waiting 5 seconds to retry...", e)
        Thread.sleep(5000)
        connectAndMigrate()
    }
  }

  def migrate(): Unit = {
    if (true) {
      flyway.migrate()
      Thread.sleep(5000)
      ()
    }
  }

  def clean(): Unit = {
    flyway.clean()
  }

  def testConnection(): Unit = {
    sql"select 1".query[Int].unique.transact(xa).unsafeToFuture()
    ()
  }

  def truncateAllTables(): Unit = {
    logger.info("In Truncate Tables ")
    val result = sql"""TRUNCATE contribution_score, monthly_individual_score, monthly_studio_score,
          all_time_individual_score, all_time_studio_score,
           dynamic_scoring CASCADE """.update.run.transact(xa).unsafeToFuture()
    Await.result(result, new DurationInt(3).seconds)
    ()
  }


  def close(): Unit = {
    done.offer(()).unsafeRunTimed(1.minute)
  }
}
