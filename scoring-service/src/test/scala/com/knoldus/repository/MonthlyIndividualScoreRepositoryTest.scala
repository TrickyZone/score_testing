package com.knoldus.repository

import cats.effect.unsafe.implicits.global
import com.knoldus.dbTest.TestEmbeddedPostgres
import com.knoldus.model.MonthlyIndividualWithStudioScore
import doobie.implicits._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class MonthlyIndividualScoreRepositoryTest extends AsyncFlatSpec with Matchers with TestEmbeddedPostgres {

  val monthlyIndividualWithStudioScore: MonthlyIndividualWithStudioScore = MonthlyIndividualWithStudioScore("bhavya@knoldus.com", 1, 1.1, 1, 2022)
  val executorService: ExecutorService = Executors.newFixedThreadPool(5)
  implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(executorService)
  lazy val monthlyIndividualScoreRepository = new MonthlyIndividualScoreRepositoryImpl(currentDb.xa)
  implicit val tenantId: Int = 1

  it should "Store monthlyIndividualScore " in {
    monthlyIndividualScoreRepository.store(monthlyIndividualWithStudioScore).map {
      result => assertResult(1)(result)
    }
  }

  it should "get IndividualMonthlyScore" in {
    val scores = for {
      _ <- monthlyIndividualScoreRepository.store(monthlyIndividualWithStudioScore)
      monthlyIndividualScore <- monthlyIndividualScoreRepository.getIndividualMonthlyScore("bhavya@knoldus.com", 1, 1, 2022).transact(currentDb.xa).unsafeToFuture
    } yield monthlyIndividualScore
   scores.map{
     result => assertResult(Some(monthlyIndividualWithStudioScore))(result)
    }
  }

  it should "insert MonthlyIndividualWithStudioScore" in {
    val scores = for {
      monthlyIndividualScore <- monthlyIndividualScoreRepository.insert(monthlyIndividualWithStudioScore).transact(currentDb.xa).unsafeToFuture()
    } yield monthlyIndividualScore
     scores.map{
       result => assertResult(1)(result)
     }
  }

  it should "update MonthlyIndividualWithStudioScore" in {
    val scores = for {
      _ <- monthlyIndividualScoreRepository.store(monthlyIndividualWithStudioScore)
      monthlyIndividualScore <- monthlyIndividualScoreRepository.update(monthlyIndividualWithStudioScore).transact(currentDb.xa).unsafeToFuture()
    } yield monthlyIndividualScore
    scores.map{
      result => assertResult(1)(result)
    }
  }

  it should "safely update MonthlyIndividualWithStudioScore" in {
    val scores = for {
      _ <- monthlyIndividualScoreRepository.store(monthlyIndividualWithStudioScore)
      monthlyIndividualScore <- monthlyIndividualScoreRepository.safeInsert(monthlyIndividualWithStudioScore).transact(currentDb.xa).unsafeToFuture()
    } yield monthlyIndividualScore
    scores.map {
      result => assertResult(1)(result)
    }
  }

  it should "safely insert MonthlyIndividualWithStudioScore" in {
    monthlyIndividualScoreRepository.safeInsert(monthlyIndividualWithStudioScore).transact(currentDb.xa).unsafeToFuture().map {
      result => assertResult(1)(result)
    }
  }

  it should "get allIndividualMonthlyScores " in {
    val scores = for {
      _ <- monthlyIndividualScoreRepository.store(monthlyIndividualWithStudioScore)
      allIndividualMonthlyScore <- monthlyIndividualScoreRepository.allIndividualMonthlyScores(1, 2022).unsafeToFuture()
    } yield allIndividualMonthlyScore
        scores.map{
    result => assertResult(List(monthlyIndividualWithStudioScore))(result)
    }
  }
}
