package com.knoldus.repository

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.knoldus.model.MonthlyIndividualWithStudioScore
import com.typesafe.scalalogging.LazyLogging
import doobie.util.transactor.Transactor

import scala.concurrent.Future


trait MonthlyIndividualScoreRepository {

  def getIndividualMonthlyScore(email: String, studioId: Int, month: Int, year: Int)(implicit tenantId: Int): doobie.ConnectionIO[Option[MonthlyIndividualWithStudioScore]]


  def store(monthlyIndividualScore: MonthlyIndividualWithStudioScore)(implicit tenantId: Int): Future[Int]

  // Returns all Improvements in the repository. Obviously this is drastically over-simplified: no pagination,
  // filtering, sorting, etc.
  def allIndividualMonthlyScores(month: Int, year: Int)(implicit tenantId: Int): IO[List[MonthlyIndividualWithStudioScore]]

  def insert(monthlyIndividualScore: MonthlyIndividualWithStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int]

  def update(monthlyIndScore: MonthlyIndividualWithStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int]

  def safeInsert(monthlyIndScore: MonthlyIndividualWithStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int]
}


/** Concrete implementation that is backed by a SQL database.
 *
 * Supports optimistic concurrency and validates the constraint that components have
 * exactly one parent.
 */
class MonthlyIndividualScoreRepositoryImpl(xa: Transactor[IO]) extends MonthlyIndividualScoreRepository with LazyLogging {

  import doobie.implicits._
  val DatabaseIntegrityError = "23505"

  /**
   * Gets Individual Monthly Score.
   *
   * @param email to get individual data.
   * @param studioId studio's id to get score.
   * @param month to fetch that month data.
   * @param year to fetch that year data.
   * @return response MonthlyIndividualWithStudioScore.
   */
  override def getIndividualMonthlyScore(email: String, studioId :Int, month : Int, year : Int)(implicit tenantId: Int):
  doobie.ConnectionIO[Option[MonthlyIndividualWithStudioScore]] = {

    sql"""select email, studio_id, score, month, year from monthly_individual_score
           where email = ${email} and studio_id = ${studioId} and month = ${month} and year = ${year}"""
      .query[MonthlyIndividualWithStudioScore]
      .option
  }

  /**
   * Stores monthly individual Score in  monthly_individual_score table.
   *
   * @param monthlyIndividualScore a case class that hold details to store.
   * @return response in the form of Integer.
   */
  override def store(monthlyIndividualScore: MonthlyIndividualWithStudioScore)(implicit tenantId: Int): Future[Int] = {
    logger.info("Persisting Information in the Database")
    val insertMonthlyScore: doobie.ConnectionIO[Int] =
      sql"""INSERT INTO monthly_individual_score(email, studio_id, score, month, year,tenant_id)
          VALUES (${monthlyIndividualScore.email}, ${monthlyIndividualScore.studioId}, ${monthlyIndividualScore.score},
          ${monthlyIndividualScore.month}, ${monthlyIndividualScore.year} ,${tenantId})""".update.run

    logger.info("Insert Statement = " + insertMonthlyScore)
    insertMonthlyScore.transact(xa).unsafeToFuture()
  }

  /**
   * Stores monthly individual Score in  monthly_individual_score table.
   *
   * @param monthlyIndividualScore a case class that hold details to store.
   * @return response in the form of IO of Integer.
   */
  override def insert(monthlyIndividualScore: MonthlyIndividualWithStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int] = {
    logger.info("Inserting  MonthlyIndividualScore in the Database")
    sql"""INSERT INTO monthly_individual_score(email, studio_id, score, month, year,tenant_id)
          VALUES (${monthlyIndividualScore.email}, ${monthlyIndividualScore.studioId}, ${monthlyIndividualScore.score},
          ${monthlyIndividualScore.month}, ${monthlyIndividualScore.year},${tenantId} )""".update.run
  }

  /**
   * Updating Monthly Individual Score in monthly_individual_score table.
   *
   * @param monthlyIndScore a case class that hold details to update.
   * @return response in the form of IO of Integer.
   */
  override def update(monthlyIndScore: MonthlyIndividualWithStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int] = {
    logger.info(s"Updating Monthly Individual Score" )
    sql"""update monthly_individual_score
            set score = ${monthlyIndScore.score}
            where email = ${monthlyIndScore.email} and  month = ${monthlyIndScore.month}
            and year = ${monthlyIndScore.year}
            and tenant_id = ${tenantId}
          """.update.run

  }

  /**
   * Calls methods of insert and update of monthly score.
   *
   * @param monthlyIndScore a case class that hold details.
   * @return response in the form IO Connection of Integer.
   */
  override def safeInsert(monthlyIndScore: MonthlyIndividualWithStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int] = {
    getIndividualMonthlyScore(monthlyIndScore.email,monthlyIndScore.studioId,
      monthlyIndScore.month, monthlyIndScore.year)
      .flatMap(score =>
        (score match {
          case Some(_) => update(monthlyIndScore)
          case None => insert(monthlyIndScore)
        })
          .map(updated => updated)
      )
  }

  /**
   * Get all time individual score.
   *
   * @param month to fetch that month data.
   * @param year to fetch that year data.
   * @return response in the form IO of list of MonthlyIndividualWithStudioScore.
   */
  override def allIndividualMonthlyScores(month : Int, year : Int)(implicit tenantId: Int): IO[List[MonthlyIndividualWithStudioScore]] = {
    sql"""select email, studio_id, score, month, year from monthly_individual_score
          where month = ${month} and year = ${year}"""
      .query[MonthlyIndividualWithStudioScore]
      .to[List]
      .transact(xa)
  }

}

