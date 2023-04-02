package com.knoldus.repository

import cats.effect.unsafe.implicits.global
import com.knoldus.dbTest.TestEmbeddedPostgres
import com.knoldus.model.{MonthlyIndividualWithStudioScore, MonthlyStudioScore}
import doobie.implicits._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class MonthlyStudioScoreRepositoryTest extends AsyncFlatSpec with Matchers with TestEmbeddedPostgres {

  val monthlyStudioScore: MonthlyStudioScore = MonthlyStudioScore(1, 1.1, 1, 2022)
  val monthlyIndividualWithStudioScore: MonthlyIndividualWithStudioScore = MonthlyIndividualWithStudioScore("bhavya@knoldus.com", 1, 1.1, 1, 2022)
  val executorService: ExecutorService = Executors.newFixedThreadPool(5)
  implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(executorService)
  lazy val monthlyStudioScoreRepository = new MonthlyStudioScoreRepositoryImpl(currentDb.xa)
  implicit val tenantId: Int = 1

  it should "get StudioMonthlyScore" in {
    val scores = for {
      _ <- monthlyStudioScoreRepository.store(monthlyStudioScore)
      monthlyStudioScore <- monthlyStudioScoreRepository.getStudioMonthlyScore(1, 1, 2022).transact(currentDb.xa).unsafeToFuture()
    } yield monthlyStudioScore
    scores.map {
      result => assertResult(Some(monthlyStudioScore))(result)
    }
  }

  it should "Store monthlyIndividualScore " in {
    monthlyStudioScoreRepository.store(monthlyStudioScore).map {
      result => assertResult(1)(result)
    }
  }

  it should "get allStudioMonthlyScores" in {
    val scores = for {
      _ <- monthlyStudioScoreRepository.store(monthlyStudioScore)
      allMonthlyStudioScore <- monthlyStudioScoreRepository.allStudioMonthlyScores(1, 2022).unsafeToFuture()
    } yield allMonthlyStudioScore
    scores.map {
      result => assertResult(List(monthlyStudioScore))(result)
    }
  }

  it should "insert monthlyStudioScore" in {
    monthlyStudioScoreRepository.insert(monthlyStudioScore).transact(currentDb.xa).unsafeToFuture().map {
      result => assertResult(1)(result)
    }
  }


  it should "update monthlyStudioScore" in {
    val scores = for {
      _ <- monthlyStudioScoreRepository.store(monthlyStudioScore)
      monthlyStudioScore <- monthlyStudioScoreRepository.update(monthlyStudioScore).transact(currentDb.xa).unsafeToFuture()
    } yield monthlyStudioScore
    scores.map {
      result => assertResult(1)(result)
    }
  }
  it should "safely insert monthlyStudioScore" in {
    val scores = for {
      _ <- monthlyStudioScoreRepository.update(monthlyStudioScore).transact(currentDb.xa).unsafeToFuture()
      monthlyStudioScore <- monthlyStudioScoreRepository.safeInsert(monthlyStudioScore).transact(currentDb.xa).unsafeToFuture()
    } yield monthlyStudioScore
    scores.map {
      result => assertResult(1)(result)
    }
  }
}
