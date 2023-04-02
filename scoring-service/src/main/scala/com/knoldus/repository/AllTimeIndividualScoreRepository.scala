package com.knoldus.repository

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.knoldus.model.{AllTimeIndividualScore, AllTimeIndividualWithStudioScore}
import com.typesafe.scalalogging.LazyLogging
import doobie.util.transactor.Transactor

import scala.concurrent.Future


trait AllTimeIndividualScoreRepository {

  def getAllTimeIndividualScore(email: String, studioId : Int)(implicit tenantId: Int): doobie.ConnectionIO[Option[AllTimeIndividualWithStudioScore]]

  def store(allTimeIndividualScore: AllTimeIndividualWithStudioScore)(implicit tenantId: Int): Future[Int]

  // Returns all Improvements in the repository. Obviously this is drastically over-simplified: no pagination,
  // filtering, sorting, etc.
  def allIndividualAllTimeScores(implicit tenantId: Int): IO[List[AllTimeIndividualScore]]

  def insert(allTimeIndividualScore: AllTimeIndividualWithStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int]

  def update(allTimeIndividualScore: AllTimeIndividualWithStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int]

  def safeInsert(allTimeIndividualScore: AllTimeIndividualWithStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int]
}


/** Concrete implementation that is backed by a SQL database.
 *
 * Supports optimistic concurrency and validates the constraint that components have
 * exactly one parent.
 */
class AllTimeIndividualScoreRepositoryImpl(xa: Transactor[IO]) extends AllTimeIndividualScoreRepository with LazyLogging {

  import doobie.implicits._

  /**
   * Gets All time Individual Score.
   *
   * @param email to get individual data.
   * @param studioId studio's id to get score.
   * @return response All time individual score with studio id.
   */
  override def getAllTimeIndividualScore(email: String, studioId :Int)(implicit tenantId: Int): doobie.ConnectionIO[Option[AllTimeIndividualWithStudioScore]] = {
    sql"""select email, studio_id, score from all_time_individual_score where email = ${email}
         and studio_id = ${studioId}"""
      .query[AllTimeIndividualWithStudioScore]
      .option
  }

  /**
   * Stores individual All time Individual Score in  all_time_individual_score table.
   *
   * @param allTimeIndividualScore a case class that hold score.
   * @return response in the form of future of Integer.
   */
  override def store(allTimeIndividualScore: AllTimeIndividualWithStudioScore)(implicit tenantId: Int): Future[Int] = {
    logger.info("Persisting Information in the Database")
    val insertAllTimeScore: doobie.ConnectionIO[Int] =
      sql"""INSERT INTO all_time_individual_score( email, studio_id, score,tenant_id)
          VALUES (${allTimeIndividualScore.email}, ${allTimeIndividualScore.studioId},
           ${allTimeIndividualScore.score},${tenantId})""".update.run

    logger.info("Insert Statement = " + insertAllTimeScore)
    insertAllTimeScore.transact(xa).unsafeToFuture()
  }

  /**
   * Stores individual All time Individual Score in  all_time_individual_score table.
   *
   * @param allTimeIndividualScore a case class that hold score.
   * @return response in the form IO Connection of Integer.
   */
  override def insert(allTimeIndividualScore: AllTimeIndividualWithStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int] = {
    logger.info("Inserting into All Time Individual Score table")
    sql"""INSERT INTO all_time_individual_score( email, studio_id, score,tenant_id)
          VALUES (${allTimeIndividualScore.email}, ${allTimeIndividualScore.studioId},
           ${allTimeIndividualScore.score},${tenantId})""".update.run
  }

  /**
   * Updates individual All time Individual Score in  all_time_individual_score table.
   *
   * @param allTimeIndividualScore a case class that stores values to update.
   * @return response in the form of future of Integer.
   */
  override def update(allTimeIndividualScore: AllTimeIndividualWithStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int] = {
    logger.info(s"Updating all Time Individual Score" )
    sql"""update all_time_individual_score
            set score = ${allTimeIndividualScore.score}
            where email = ${allTimeIndividualScore.email}
            and tenant_id = ${tenantId}
          """.update.run

  }

  /**
   * Calls methods of insert and update to stores individual All time Individual Score in all_time_individual_score table.
   *
   * @param allTimeIndividualScore a case class that hold score.
   * @return response in the form IO Connection of Integer.
   */
  override def safeInsert(allTimeIndividualScore: AllTimeIndividualWithStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int] = {
    getAllTimeIndividualScore(allTimeIndividualScore.email,allTimeIndividualScore.studioId )
      .flatMap(score =>
        (score match {
          case Some(_) => update(allTimeIndividualScore)
          case None => insert(allTimeIndividualScore)
        })
          .map(updated => updated)
      )
  }

  /**
   * Gets All time Individual Score from all_time_individual_score table.
   *
   * @return response in the form of list of All time individual score.
   */
  override def allIndividualAllTimeScores(implicit tenantId: Int): IO[List[AllTimeIndividualScore]] = {
    sql"""select email, sum(score) from all_time_individual_score group by email"""
      .query[AllTimeIndividualScore]
      .to[List]
      .transact(xa)
  }


}

