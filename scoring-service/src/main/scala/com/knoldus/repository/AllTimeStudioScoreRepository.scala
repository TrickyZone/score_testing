package com.knoldus.repository

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.knoldus.model.AllTimeStudioScore
import com.typesafe.scalalogging.LazyLogging
import doobie.util.transactor.Transactor

import scala.concurrent.Future


trait AllTimeStudioScoreRepository {

  def getAllTimeStudioScore(studioId: Int)(implicit tenantId: Int): doobie.ConnectionIO[Option[AllTimeStudioScore]]


  def store(allTimeStudioScore: AllTimeStudioScore)(implicit tenantId: Int): Future[Int]

  // Returns all Improvements in the repository. Obviously this is drastically over-simplified: no pagination,
  // filtering, sorting, etc.
  def allStudioAllTimeScores(implicit tenantId: Int): IO[List[AllTimeStudioScore]]

  def insert(allTimeStudioScore: AllTimeStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int]

  def update(allTimeStudioScore: AllTimeStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int]

  def safeInsert(studioScore: AllTimeStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int]
}


/** Concrete implementation that is backed by a SQL database.
 *
 * Supports optimistic concurrency and validates the constraint that components have
 * exactly one parent.
 */
class AllTimeStudioScoreRepositoryImpl(xa: Transactor[IO]) extends AllTimeStudioScoreRepository with LazyLogging {

  import doobie.implicits._

  /**
   * Gets All time studio Score from all_time_studio_score table.
   *
   * @param studioId studio's id to get score.
   * @return response All time studio score with studio id.
   */
  override def getAllTimeStudioScore(studioId :Int)(implicit tenantId: Int): doobie.ConnectionIO[Option[AllTimeStudioScore]] = {
    sql"""select studio_id, score from all_time_studio_score where studio_id = ${studioId} """
      .query[AllTimeStudioScore]
      .option
  }

  /**
   * Stores All time studio Score in  all_time_studio_score table.
   *
   * @param allTimeStudioScore a case class that hold score.
   * @return response in the form of future of Integer.
   */
  override def store(allTimeStudioScore: AllTimeStudioScore)(implicit tenantId: Int): Future[Int] = {
    logger.info("Persisting Information in the Database")
    val insertAllTimeScore: doobie.ConnectionIO[Int] =
      sql"""INSERT INTO all_time_studio_score(studio_id, score,tenant_id)
          VALUES ( ${allTimeStudioScore.studioId}, ${allTimeStudioScore.score}, ${tenantId})""".update.run

    logger.info("Insert Statement = " + insertAllTimeScore)
    insertAllTimeScore.transact(xa).unsafeToFuture()
  }

  /**
   * Stores All time studio Score in  all_time_studio_score table.
   *
   * @param allTimeStudioScore a case class that hold score.
   * @return response in the form IO Connection of Integer.
   */
  override def insert(allTimeStudioScore: AllTimeStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int] = {
    logger.info("Persisting Information in the Database")
    sql"""INSERT INTO all_time_studio_score(studio_id, score,tenant_id)
          VALUES ( ${allTimeStudioScore.studioId}, ${allTimeStudioScore.score},${tenantId})""".update.run
  }

  /**
   * Updates All time Individual Score in  all_time_studio_score table.
   *
   * @param allTimeStudioScore a case class that stores values to update.
   * @return response in the form of future of Integer.
   */
  override def update(allTimeStudioScore: AllTimeStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int] = {
    logger.info("Update All Time Studio Score")
    sql"""  update all_time_studio_score set score = ${allTimeStudioScore.score}
          where studio_id = ${allTimeStudioScore.studioId} and tenant_id = ${tenantId}""".update.run
  }

  /**
   * Gets All time studio Score from all_time_studio_score table.
   *
   * @return response in the form of list of All time individual score.
   */
  override def allStudioAllTimeScores(implicit tenantId: Int) : IO[List[AllTimeStudioScore]] = {
    sql"""select studio_id, score from all_time_studio_score"""
      .query[AllTimeStudioScore]
      .to[List]
      .transact(xa)
  }

  /**
   * Calls methods of insert and update of all time studio score.
   *
   * @param studioScore a case class that hold score.
   * @return response in the form IO Connection of Integer.
   */
  override def safeInsert(studioScore: AllTimeStudioScore)(implicit tenantId: Int): doobie.ConnectionIO[Int] = {
    getAllTimeStudioScore(studioScore.studioId)
      .flatMap(score =>
        (score match {
          case Some(_) => update(studioScore)
          case None => insert(studioScore)
        })
          .map(updated => updated)
      )
  }


}

