package com.knoldus.repository

import cats.data.OptionT
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.knoldus.model.ScoreMultiplier
import com.typesafe.scalalogging.LazyLogging
import doobie.util.transactor.Transactor

import scala.concurrent.Future


trait SpikeMonthScoreRepository {

  def getSpikeMonthMultipliers(month: Int, year: Int)(implicit tenantId: Int): IO[Option[ScoreMultiplier]]


  def store(scoreMultiplier: ScoreMultiplier)(implicit tenantId: Int): Future[Int]

}


/** Concrete implementation that is backed by a SQL database.
 *
 * Supports optimistic concurrency and validates the constraint that components have
 * exactly one parent.
 */
class SpikeMonthScoreRepositoryImpl(xa: Transactor[IO]) extends SpikeMonthScoreRepository with LazyLogging {

  import doobie.implicits._

  /**
   * Gets spike month Score from dynamic_scoring table.
   *
   * @param month to fetch that month contribution score.
   * @param year to fetch that year contribution score.
   * @return response in the form of option of ScoreMultiplier.
   */
  override def getSpikeMonthMultipliers(month: Int, year: Int)(implicit tenantId: Int): IO[Option[ScoreMultiplier]] = {
    val select =
      sql"""select blog_score_multiplier, knolx_score_multiplier, webinar_score_multiplier,
             os_contribution_score_multiplier, techhub_score_multiplier,  conference_score_multiplier,
              book_score_multiplier, research_paper_score_multiplier, meetup_score_multiplier, month, year
             from dynamic_scoring where month = $month and year = $year and tenant_id = $tenantId"""
        .query[ScoreMultiplier]
        .option
    OptionT(select.transact(xa)).value
  }

  /**
   * Stores spike month Score in dynamic_scoring table.
   *
   * @param scoreMultiplier a case class that hold score of all type contributions.
   * @return response in the form of Integer.
   */
  override def store(scoreMultiplier: ScoreMultiplier)(implicit tenantId: Int): Future[Int] = {
    logger.info("Persisting Information in the Database")
    val insertScoreMultiplier: doobie.ConnectionIO[Int] =
      sql"""INSERT INTO dynamic_scoring(blog_score_multiplier, knolx_score_multiplier, webinar_score_multiplier,
             techhub_score_multiplier, os_contribution_score_multiplier, conference_score_multiplier,
              research_paper_score_multiplier,meetup_score_multiplier,book_score_multiplier, month, year,tenant_id)
          VALUES (${scoreMultiplier.blogScoreMultiplier}, ${scoreMultiplier.knolxScoreMultiplier},
          ${scoreMultiplier.webinarScoreMultiplier}, ${scoreMultiplier.techHubScoreMultiplier},
          ${scoreMultiplier.osContributionScoreMultiplier},${scoreMultiplier.conferenceScoreMultiplier},
          ${scoreMultiplier.researchPaperScoreMultiplier}, ${scoreMultiplier.meetupScoreMultiplier},
          ${scoreMultiplier.bookScoreMultiplier}, ${scoreMultiplier.month}, ${scoreMultiplier.year},${tenantId} )""".update.run

    logger.info("Insert Statement = " + insertScoreMultiplier)
    insertScoreMultiplier.transact(xa).unsafeToFuture()
  }

}

