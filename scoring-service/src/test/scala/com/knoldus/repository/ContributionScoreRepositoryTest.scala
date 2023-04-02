package com.knoldus.repository

import cats.effect.unsafe.implicits.global
import com.knoldus.dbTest.TestEmbeddedPostgres
import com.knoldus.model._
import doobie.implicits._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class ContributionScoreRepositoryTest extends AsyncFlatSpec with Matchers with TestEmbeddedPostgres {

  val executorService: ExecutorService = Executors.newFixedThreadPool(5)
  implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(executorService)
  lazy val contributionScoreRepository = new ContributionScoreRepositoryImpl(currentDb.xa)
  val contributionScore: ContributionScore = ContributionScore(Some(1), Some("1"), Some("bhavya"), "bhavya@knoldus.com", ContributionType.BLOG,
    "Scala", "2022-06-02 09:45:15.000", Some("akka"), Some("www.scala.com"), 8, Some("Scala Studio"), 5, "561BD57BB85EDB12AB")
  val monthlyIndividualScore: MonthlyIndividualScore = MonthlyIndividualScore("bhavya@knoldus.com", 5, 6, 2022)
  val monthlyStudioScore: MonthlyStudioScore = MonthlyStudioScore(8, 5.0, 6, 2022)
  val allTimeStudioScore: AllTimeStudioScore = AllTimeStudioScore(7, 1.1)
  val individualMonthlyContributionTypeScore: IndividualMonthlyContributionTypeScore = IndividualMonthlyContributionTypeScore("bhavya@knoldus.com",6,2022,ContributionType.BLOG,5.0)
  val dailyScore: DailyScore = DailyScore("abc@gmail.com","abc",1.1)
  val individualContributionTypeScore: IndividualContributionTypeScore = IndividualContributionTypeScore("bhavya@knoldus.com",ContributionType.BLOG,5.0)
  val allTimeIndividualWithStudioScore: AllTimeIndividualWithStudioScore = AllTimeIndividualWithStudioScore("bhavya@knoldus.com",8, 5.0)
  val monthlyIndividualWithStudioScore: MonthlyIndividualWithStudioScore = MonthlyIndividualWithStudioScore("bhavya@knoldus.com",8, 5.0, 6, 2022)
  val allTimeIndividualScore: AllTimeIndividualScore = AllTimeIndividualScore("bhavya@knoldus.com",5.0)
  implicit val tenantId: Int = 1

  it should "store ContributionScore" in {
    contributionScoreRepository.store(contributionScore).map {
      result => assertResult(1)(result)
    }
  }

  it should "getMonthlyIndividualContributionScores" in {
    val scores = for {
      _ <- contributionScoreRepository.store(contributionScore)
      monthlyScore <- contributionScoreRepository.getMonthlyIndividualContributionScores(6, 2022).unsafeToFuture()
    } yield monthlyScore
    scores.map {
      result => assertResult(List(monthlyIndividualScore))(result)
    }
  }

  it should "getMonthlyStudioContributionScore" in {
    val scores = for {
      _ <- contributionScoreRepository.store(contributionScore)
      monthlyScore <- contributionScoreRepository.getMonthlyStudioContributionScore(8, 6, 2022).transact(currentDb.xa).unsafeToFuture()
    } yield monthlyScore
    scores.map {
      result => assertResult(Some(monthlyStudioScore))(result)
    }
  }
  it should " getAllTimeStudioContributionScore" in {
    val allTimeStudioScore = AllTimeStudioScore(8, 5.0)
    val scores = for {
      _ <- contributionScoreRepository.store(contributionScore)
      allTimeScore <- contributionScoreRepository.getAllTimeStudioContributionScore(8).transact(currentDb.xa).unsafeToFuture()
    } yield allTimeScore
    scores.map {
      result => assertResult(Some(allTimeStudioScore))(result)
    }
  }
  it should " getAllTimeIndividualContributionScores" in {
    val scores = for {
      _ <- contributionScoreRepository.store(contributionScore)
      individualScore <- contributionScoreRepository.getAllTimeIndividualContributionScores().unsafeToFuture()
    } yield individualScore
    scores.map {
      result => assertResult(List(allTimeIndividualScore))(result)
    }
  }
  it should "getAllTimeMonthlyContributions" in {
    val scores = for {
      _ <- contributionScoreRepository.store(contributionScore)
      individualMonthlyTypeScore <- contributionScoreRepository.getAllTimeMonthlyContributions().unsafeToFuture()
    } yield individualMonthlyTypeScore
    scores.map {
      result => assertResult(List(individualMonthlyContributionTypeScore))(result)
    }
  }
  it should "getContributionScoreForMd5Hash" in {
    val scores = for {
      _ <- contributionScoreRepository.store(contributionScore)
      md5HashScore <- contributionScoreRepository.getContributionScoreForMd5Hash("561BD57BB85EDB12AB",ContributionType.BLOG,"1","akash.kumar@knoldus.com").unsafeToFuture()
    } yield md5HashScore
    scores.map {
      result => assertResult(Some(7))(result)
    }
  }
  it should " getMonthlyIndividualContributionScore" in {
    val scores = for {
      _ <- contributionScoreRepository.store(contributionScore)
      individualContributionScore <- contributionScoreRepository.getMonthlyIndividualContributionScore("bhavya@knoldus.com",8,6,2022).transact(currentDb.xa).unsafeToFuture()
    } yield individualContributionScore
    scores.map {
      result => assertResult(Some(monthlyIndividualWithStudioScore))(result)
    }
  }
  it should " getAllTimeContributionScoreForIndividual" in {
    val scores = for {
      _ <- contributionScoreRepository.store(contributionScore)
      individualContributionScore <- contributionScoreRepository.getAllTimeContributionScoreForIndividual("bhavya@knoldus.com",8).transact(currentDb.xa).unsafeToFuture()
    } yield individualContributionScore
    scores.map {
      result => assertResult(Some(allTimeIndividualWithStudioScore))(result)
    }
  }
  it should "getMonthlyContributions" in {
    val scores = for {
      _ <- contributionScoreRepository.store(contributionScore)
      monthlyContributionScore <- contributionScoreRepository.getMonthlyContributions(6,2022).unsafeToFuture()
    } yield monthlyContributionScore
    scores.map {
      result => assertResult(List(individualContributionTypeScore))(result)
    }
  }
//  it should "get ContributionScore" in {
//    val scores = for {
//      _ <- contributionScoreRepository.store(contributionScore)
//      contriScore <- contributionScoreRepository.get(1).unsafeToFuture()
//    }yield contriScore
//        scores.map {
//      result => assertResult(Some(contributionScore))(result)
//    }
//  }
  it should "storeContributionScore" in {
       contributionScoreRepository.storeContributionScore(contributionScore).transact(currentDb.xa).unsafeToFuture().map{
      result => assertResult(1)(result)
    }
  }
//  it should "all ContributionScore" in {
//    val scores = for {
//      _ <- contributionScoreRepository.store(contributionScore)
//      contriScore <- contributionScoreRepository.all.unsafeToFuture()
//    } yield contriScore
//        scores.map{
//      result => assertResult(Vector(contributionScore))(result)
//    }
//  }
  it should "getAllIndividualContributionScores" in {
    val scores = for {
      _ <- contributionScoreRepository.store(contributionScore)
      individualScore <- contributionScoreRepository.getAllIndividualContributionScores.unsafeToFuture()
    } yield individualScore
    scores.map {
      result => assertResult(List(monthlyIndividualScore))(result)
    }
  }
}
