package com.knoldus.repository

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.knoldus.model.MonthlyStudioScore
import com.typesafe.scalalogging.LazyLogging
import doobie.util.transactor.Transactor

import scala.concurrent.Future


trait MonthlyStudioScoreRepository {

  def getStudioMonthlyScore(studioId: Int, month: Int, year: Int)(implicit tenantId: Int): doobie.ConnectionIO[Option[MonthlyStudioScore]]


  def store(monthlyIndividualScore: MonthlyStudioScore)(implicit tenantId: Int): Future[Int]

  // Returns all Improvements in the repository. Obviously this is drastically over-simplified: no pagination,
  // filtering, sorting, etc.
  def allStudioMonthlyScores(month: Int, year: Int)(implicit tenantId: Int): IO[List[MonthlyStudioScore]]

  def insert(monthlyStudioScore: MonthlyStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int]

  def update(monthlyStudioScore: MonthlyStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int]

  def safeInsert(monthlyStudioScore: MonthlyStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int]
}


/** Concrete implementation that is backed by a SQL database.
 *
 * Supports optimistic concurrency and validates the constraint that components have
 * exactly one parent.
 */
class MonthlyStudioScoreRepositoryImpl(xa: Transactor[IO]) extends MonthlyStudioScoreRepository with LazyLogging {

  import doobie.implicits._

  /**
   * Gets studio Monthly Score.
   *
   * @param studioId studio's id to get score.
   * @param month to fetch that month data.
   * @param year to fetch that year data.
   * @return response MonthlyStudioScore.
   */
  override def getStudioMonthlyScore(studioId :Int, month : Int, year : Int)(implicit tenantId: Int): doobie.ConnectionIO[Option[MonthlyStudioScore]] = {
    sql"""select  studio_id, score, month, year from monthly_studio_score
           where studio_id = ${studioId} and month = ${month} and year = ${year} """
      .query[MonthlyStudioScore]
      .option
  }

  /**
   * Stores monthly studio Score in  monthly_individual_score table.
   *
   * @param monthlyStudioScore a case class that hold score of studio to store.
   * @return response in the form of Integer.
   */
  override def store(monthlyStudioScore: MonthlyStudioScore)(implicit tenantId: Int): Future[Int] = {
    logger.info("Persisting Information in the Database")
    val insertMonthlyScore: doobie.ConnectionIO[Int] =
      sql"""INSERT INTO monthly_studio_score( studio_id, score, month, year,tenant_id)
          VALUES ( ${monthlyStudioScore.studioId}, ${monthlyStudioScore.score},${tenantId},
          ${monthlyStudioScore.month}, ${monthlyStudioScore.year} )""".update.run

    logger.info("Insert Statement = " + insertMonthlyScore)
    insertMonthlyScore.transact(xa).unsafeToFuture()
  }

  /**
   * Get all studio score by month.
   *
   * @param month to fetch that month data.
   * @param year to fetch that year data.
   * @return response in the form IO of list of MonthlyStudioScore.
   */
  override def allStudioMonthlyScores(month : Int, year : Int)(implicit tenantId: Int): IO[List[MonthlyStudioScore]] = {
    sql"""select  studio_id, score, month, year from monthly_studio_score where month = ${month} and year = ${year}"""
      .query[MonthlyStudioScore]
      .to[List]
      .transact(xa)
  }

  /**
   * Stores monthly studio Score in  monthly_individual_score table.
   *
   * @param monthlyStudioScore a case class that hold score to store.
   * @return response in the form of IO of Integer.
   */
  override def insert(monthlyStudioScore: MonthlyStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int] = {
    logger.info("Inserting Monthly Studio Score")
    sql"""INSERT INTO monthly_studio_score( studio_id, score, month, year,tenant_id)
          VALUES ( ${monthlyStudioScore.studioId}, ${monthlyStudioScore.score},
          ${monthlyStudioScore.month}, ${monthlyStudioScore.year},${tenantId} )""".update.run
  }

  /**
   * Updating Monthly studio Score in monthly_studio_score table.
   *
   * @param monthlyStudioScore a case class that hold score to update.
   * @return response in the form of IO of Integer.
   */
  override def update(monthlyStudioScore: MonthlyStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int] = {
    sql"""update monthly_studio_score
            set score = ${monthlyStudioScore.score}
            where studio_id = ${monthlyStudioScore.studioId} and  month = ${monthlyStudioScore.month}
            and year = ${monthlyStudioScore.year}
            and tenant_id = ${tenantId}
          """.update.run

  }

  /**
   * Calls methods of insert and update of monthly score.
   *
   * @param monthlyStudioScore a case class that hold monthly studio score.
   * @return response in the form IO Connection of Integer.
   */
  override def safeInsert(monthlyStudioScore: MonthlyStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int] = {
    getStudioMonthlyScore(monthlyStudioScore.studioId, monthlyStudioScore.month, monthlyStudioScore.year)
      .flatMap(score =>
        (score match {
          case Some(_) => update(monthlyStudioScore)
          case None => insert(monthlyStudioScore)
        })
          .map(updated => updated)
      )
  }

}

