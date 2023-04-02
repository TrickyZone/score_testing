package com.knoldus.service.scoring

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.free.Free
import cats.implicits.catsSyntaxApplicativeId
import com.knoldus.common.{DeliverableScoreConfig, ScoreConfig}
import com.knoldus.model.ContributionType.ContributionType
import com.knoldus.model.DeliverableType.DeliverableType
import com.knoldus.model._
import com.knoldus.repository._
import com.knoldus.utils.MD5HashGenerator
import com.typesafe.scalalogging.LazyLogging
import doobie.ConnectionIO
import doobie.free.connection
import doobie.implicits.{toDoobieApplicativeErrorOps, _}
import doobie.util.transactor.Transactor

import java.sql.Timestamp
import java.util.Calendar
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class ScoringServiceImpl(implicit xa: Transactor[IO], ec: ExecutionContext, scoreConfig: ScoreConfig,
                         deliverableConfig: DeliverableScoreConfig)
  extends ScoringService with LazyLogging {

  val contributionScoreRepo = new ContributionScoreRepositoryImpl(xa)
  val spikeMonthScoreRepo = new SpikeMonthScoreRepositoryImpl(xa)
  val monthlyIndividualScoreRepo = new MonthlyIndividualScoreRepositoryImpl(xa)
  val monthlyStudioScoreRepo = new MonthlyStudioScoreRepositoryImpl(xa)
  val allTimeStudioScoreRepo = new AllTimeStudioScoreRepositoryImpl(xa)
  val allTimeIndividualScoreRepo = new AllTimeIndividualScoreRepositoryImpl(xa)

  private val contributionTypeToScoreMap: Map[ContributionType, Int] = {
    Map(ContributionType.BLOG -> scoreConfig.scorePerBlog,
      ContributionType.KNOLX -> scoreConfig.scorePerKnolx,
      ContributionType.CONFERENCE -> scoreConfig.scorePerConference,
      ContributionType.MEETUP -> scoreConfig.scorePerMeetup,
      ContributionType.OPEN_SOURCE -> scoreConfig.scorePerOsContribution,
      ContributionType.RESEARCH_PAPER -> scoreConfig.scorePerResearchPaper,
      ContributionType.TECHHUB -> scoreConfig.scorePerTechhub,
      ContributionType.WEBINAR -> scoreConfig.scorePerWebinar,
      ContributionType.BOOKS -> scoreConfig.scorePerBook,
      ContributionType.PODCAST -> scoreConfig.scorePerPodcast,
      ContributionType.PMO_TEMPLATES -> scoreConfig.scorePerPmoTemplate,
      ContributionType.PROCESSED_DOCUMENTS -> scoreConfig.scorePerProcessedDocument,
      ContributionType.PROPOSAL -> scoreConfig.scorePerProposal,
      ContributionType.CERTIFICATION -> scoreConfig.scorePerCertification,
      ContributionType.KNOLX_ATTENDEE -> scoreConfig.scoreKnolxAttendee,
      ContributionType.KNOLX_SPOTLIGHT_MEMBER -> scoreConfig.scoreKnolxSpotlightMember,
      ContributionType.OTHER -> scoreConfig.scorePerOther
    )
  }
  private val deliverableTypeToScoreMap: Map[DeliverableType, Int] = {
    Map(DeliverableType.VIDEOS -> deliverableConfig.scorePerVideos,
      DeliverableType.PODCAST -> deliverableConfig.scorePerPodcast,
      DeliverableType.DOCUMENT -> deliverableConfig.scorePerDocument,
      DeliverableType.PRESENTATION -> deliverableConfig.scorePerPresentation)
  }

  /**
   * Calculate Updated Monthly Individual Score in monthly_individual_score table.
   *
   * @param contribution a case class that contains details to update.
   * @return response in the form of Integer.
   */
  override def calculateScore(contribution: ContributionWithStatus)(implicit tenantId: Int): Future[Int] = {
    //contributionScoreRepo.store(contributionScore)
    contribution match {
      case contribution if contribution.status == ContributionStatus.APPROVED => updateScores(contribution)
      case contribution if contribution.status == ContributionStatus.REJECTED => calculateRejectedScore(contribution)
    }
  }


  /**
   * Calculating Score for the Contribution and check duplicate contribution.
   *
   * @param contribution a case class that contains details.
   * @return response in the form of Integer.
   */
  private def updateScores(contribution: ContributionWithStatus)(implicit tenantId: Int): Future[Int] = {
    logger.info(s"Calculating Score for the Contribution ${contribution.title}")
    val score = checkingContributionScore(contribution)
    logger.info(s"Calculating Score for the Other Contribution ${score}")
    val multiplier = Await.result(getScoreMultiplierForContributionType(contribution), Duration(500,TimeUnit.MILLISECONDS))
    val finalScore = (multiplier * score).round.toFloat
    val md5Hash = MD5HashGenerator.getMD5HashValue(contribution.toString)
    logger.info(s"MD5Hash Generated = $md5Hash")
    val contributionExists: IO[Option[Int]] = contributionScoreRepo.getContributionScoreForMd5Hash(md5Hash,
      contribution.contributionType, contribution.contributionId, contribution.email)

    contributionExists.unsafeToFuture().flatMap {
      case Some(_) =>
        logger.info(s"Duplicate Contribution Received with Title = ${contribution.title}, " +
          s"contributionType = ${contribution.contributionType} and email = ${contribution.email}" +
          s" Ignoring Contribution ")
        Future {-1}
      case None =>
        val contributionScore = ContributionScore(None, Some(contribution.contributionId), contribution.fullName,
          contribution.email, contribution.contributionType, contribution.title, contribution.contributionDate,
          contribution.technologyDetails, contribution.urlDetails, contribution.studioId, contribution.studioName,
          finalScore, md5Hash)
        logger.info(s"Contribution Score in the Database for $contributionScore for Tenant = $tenantId and Score = $score")

        val contributionDate = Timestamp.valueOf(contribution.contributionDate)
        logger.info(s"Contribution Date = ${contributionDate}")
        val cal = Calendar.getInstance()
        cal.setTimeInMillis(contributionDate.getTime)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)

        val email = contribution.email
        val studioId = contribution.studioId

        val query: Free[connection.ConnectionOp, Int] = for {
          updated <- contributionScoreRepo.storeContributionScore(contributionScore)
          monthlyIndScore <- contributionScoreRepo.getMonthlyIndividualContributionScore(email, studioId, month, year)
          _ <- monthlyIndScore match {
            case Some(monthlyScore) => monthlyIndividualScoreRepo.safeInsert(monthlyScore)
            case None =>   MonthlyIndividualWithStudioScore(email,studioId, finalScore, month, year).pure[ConnectionIO]
          }
          monthlyStudioScore <- contributionScoreRepo.getMonthlyStudioContributionScore(studioId, month, year)
          _ <- monthlyStudioScore match {
            case Some(monthlyScore) => monthlyStudioScoreRepo.safeInsert(monthlyScore)
            case None =>  MonthlyStudioScore(studioId, finalScore, month, year).pure[ConnectionIO]
          }
          allTimeIndividualScore <- contributionScoreRepo.getAllTimeContributionScoreForIndividual(email,studioId)
          _ <- allTimeIndividualScore match {
            case Some(allTimeScore) => allTimeIndividualScoreRepo.safeInsert(allTimeScore)
            case None => AllTimeIndividualWithStudioScore(email,studioId, finalScore).pure[ConnectionIO]
          }
          getAllTimeStudioScore <- contributionScoreRepo.getAllTimeStudioContributionScore(studioId)
          _ <- getAllTimeStudioScore match {
            case Some(allTimeScore) => allTimeStudioScoreRepo.safeInsert(allTimeScore)
            case None => AllTimeStudioScore(studioId, finalScore).pure[ConnectionIO]
          }
        } yield {
          updated
        }
        query.exceptSql(sqlException => throw new RuntimeException(sqlException)).transact(xa).unsafeToFuture()
    }


  }


  private def getScoreMultiplierForContributionType(contribution: ContributionWithStatus)(implicit tenantId: Int): Future[Float] = {
    val contributionDate = Timestamp.valueOf(contribution.contributionDate)
    logger.info(s"Contribution Date = $contributionDate")
    val cal = Calendar.getInstance()
    cal.setTimeInMillis(contributionDate.getTime)
    val month = cal.get(Calendar.MONTH) + 1
    val year = cal.get(Calendar.YEAR)
    val spikeMonthMultipliers = getSpikeMonthDetails(month, year)
    spikeMonthMultipliers map  {
      value => value match {
        case Some(scoreMultiplier: ScoreMultiplier) =>
          logger.info(s"Score Multiplier Value ${scoreMultiplier}  ")
          contributionTypeToScoreMultiplierMap(scoreMultiplier).get(contribution.contributionType).getOrElse(1.0f)
        case None => 1.0f
      }
    }
  }


  /**
   * Gets Monthly Score of ALl contribution type.
   *
   * @param month to fetch that month data.
   * @param year to fetch that year data.
   * @return response in the form of AllContributionTypeScores.
   */
  override def getMonthlyScores(month: Int, year: Int)(implicit tenantId: Int): Future[Option[List[AllContributionTypeScores]]] = {
    val individualScores = getMonthlyContributionScores(month, year)
    individualScores.map {
      case Some(scores) => scores match {
        case Right(value) => Option(createMonthlyContributionMap(value))
        case Left(_) => Option(List.empty[AllContributionTypeScores])
      }
      case None => Option(List.empty[AllContributionTypeScores])
    }
  }

  /**
   * Gets Monthly Score of each contribution type.
   *
   * @param month to fetch that month data.
   * @param year to fetch that year data.
   * @return response either list of IndividualContributionTypeScore or list of IndividualContributionTypeScore.
   */
  override def getMonthlyContributionScores(month: Int, year: Int)(implicit tenantId: Int):
  Future[Option[Either[List[IndividualContributionTypeScore], List[IndividualContributionTypeScore]]]] = {
    for {
      result <- contributionScoreRepo.getMonthlyContributions(month, year).attemptSomeSqlState {
        case _ => List.empty[IndividualContributionTypeScore]
      }.unsafeToFuture()
    } yield Option(result)
  }



  /**
   * Gets Monthly Score of ALl contribution type.
   *
   * @param month to fetch that month data.
   * @param year to fetch that year data.
   * @return response in the form of AllContributionTypeScores.
   */
  override def getAllTimeMonthlyScores()(implicit tenantId: Int): Future[Option[List[AllMonthlyContributionTypeScores]]] = {
    val individualMonthlyScores = getAllTimeMonthlyContributionTypeScores()
    individualMonthlyScores.map {
      case Some(scores) => scores match {
        case Right(value) => Option(createAllTimeMonthlyContributionMap(value))
        case Left(_) => Option(List.empty[AllMonthlyContributionTypeScores])
      }
      case None => Option(List.empty[AllMonthlyContributionTypeScores])
    }
  }


  /**
   *
   * @return
   */
  override def getAllTimeMonthlyContributionTypeScores()(implicit tenantId: Int):
  Future[Option[Either[List[IndividualMonthlyContributionTypeScore], List[IndividualMonthlyContributionTypeScore]]]] = {

    for {
      result <- contributionScoreRepo.getAllTimeMonthlyContributions().attemptSomeSqlState {
        case _ => List.empty[IndividualMonthlyContributionTypeScore]
      }.unsafeToFuture()
    } yield Option(result)
  }

  /**
   * Gets Monthly Individual Score.
   *
   * @param month to fetch that month data.
   * @param year to fetch that year data.
   * @return response in the form of list of MonthlyIndividualScore.
   */
  override def getAllMonthlyIndividualScores(month: Int, year: Int)(implicit tenantId: Int): Future[Option[List[MonthlyIndividualScore]]] = {
    for {
      result <- contributionScoreRepo.getMonthlyIndividualContributionScores(month,year).unsafeToFuture()
    }
      yield Option(result)
  }

  /**
   * Gets All time Monthly Studio Score.
   *
   * @param month to fetch that month data.
   * @param year to fetch that year data.
   * @return response in the form of list of MonthlyStudioScore.
   */
  override def getAllMonthlyStudioScores(month: Int, year: Int)(implicit tenantId: Int): Future[Option[List[MonthlyStudioScore]]] = {
    for {
      result <- monthlyStudioScoreRepo.allStudioMonthlyScores(month,year).unsafeToFuture()
    }
      yield Option(result)
  }

  /**
   * Gets All time Monthly Individual Scores.
   *
   * @return response in the form of list of AllTimeIndividualScore.
   */
  override def getAllTimeIndividualScores()(implicit tenantId: Int): Future[Option[List[AllTimeIndividualScore]]] = {
    for {
      result <- contributionScoreRepo.getAllTimeIndividualContributionScores().unsafeToFuture()
    }
      yield Option(result)
  }


  /**
   * Gets Daily Top Individual Scores.
   *
   * @return response in the form of list of AllTimeIndividualScore.
   */
  override def getTopDailyScores()(implicit tenantId: Int): Future[Option[List[DailyScore]]] = {
    for {
      result <- contributionScoreRepo.getDailyLeadersScore().unsafeToFuture()
    }
      yield Option(result)
  }


  /**
   * Gets All time Studio Score.
   *
   * @return response in the form of list of AllTimeStudioScore.
   */
  override def getAllTimeStudioScores()(implicit tenantId: Int): Future[Option[List[AllTimeStudioScore]]] = {
    for {
      result <- allTimeStudioScoreRepo.allStudioAllTimeScores.unsafeToFuture()
    }
      yield Option(result)
  }

  /**
   * Gets Spike month details of all type of contributions.
   *
   * @param month to fetch that month data.
   * @param year to fetch that year data.
   * @return response in the form of ScoreMultiplier.
   */
  def getSpikeMonthDetails(month: Int, year: Int)(implicit tenantId: Int): Future[Option[ScoreMultiplier]] ={
    spikeMonthScoreRepo.getSpikeMonthMultipliers(month,year).unsafeToFuture()
  }

  /**
   * Mapping monthly contribution type score.
   *
   * @param contributionTypeScoreList list of score with each contribution type.
   * @return response in the form of list of AllContributionTypeScores.
   */
  private def createMonthlyContributionMap(contributionTypeScoreList: List[IndividualContributionTypeScore]): List[AllContributionTypeScores] = {
    contributionTypeScoreList.groupBy(contri => (contri.email)).map {
      case (email, scoreList) =>
        val scores = scoreList.map {
          indContributionScore =>
            ContributionTypeScore(indContributionScore.contributionType,
              indContributionScore.score)
        }
        AllContributionTypeScores(email, scores)
    }.toList

  }

  /**
   * Mapping monthly contribution type score.
   *
   * @param contributionTypeScoreList list of score with each contribution type.
   * @return response in the form of list of AllContributionTypeScores.
   */
  private def createAllTimeMonthlyContributionMap(contributionTypeScoreList: List[IndividualMonthlyContributionTypeScore]):
  List[AllMonthlyContributionTypeScores] = {
    contributionTypeScoreList.groupBy(contri => (contri.email,contri.month, contri.year)).map {
      case ((email, month, year), scoreList) =>
        val scores = scoreList.map {
          indContributionScore =>
            ContributionTypeScore(indContributionScore.contributionType,
              indContributionScore.score)
        }
        AllMonthlyContributionTypeScores(email, month, year, scores)
    }.toList

  }


  private def contributionTypeToScoreMultiplierMap(scoreMultiplier: ScoreMultiplier): Map[ContributionType, Float] = {
    Map(ContributionType.BLOG -> scoreMultiplier.blogScoreMultiplier,
      ContributionType.KNOLX -> scoreMultiplier.knolxScoreMultiplier,
      ContributionType.CONFERENCE -> scoreMultiplier.conferenceScoreMultiplier,
      ContributionType.MEETUP -> scoreMultiplier.meetupScoreMultiplier,
      ContributionType.OPEN_SOURCE -> scoreMultiplier.osContributionScoreMultiplier,
      ContributionType.RESEARCH_PAPER -> scoreMultiplier.researchPaperScoreMultiplier,
      ContributionType.TECHHUB -> scoreMultiplier.techHubScoreMultiplier,
      ContributionType.BOOKS -> scoreMultiplier.bookScoreMultiplier,
      ContributionType.WEBINAR -> scoreMultiplier.webinarScoreMultiplier,
    )
  }

  override def calculateRejectedScore(contribution: ContributionWithStatus)(implicit tenantId: Int): Future[Int] = {
    val score = contributionTypeToScoreMap.getOrElse(contribution.contributionType, 0)
    val multiplier = Await.result(getScoreMultiplierForContributionType(contribution), Duration(500, TimeUnit.MILLISECONDS))
    val finalScore = (multiplier * score).round.toFloat * -1
    val md5Hash = MD5HashGenerator.getMD5HashValue(contribution.toString)

        val contributionScore = ContributionScore(None, Some(contribution.contributionId), contribution.fullName,
          contribution.email, contribution.contributionType, contribution.title, contribution.contributionDate,
          contribution.technologyDetails, contribution.urlDetails, contribution.studioId, contribution.studioName,
          finalScore, md5Hash)
        contributionScoreRepo.storeContributionForRejected(contributionScore).transact(xa).unsafeToFuture()
    }

  private def checkingContributionScore(contribution: ContributionWithStatus): Int = {
    val checkContributionIsDeliverableOrNot = contribution.deliverableType match {
      case Some(deliverableType) => deliverableTypeToScoreMap.getOrElse(contribution.deliverableType.getOrElse(deliverableType), 0)
      case None => contributionTypeToScoreMap.getOrElse(contribution.contributionType, 0)
    }
    contribution.deliverableContributioncount match {
      case Some(count) => contribution match {
        case onlineCourseContribution if onlineCourseContribution.contributionType == ContributionType.ONLINE_COURSE =>
          count * checkContributionIsDeliverableOrNot
        case _ =>
          1 * checkContributionIsDeliverableOrNot
      }
      case None =>
        1 * checkContributionIsDeliverableOrNot
    }
  }
}
