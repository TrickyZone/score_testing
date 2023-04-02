package com.knoldus.repository

import cats.effect.unsafe.implicits.global
import com.knoldus.dbTest.TestEmbeddedPostgres
import com.knoldus.model.ScoreMultiplier
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class SpikeMonthScoreRepositoryTest extends AsyncFlatSpec with Matchers with TestEmbeddedPostgres {

  val executorService: ExecutorService = Executors.newFixedThreadPool(5)
  implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(executorService)
  lazy val spikeMonthScoreRepository = new SpikeMonthScoreRepositoryImpl(currentDb.xa)
  val scoreMultiplier: ScoreMultiplier = ScoreMultiplier(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 2, 2022)
  implicit val tenantId: Int = 1

  it should "Store Multiplier " in {
    spikeMonthScoreRepository.store(scoreMultiplier).map {
      result => assertResult(1)(result)
    }
  }

  it should "Read Multiplier " in {
    val scores = for {
      _ <- spikeMonthScoreRepository.store(scoreMultiplier)
      scoreMultiplier <- spikeMonthScoreRepository.getSpikeMonthMultipliers(2, 2022).unsafeToFuture()
    } yield scoreMultiplier
    scores.map {
      result => assertResult(Some(scoreMultiplier))(result)
    }
  }
}