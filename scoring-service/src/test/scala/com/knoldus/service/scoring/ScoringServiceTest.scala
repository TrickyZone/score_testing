package com.knoldus.service.scoring

import cats.effect.IO
import com.knoldus.QuickstartApp.{scoringConfig, xa}
import com.knoldus.dbTest.TestEmbeddedPostgres
import com.knoldus.model._
import com.knoldus.repository._
import org.mockito.MockitoSugar.{mock, when}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class ScoringServiceTest extends AsyncFlatSpec with Matchers with TestEmbeddedPostgres {


  val executorService: ExecutorService = Executors.newFixedThreadPool(5)
  implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(executorService)
  lazy val scoringService = new ScoringServiceImpl
  val mockspikeMonthScoreRepositoryImpl: SpikeMonthScoreRepositoryImpl = mock[SpikeMonthScoreRepositoryImpl]
  val mockcontributionScoreRepo: ContributionScoreRepositoryImpl = mock[ContributionScoreRepositoryImpl]
  val mockmonthlyIndividualScoreRepo: MonthlyIndividualScoreRepositoryImpl = mock[MonthlyIndividualScoreRepositoryImpl]
  val monthlyStudioScoreRepo: MonthlyStudioScoreRepositoryImpl = mock[MonthlyStudioScoreRepositoryImpl]
  val allTimeIndividualScoreRepo: AllTimeIndividualScoreRepositoryImpl = mock[AllTimeIndividualScoreRepositoryImpl]
  val allTimeStudioScoreRepo: AllTimeStudioScoreRepositoryImpl = mock[AllTimeStudioScoreRepositoryImpl]

  val contribution: Contribution = Contribution("105", Some("Bhavya"), "bhavya@knoldus.com", ContributionType.BOOKS, "Doobie", "2022-08-07 12:33:01.617",
    Some("akka"), Some("www.spark.com"), 7, Some("Scala Studio"))
  val contributionWithStatus: ContributionWithStatus = ContributionWithStatus("105", Some("Bhavya"), "bhavya@knoldus.com", ContributionType.BOOKS, "Doobie", "2022-08-07 12:33:01.617",
    Some("akka"), ContributionStatus.APPROVED, Some("www.spark.com"), 7, Some("Scala Studio"))
  val scoreMultiplier: ScoreMultiplier = ScoreMultiplier(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 7, 2022)
  val contributionScore: ContributionScore = ContributionScore(Some(105), Some("105"), Some("Bhavya"), "bhavya@knoldus.com", ContributionType.BOOKS, "Spark", "2022-07-30 12:33:01.617",
    Some("akka"), Some("www.spark.com"), 7, Some("Scala Studio"), 1.0, "561BD57BB85EDB12AB")
  val monthlyStudioScore: MonthlyStudioScore = MonthlyStudioScore(7, 100.0,8, 2022)
  val allTimeIndividualWithStudioScore: AllTimeIndividualWithStudioScore = AllTimeIndividualWithStudioScore("bhavya@knoldus.com", 7, 1.1)
  val allTimeStudioScore: AllTimeStudioScore = AllTimeStudioScore(7, 100.0)
  val monthlyIndividualWithStudioScore: MonthlyIndividualWithStudioScore = MonthlyIndividualWithStudioScore("bhavya@knoldus.com", 7, 1.0, 7, 2022)
  val allTimeIndividualScore: AllTimeIndividualScore = AllTimeIndividualScore("bhavya@knoldus.com", 100.0)
  val monthlyIndividualScore: MonthlyIndividualScore = MonthlyIndividualScore("bhavya@knoldus.com", 100.0, 8, 2022)
  val individualMonthlyContributionTypeScore: IndividualMonthlyContributionTypeScore = IndividualMonthlyContributionTypeScore("bhavya@knoldus.com", 8, 2022, ContributionType.BOOKS, 100.0)
  val contributionTypeScore: ContributionTypeScore = ContributionTypeScore(ContributionType.BOOKS, 100.0)
  val individualContributionTypeScore: IndividualContributionTypeScore = IndividualContributionTypeScore("bhavya@knoldus.com", ContributionType.BOOKS, 100.0)
  val allContributionTypeScores: AllContributionTypeScores = AllContributionTypeScores("bhavya@knoldus.com", List(contributionTypeScore))
  val dailyScore: DailyScore = DailyScore("bhavya@knoldus.com", "Bhavya", 100.0)
  val allMonthlyContributionTypeScores: AllMonthlyContributionTypeScores = AllMonthlyContributionTypeScores("bhavya@knoldus.com", 8, 2022, List(ContributionTypeScore(ContributionType.BOOKS, 100.0)))
  implicit val tenantId: Int = 1

  it should "return calculate score" in {

    val result = scoringService.calculateScore(contributionWithStatus)
    result.map{
      result => assertResult(1)(result)
    }
  }

  it should "get SpikeMonthDetails" in {
    val mockSpikeMonthScoreRepositoryImpl = mock[SpikeMonthScoreRepositoryImpl]
    when(mockSpikeMonthScoreRepositoryImpl.getSpikeMonthMultipliers(7, 2022)).thenReturn(IO.some(scoreMultiplier))
    scoringService.getSpikeMonthDetails(7, 2022).map {
          result => assertResult(Some(scoreMultiplier))(result)
    }
  }

  it should "get AllTimeStudioScores" in {
    val mockAllTimeStudioScoreRepositoryImpl = mock[AllTimeStudioScoreRepositoryImpl]
    when(mockAllTimeStudioScoreRepositoryImpl.allStudioAllTimeScores).thenReturn(IO(List(allTimeStudioScore)))
    scoringService.getAllTimeStudioScores().map{
      result => assertResult(Some(List(allTimeStudioScore)))(result)
    }
  }

  it should "get allTimeIndividualScores" in {
    val mockContributionScoreRepositoryImpl = mock[ContributionScoreRepositoryImpl]
    when(mockContributionScoreRepositoryImpl.getAllTimeIndividualContributionScores()).thenReturn(IO(List(allTimeIndividualScore)))
    scoringService.getAllTimeIndividualScores().map {
      result => assertResult(Some(List(allTimeIndividualScore)))(result)
    }
  }

  it should "get AllMonthlyStudioScores" in {
    val mockMonthlyStudioScoreRepositoryImpl = mock[MonthlyStudioScoreRepositoryImpl]
    when(mockMonthlyStudioScoreRepositoryImpl.allStudioMonthlyScores(8, 2022)).thenReturn(IO(List(monthlyStudioScore)))
    scoringService.getAllMonthlyStudioScores(8, 2022).map {
      result => assertResult(Some(List(monthlyStudioScore)))(result)
    }
  }

  it should "get AllMonthlyIndividualScores" in {
    val mockContributionScoreRepositoryImpl = mock[ContributionScoreRepositoryImpl]
    val monthlyIndividualScore = MonthlyIndividualScore("bhavya@knoldus.com", 100.0, 8, 2022)
    when(mockContributionScoreRepositoryImpl.getMonthlyIndividualContributionScores(8, 2022)).thenReturn(IO(List(monthlyIndividualScore)))
    scoringService.getAllMonthlyIndividualScores(8, 2022).map {
      result => assertResult(Some(List(monthlyIndividualScore)))(result)
    }
  }

  it should "get AllTimeMonthlyContributionTypeScores" in {
    val contributionScoreRepositoryImpl = mock[ContributionScoreRepositoryImpl]
    when(contributionScoreRepositoryImpl.getAllTimeMonthlyContributions()).thenReturn(IO(List(individualMonthlyContributionTypeScore)))
    scoringService.getAllTimeMonthlyContributionTypeScores().map {
      result => assertResult(Some(Right(List(individualMonthlyContributionTypeScore))))(result)
    }
  }

  it should "get MonthlyScores" in {
    val mockContributionScoreRepositoryImpl = mock[ContributionScoreRepositoryImpl]
    when(mockContributionScoreRepositoryImpl.getMonthlyContributions(8,2022)).thenReturn(IO(List(individualContributionTypeScore)))
    scoringService.getMonthlyScores(8, 2022).map {
      result => assertResult(Some(List(allContributionTypeScores)))(result)
    }
  }

  it should "get MonthlyContributionScores" in {
    val mockContributionScoreRepositoryImpl = mock[ContributionScoreRepositoryImpl]
    val individualContributionTypeScore = IndividualContributionTypeScore("bhavya@knoldus.com", ContributionType.BOOKS, 100.0)
    when(mockContributionScoreRepositoryImpl.getMonthlyContributions(8, 2022)).thenReturn(IO(List(individualContributionTypeScore)))
    scoringService.getMonthlyContributionScores(8, 2022).map {
      result => assertResult(Some(Right(List(individualContributionTypeScore))))(result)
    }
  }

  it should "get AllTimeMonthlyScores" in {
    val contributionScoreRepositoryImpl = mock[ContributionScoreRepositoryImpl]
    when(contributionScoreRepositoryImpl.getAllTimeMonthlyContributions()).thenReturn(IO(List(individualMonthlyContributionTypeScore)))
    scoringService.getAllTimeMonthlyScores().map {
      result => assertResult(Some(List(allMonthlyContributionTypeScores)))(result)
    }
  }

  it should "get TopDailyScores" in {
    val mockContributionScoreRepositoryImpl = mock[ContributionScoreRepositoryImpl]
    when(mockContributionScoreRepositoryImpl.getDailyLeadersScore()).thenReturn(IO(List(dailyScore)))
    scoringService.getTopDailyScores().map {
      result => assertResult(Some(List(dailyScore)))(result)
    }
  }
}
