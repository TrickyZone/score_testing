package com.knoldus.repository

import cats.effect.unsafe.implicits.global
import com.knoldus.dbTest.TestEmbeddedPostgres
import com.knoldus.model.AllTimeStudioScore
import doobie.implicits._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class AllTimeStudioScoreRepositoryTest extends AsyncFlatSpec with Matchers with TestEmbeddedPostgres {

  val executorService: ExecutorService = Executors.newFixedThreadPool(5)
  implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(executorService)
  lazy val allTimeStudioScoreRepository = new AllTimeStudioScoreRepositoryImpl(currentDb.xa)
  val allTimeStudioScore: AllTimeStudioScore = AllTimeStudioScore(1, 1.1)
  implicit val tenantId: Int = 1

  it should "store allTimeStudioScore" in {
    allTimeStudioScoreRepository.store(allTimeStudioScore).map {
      result => assertResult(1)(result)
    }
  }

  it should "get allStudioAllTimeScores" in {
    val scores = for {
      _ <- allTimeStudioScoreRepository.store(allTimeStudioScore)
      allTimeStudioScores <- allTimeStudioScoreRepository.allStudioAllTimeScores.unsafeToFuture()
    } yield allTimeStudioScores
    scores.map {
      result => assertResult(List(allTimeStudioScore))(result)
    }
  }

  it should "get AllTimeStudioScore" in {
    val scores = for {
      _ <- allTimeStudioScoreRepository.store(allTimeStudioScore)
      allTimeStudioScores <- allTimeStudioScoreRepository.getAllTimeStudioScore(1).transact(currentDb.xa).unsafeToFuture()
    } yield allTimeStudioScores
    scores.map {
      result => assertResult(Some(allTimeStudioScore))(result)
    }
  }

  it should "insert AllTimeStudioScore" in {
    allTimeStudioScoreRepository.insert(allTimeStudioScore).transact(currentDb.xa).unsafeToFuture().map {
      result => assertResult(1)(result)
    }
  }

  it should "update AllTimeStudioScore" in {
    val scores = for {
      _ <- allTimeStudioScoreRepository.store(allTimeStudioScore)
      allTimeStudioScores <- allTimeStudioScoreRepository.update(allTimeStudioScore).transact(currentDb.xa).unsafeToFuture()
    } yield allTimeStudioScores
    scores.map {
      result => assertResult(1)(result)
    }
  }

  it should "safely update AllTimeStudioScore" in {
    val scores = for {
      _ <- allTimeStudioScoreRepository.store(allTimeStudioScore)
      allTimeStudioScores <- allTimeStudioScoreRepository.safeInsert(allTimeStudioScore).transact(currentDb.xa).unsafeToFuture()
    } yield allTimeStudioScores
    scores.map {
      result => assertResult(1)(result)
    }
  }

  it should "safely insert AllTimeStudioScore" in {
    allTimeStudioScoreRepository.safeInsert(allTimeStudioScore).transact(currentDb.xa).unsafeToFuture().map {
      result => assertResult(1)(result)
    }
  }
}