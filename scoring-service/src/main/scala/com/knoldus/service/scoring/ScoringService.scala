package com.knoldus.service.scoring

import com.knoldus.model._

import scala.concurrent.Future

trait ScoringService {

  def calculateScore(contribution: ContributionWithStatus)(implicit tenantId: Int): Future[Int]

  def getMonthlyContributionScores(month: Int, year: Int)(implicit tenantId: Int):
  Future[Option[Either[List[IndividualContributionTypeScore], List[IndividualContributionTypeScore]]]]

  def getMonthlyScores(month: Int, year: Int)(implicit tenantId: Int): Future[Option[List[AllContributionTypeScores]]]

  def getSpikeMonthDetails(month: Int, year: Int)(implicit tenantId: Int): Future[Option[ScoreMultiplier]]

  def getAllTimeIndividualScores()(implicit tenantId: Int): Future[Option[List[AllTimeIndividualScore]]]

  def getAllTimeStudioScores()(implicit tenantId: Int): Future[Option[List[AllTimeStudioScore]]]

  def getAllMonthlyStudioScores(month: Int, year: Int)(implicit tenantId: Int): Future[Option[List[MonthlyStudioScore]]]

  def getAllMonthlyIndividualScores(month: Int, year: Int)(implicit tenantId: Int): Future[Option[List[MonthlyIndividualScore]]]

  def getAllTimeMonthlyContributionTypeScores()(implicit tenantId: Int): Future[Option[Either[List[IndividualMonthlyContributionTypeScore],
    List[IndividualMonthlyContributionTypeScore]]]]

  /**
   * Gets Monthly Score of ALl contribution type.
   *
   * @param month to fetch that month data.
   * @param year  to fetch that year data.
   * @return response in the form of AllContributionTypeScores.
   */
  def getAllTimeMonthlyScores()(implicit tenantId: Int): Future[Option[List[AllMonthlyContributionTypeScores]]]

  def getTopDailyScores()(implicit tenantId: Int): Future[Option[List[DailyScore]]]

  def calculateRejectedScore(contribution: ContributionWithStatus)(implicit tenantId: Int): Future[Int]
}
