package com.knoldus.repository

import cats.effect.unsafe.implicits.global
import com.knoldus.dbTest.TestEmbeddedPostgres
import com.knoldus.model.{AllTimeIndividualScore, AllTimeIndividualWithStudioScore}
import doobie.implicits._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class AllTimeIndividualScoreRepositoryTest extends AsyncFlatSpec with Matchers with TestEmbeddedPostgres {

  val executorService: ExecutorService = Executors.newFixedThreadPool(5)
  implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(executorService)
  lazy val allTimeIndividualScoreRepository = new AllTimeIndividualScoreRepositoryImpl(currentDb.xa)
  val allTimeIndividualWithStudioScore: AllTimeIndividualWithStudioScore = AllTimeIndividualWithStudioScore("bhavya@knoldus.com", 1, 1.1)
  val allTimeIndividualScore: AllTimeIndividualScore = AllTimeIndividualScore("bhavya@knoldus.com", 1.1)
  implicit val tenantId: Int = 1

  it should "getAllTimeIndividualScore" in {
    val scores = for {
      _ <- allTimeIndividualScoreRepository.store(allTimeIndividualWithStudioScore)
      allTimeIndividualScore <- allTimeIndividualScoreRepository.getAllTimeIndividualScore("bhavya@knoldus.com", 1).transact(currentDb.xa).unsafeToFuture
    } yield allTimeIndividualScore
    scores.map {
      result => assertResult(Some(allTimeIndividualWithStudioScore))(result)
    }
  }

  it should "Store AllTimeIndividualScore " in {
    allTimeIndividualScoreRepository.store(allTimeIndividualWithStudioScore).map {
      result => assertResult(1)(result)
    }
  }

  it should "get AllIndividualAllTimeScores" in {
    val scores = for {
      _ <- allTimeIndividualScoreRepository.store(allTimeIndividualWithStudioScore)
      allTimeIndividualScore <- allTimeIndividualScoreRepository.allIndividualAllTimeScores.unsafeToFuture()
    } yield allTimeIndividualScore
    scores.map {
      result => assertResult(List(allTimeIndividualScore))(result)
    }
  }

  it should "insert allTimeIndividualScore" in {
    allTimeIndividualScoreRepository.insert(allTimeIndividualWithStudioScore).transact(currentDb.xa).unsafeToFuture().map {
      result => assertResult(1)(result)
    }
  }

  it should "update allTimeIndividualScore" in {
    val scores = for {
      _ <- allTimeIndividualScoreRepository.store(allTimeIndividualWithStudioScore)
      allTimeIndividualScore <- allTimeIndividualScoreRepository.update(allTimeIndividualWithStudioScore).transact(currentDb.xa).unsafeToFuture()
    } yield allTimeIndividualScore
    scores.map {
      result => assertResult(1)(result)
    }
  }

  it should "safely update AllTimeIndividualScore" in {
    val scores = for {
      _ <- allTimeIndividualScoreRepository.store(allTimeIndividualWithStudioScore)
      allTimeIndividualScore <- allTimeIndividualScoreRepository.safeInsert(allTimeIndividualWithStudioScore).transact(currentDb.xa).unsafeToFuture()
    } yield allTimeIndividualScore
    scores.map {
      result => assertResult(1)(result)
    }
  }

  it should "safely insert AllTimeIndividualScore" in {
    allTimeIndividualScoreRepository.safeInsert(allTimeIndividualWithStudioScore).transact(currentDb.xa).unsafeToFuture().map {
      result => assertResult(1)(result)
    }
  }
}