package com.knoldus.repository

import cats.data.OptionT
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.knoldus.model.ContributionType.ContributionType
import com.knoldus.model._
import com.typesafe.scalalogging.LazyLogging
import doobie.ConnectionIO
import doobie.util.transactor.Transactor
import doobie.util.{Read, Write}

import java.sql.Timestamp
import scala.concurrent.Future


trait ContributionScoreRepository {

  def get(id: Long)(implicit tenantId: Int): IO[Option[ContributionScore]]

  def getMonthlyContributions(month: Int, year: Int)(implicit tenantId: Int): IO[List[IndividualContributionTypeScore]]

  def store(contributionScore: ContributionScore)(implicit tenantId: Int): Future[Int]

  def all(implicit tenantId: Int): IO[Vector[ContributionScore]]

  def getMonthlyIndividualContributionScore(email: String, studioId: Int, month: Int, year: Int)(implicit tenantId: Int):
  ConnectionIO[Option[MonthlyIndividualWithStudioScore]]

  def getAllTimeContributionScoreForIndividual(email: String, studioId: Int)(implicit tenantId: Int): ConnectionIO[Option[AllTimeIndividualWithStudioScore]]

  def getMonthlyStudioContributionScore(studioId: Int, month: Int, year: Int)(implicit tenantId: Int):
  doobie.ConnectionIO[Option[MonthlyStudioScore]]

  def getAllTimeStudioContributionScore(studioId: Int)(implicit tenantId: Int): ConnectionIO[Option[AllTimeStudioScore]]

  def storeContributionScore(contributionScore: ContributionScore)(implicit tenantId: Int): ConnectionIO[Int]

  def getMonthlyIndividualContributionScores(month: Int, year: Int)(implicit tenantId: Int): IO[List[MonthlyIndividualScore]]

  def getAllIndividualContributionScores()(implicit tenantId: Int): IO[List[MonthlyIndividualScore]]

  def getAllTimeIndividualContributionScores()(implicit tenantId: Int): IO[List[AllTimeIndividualScore]]

  /**
   * Gets All Time Individual Monthly contribution data.
   *
   * @return response in the form list of IndividualContributionTypeScore.
   */
  def getAllTimeMonthlyContributions()(implicit tenantId: Int): IO[List[IndividualMonthlyContributionTypeScore]]

  def getDailyLeadersScore()(implicit tenantId: Int): IO[List[DailyScore]]

  /**
   * Gets contribution id in the form of MD5 Hash.
   *
   * @param md5Hash to get contribution id.
   * @return response in the form Optional IO of Integer.
   */
  def getContributionScoreForMd5Hash(md5Hash: String, contributionType: ContributionType,
                                     contributionId: String, email: String)(implicit tenantId: Int): IO[Option[Int]]
  def storeContributionForRejected(contributionScore: ContributionScore)(implicit tenantId: Int):ConnectionIO[Int]
}


/** Concrete implementation that is backed by a SQL database.
 *
 * Supports optimistic concurrency and validates the constraint that components have
 * exactly one parent.
 */
class ContributionScoreRepositoryImpl(xa: Transactor[IO]) extends ContributionScoreRepository with LazyLogging {

  import doobie.implicits._
  import doobie.implicits.javasql._

  // Vendor-specific error code for unique index constraints.
  val MySQLIntegrityError = "23000"
  val H2IntegrityError = "23505"


  /**
   * Gets contribution data from contribution_score table.
   *
   * @param id contribution id to get data.
   * @return response in the form of ContributionScore.
   */
  override def get(id: Long)(implicit tenantId: Int): IO[Option[ContributionScore]] = {
    val select =
      sql"""select id, full_name, email, contribution_id, contribution_type, title, contribution_date, technology_details,
            url_details, studio_name, studio_id, score from contribution_score where id = $id"""
        .query[ContributionScore]
        .option
    OptionT(select.transact(xa)).value
  }

  /**
   * Stores Contribution Score in Contribution_score table.
   *
   * @param contributionScore a case class that hold contribution details to store.
   * @return response in the form of Integer.
   */
  override def store(contributionScore: ContributionScore)(implicit tenantId: Int): Future[Int] = {
    logger.info("Persisting Information in the Database")
    val insertContributionScore: ConnectionIO[Int] =
      sql"""INSERT INTO contribution_score( full_name, email, contribution_id, contribution_type, title,
                 contribution_date, technology_details, url_details, studio_name, studio_id,  score, md5hash,tenant_id)
          VALUES (${contributionScore.fullName}, ${contributionScore.email}, ${contributionScore.contributionId},
          ${contributionScore.contributionType.toString},${contributionScore.title},${Timestamp.valueOf(contributionScore.contributionDate)},
          ${contributionScore.technologyDetails}, ${contributionScore.urlDetails}, ${contributionScore.studioName},
          ${contributionScore.studioId},${contributionScore.score}, ${contributionScore.md5Hash},${tenantId} )""".update.run

    logger.info("Insert Statement = " + insertContributionScore)
    insertContributionScore.transact(xa).unsafeToFuture()
  }

  /**
   * Stores Contribution Score in Contribution_score table.
   *
   * @param contributionScore a case class that hold contribution details to store.
   * @return response in the form IO connection of Integer.
   */
  override def storeContributionScore(contributionScore: ContributionScore)(implicit tenantId: Int): ConnectionIO[Int] = {
    logger.info(s"Persisting Contribution Score in the Database for $contributionScore for Tenant = $tenantId")
    val insertContributionScore: ConnectionIO[Int] =
      sql"""INSERT INTO contribution_score( full_name, email, contribution_id, contribution_type, title,
                 contribution_date, technology_details, url_details, studio_name, studio_id,  score, md5hash,tenant_id)
          VALUES (${contributionScore.fullName}, ${contributionScore.email}, ${contributionScore.contributionId},
          ${contributionScore.contributionType.toString},${contributionScore.title},${Timestamp.valueOf(contributionScore.contributionDate)},
          ${contributionScore.technologyDetails}, ${contributionScore.urlDetails}, ${contributionScore.studioName},
          ${contributionScore.studioId},${contributionScore.score}, ${contributionScore.md5Hash},${tenantId} )""".update.run

    logger.info("Insert Statement = " + insertContributionScore)
    insertContributionScore
  }

  /**
   * Gets contribution id in the form of MD5 Hash.
   *
   * @param md5Hash to get contribution id.
   * @return response in the form Optional IO of Integer.
   */
  override def getContributionScoreForMd5Hash(md5Hash : String, contributionType: ContributionType,
                                              contributionId : String, email : String)(implicit tenantId: Int): IO[Option[Int]] = {
    val select =
      sql"""select id from contribution_score where md5hash = $md5Hash or
           (contribution_type = ${contributionType.toString} and contribution_id = $contributionId  and email = $email) """
        .query[Int]
        .option
    OptionT(select.transact(xa)).value
  }

  /**
   * Gets all contribution data.
   *
   * @return response in the form Contribution Score.
   */
  override def all(implicit tenantId: Int): IO[Vector[ContributionScore]] = {
    sql"""select id, full_name, email, contribution_id, contribution_type, title, contribution_date, technology_details,
         url_details, studio_name, studio_id, score from contribution_score"""
      .query[ContributionScore]
      .to[Vector]
      .transact(xa)
  }

  /**
   * Gets Monthly contribution data.
   *
   * @param month to fetch that month data.
   * @param year to fetch that year data.
   * @return response in the form list of IndividualContributionTypeScore.
   */
  override def getMonthlyContributions(month: Int, year: Int)(implicit tenantId: Int): IO[List[IndividualContributionTypeScore]] = {

    implicit val contributionTypeScoreRead: Read[IndividualContributionTypeScore] = Read[(String, String, Double)].map {
      case (email, contributionType, score) =>
        IndividualContributionTypeScore(email, ContributionType.withName(contributionType), score)
    }

    implicit val contributionTypeScoreWrite: Write[IndividualContributionTypeScore] = Write[(String, String, Double)].
      contramap { contriScore =>
        (contriScore.email, contriScore.contributionType.toString, contriScore.score)
      }


    val query =
      sql"""select email, contribution_type, sum(score) from contribution_score
           where extract(year from contribution_date)  = ${year}
           and extract(month from contribution_date) = ${month}
           group by email, contribution_type;""".query[IndividualContributionTypeScore]
    val findAllMonthlyContributions = query.to[List]
    findAllMonthlyContributions.transact(xa)
  }



  /**
   * Gets All Time Individual Monthly contribution data.
   *
   * @return response in the form list of IndividualContributionTypeScore.
   */
  override def getAllTimeMonthlyContributions()(implicit tenantId: Int): IO[List[IndividualMonthlyContributionTypeScore]] = {

    implicit val contributionTypeScoreRead: Read[IndividualMonthlyContributionTypeScore] = Read[(String, Int, Int,
      String, Double)].map {
      case (email, month, year, contributionType, score) =>
        IndividualMonthlyContributionTypeScore(email, month, year, ContributionType.withName(contributionType), score)
    }

    implicit val contributionTypeScoreWrite: Write[IndividualMonthlyContributionTypeScore] = Write[(String, Int, Int,
      String, Double)].
      contramap { contriScore =>
        (contriScore.email, contriScore.month, contriScore.year, contriScore.contributionType.toString, contriScore.score)
      }


    val query =
      sql"""select email, extract(month from contribution_date) as month,
            extract(year from contribution_date) as year, contribution_type, sum(score) from contribution_score
           group by email, contribution_type, month, year order by month, year;""".query[IndividualMonthlyContributionTypeScore]
    val findAllMonthlyContributions = query.to[List]
    findAllMonthlyContributions.transact(xa)
  }

  /**
   * Gets Monthly Individual contribution score.
   *
   * @param email id to get individual score.
   * @param studioId studio id of user.
   * @param month to fetch that month data.
   * @param year to fetch that year data.
   * @return response in the form list of IndividualContributionTypeScore.
   */
  override def getMonthlyIndividualContributionScore(email :String, studioId : Int, month: Int, year: Int)(implicit tenantId: Int):
  doobie.ConnectionIO[Option[MonthlyIndividualWithStudioScore]] = {
    sql"""select email, studio_id, sum(score), extract(month from contribution_date) as month,
          extract(year from contribution_date) as year from contribution_score
          where extract(year from contribution_date)  = ${year}
           and extract(month from contribution_date) = ${month}
           and email =${email}
           and studio_id =${studioId}
           group by email, studio_id, month, year""".query[MonthlyIndividualWithStudioScore].option
  }

  /**
   * Gets Monthly Individual contribution score.
   *
   * @param month to fetch that month data.
   * @param year to fetch that year data.
   * @return response in the form IO of list of IndividualContributionTypeScore.
   */
  override def getMonthlyIndividualContributionScores(month: Int, year: Int)(implicit tenantId: Int): IO[List[MonthlyIndividualScore]] = {
    val query = sql"""select email, sum(score), extract(month from contribution_date) as month,
          extract(year from contribution_date) as year from contribution_score
          where extract(year from contribution_date)  = ${year}
           and extract(month from contribution_date) = ${month}
           group by email, month, year""".query[MonthlyIndividualScore]
    query.to[List].transact(xa)

  }

  /**
   * Gets All time Individual contribution score.
   *
   * @return response in the form of IO of list of IndividualContributionTypeScore.
   */
  override def getAllIndividualContributionScores()(implicit tenantId: Int): IO[List[MonthlyIndividualScore]] = {
    val query = sql"""select email,  sum(score), extract(month from contribution_date) as month,
          extract(year from contribution_date) as year from contribution_score
           group by email, month, year""".query[MonthlyIndividualScore]
    query.to[List].transact(xa)

  }


  /**
   * Gets Monthly Individual contribution score.
   *
   * @param studioId studio id to get data.
   * @param month to fetch that month data.
   * @param year to fetch that year data.
   * @return response in the form list of IndividualContributionTypeScore.
   */
  override def getMonthlyStudioContributionScore(studioId :Int, month: Int, year: Int)(implicit tenantId: Int):
  ConnectionIO[Option[MonthlyStudioScore]] = {
    sql"""select  studio_id,  sum(score), extract(month from contribution_date) as month,
          extract(year from contribution_date) as year from contribution_score
           where extract(year from contribution_date)  = ${year}
           and extract(month from contribution_date) = ${month}
           and studio_id =${studioId} group by studio_id, month, year""".query[MonthlyStudioScore].option
  }

  /**
   * Gets All time individual contribution score.
   *
   * @param email email id.
   * @param studioId studio id to get data.
   * @return response in the form AllTimeIndividualWithStudioScore.
   */
  override def getAllTimeContributionScoreForIndividual(email :String, studioId : Int)(implicit tenantId: Int):
  ConnectionIO[Option[AllTimeIndividualWithStudioScore]] = {
    sql"""select email, studio_id, sum(score) from contribution_score
           where email = ${email} and studio_id =  ${studioId} group by email, studio_id"""
      .query[AllTimeIndividualWithStudioScore].option
  }

  /**
   * Gets All time Individual contribution score.
   *
   * @return response in the form list of AllTimeIndividualScore.
   */
  override def getAllTimeIndividualContributionScores()(implicit tenantId: Int): IO[List[AllTimeIndividualScore]] = {
    val query = sql"""select email, sum(score) from contribution_score group by email"""
      .query[AllTimeIndividualScore]
    query.to[List].transact(xa)
  }


  /**
   * Gets All time studio contribution score.
   *
   * @param studioId to fetch studio contribution data.
   * @return response in the form option of AllTimeIndividualScore.
   */
  override def getAllTimeStudioContributionScore(studioId :Int)(implicit tenantId: Int): ConnectionIO[Option[AllTimeStudioScore]] = {
    sql"""select  studio_id,  sum(score) from contribution_score
           where studio_id =${studioId} group by studio_id""".query[AllTimeStudioScore].option
  }

  override def getDailyLeadersScore()(implicit tenantId: Int): IO[List[DailyScore]] = {
    import java.time.LocalDate
    val today = LocalDate.now
    val yesterday = today.minusDays(1)

    val query = sql"""select  email, full_name,  sum(score) as daily_score from contribution_score where
           extract(day from contribution_date) =  ${yesterday.getDayOfMonth}
           and  extract(month from contribution_date) =  ${yesterday.getMonthValue}
            and  extract(year from contribution_date) =   ${yesterday.getYear}  group by email,
            full_name order by daily_score desc limit 5""".query[DailyScore]
    query.to[List].transact(xa)
  }

  override def storeContributionForRejected(contributionScore: ContributionScore)(implicit tenantId: Int): ConnectionIO[Int] = {

    val insertContributionScore: ConnectionIO[Int] =
      sql"""INSERT INTO contribution_score(full_name, email, contribution_id, contribution_type, title,
                  contribution_date, technology_details, url_details, studio_name, studio_id,  score, md5hash,tenant_id)
            VALUES (${contributionScore.fullName}, ${contributionScore.email}, ${contributionScore.contributionId},
            ${contributionScore.contributionType.toString}, ${contributionScore.title}, ${Timestamp.valueOf(contributionScore.contributionDate)},
            ${contributionScore.technologyDetails}, ${contributionScore.urlDetails}, ${contributionScore.studioName},
            ${contributionScore.studioId}, ${contributionScore.score}, ${contributionScore.md5Hash}, $tenantId)""".update.run

    logger.info("Insert Statement = " + insertContributionScore)
    insertContributionScore
  }
}

