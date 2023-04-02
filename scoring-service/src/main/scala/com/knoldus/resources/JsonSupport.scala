package com.knoldus.resources

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.knoldus.model.ContributionStatus.ContributionStatus
import com.knoldus.model.ContributionType.ContributionType
import com.knoldus.model.DeliverableType.DeliverableType
import com.knoldus.model.{AllContributionTypeScores, AllMonthlyContributionTypeScores, AllTimeIndividualScore, AllTimeIndividualWithStudioScore, AllTimeStudioScore, Contribution, ContributionStatus, ContributionType, ContributionTypeScore, ContributionWithStatus, DailyScore, DeliverableType, IndividualContributionScores, IndividualContributionTypeScore, IndividualMonthlyContributionTypeScore, MonthlyIndividualScore, MonthlyIndividualWithStudioScore, MonthlyStudioScore, Pessoa, Status, updateContribution}
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat}

import scala.util.{Failure, Success, Try}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  def enumJsonFormat[T <: Enumeration](enum: T): RootJsonFormat[T#Value] =
    new RootJsonFormat[T#Value] {
      override def write(t: T#Value): JsValue = JsString(t.toString)

      override def read(json: JsValue): T#Value =
        json match {
          case JsString(string) =>
            Try(enum.withName(string)) match {
              case Success(e) => e
              case Failure(_) => throw DeserializationException(s"Unexpected feature string $string")
            }
          case any => throw DeserializationException(s"Expected to read String type, received $any")
        }
    }

  implicit val ContributionTypeJsonFormat: RootJsonFormat[ContributionType] = enumJsonFormat(ContributionType)

  implicit val statusFormat = jsonFormat1(Status)
  implicit val pessoaFormat = jsonFormat3(Pessoa)

  implicit val contributionFormat: RootJsonFormat[Contribution] = jsonFormat10(Contribution)
  implicit val contributionStatusJsonFormat: RootJsonFormat[ContributionStatus] = enumJsonFormat(ContributionStatus)
  implicit val deliverableFormatJsonFormat: RootJsonFormat[DeliverableType] = enumJsonFormat(DeliverableType)
  implicit val contributionFormat2: RootJsonFormat[ContributionWithStatus] = jsonFormat13(ContributionWithStatus)
  implicit val updateContributionFormat:RootJsonFormat[updateContribution] = jsonFormat2(updateContribution)

  implicit val individualContributionTypeScoreFormat: RootJsonFormat[IndividualContributionTypeScore] =
    jsonFormat3(IndividualContributionTypeScore)

  implicit val individualMonthlyContributionTypeScoreFormat: RootJsonFormat[IndividualMonthlyContributionTypeScore] =
    jsonFormat5(IndividualMonthlyContributionTypeScore)

  implicit val contributionTypeScoreFormat: RootJsonFormat[ContributionTypeScore] =
    jsonFormat2(ContributionTypeScore)


  implicit val individualContributionScoresFormat: RootJsonFormat[IndividualContributionScores] =
    jsonFormat3(IndividualContributionScores)

  implicit val allContributionTypeScoresFormat: RootJsonFormat[AllContributionTypeScores] =
    jsonFormat2(AllContributionTypeScores)

  implicit val allMonthlyContributionTypeScoresFormat: RootJsonFormat[AllMonthlyContributionTypeScores] =
    jsonFormat4(AllMonthlyContributionTypeScores)

  implicit val monthlyIndividualWithStudioScoreFormat: RootJsonFormat[MonthlyIndividualWithStudioScore] =
    jsonFormat5(MonthlyIndividualWithStudioScore)

  implicit val monthlyIndividualScoreFormat: RootJsonFormat[MonthlyIndividualScore] =
    jsonFormat4(MonthlyIndividualScore)

  implicit val monthlyStudioScoreFormat: RootJsonFormat[MonthlyStudioScore] =
    jsonFormat4(MonthlyStudioScore)


  implicit val allTimeIndividualWithStudioScoreFormat: RootJsonFormat[AllTimeIndividualWithStudioScore] =
    jsonFormat3(AllTimeIndividualWithStudioScore)

  implicit val allTimeIndividualScoreFormat: RootJsonFormat[AllTimeIndividualScore] =
    jsonFormat2(AllTimeIndividualScore)

  implicit val allTimeStudioScoreFormat: RootJsonFormat[AllTimeStudioScore] =
    jsonFormat2(AllTimeStudioScore)

  implicit val dailyScoreFormat: RootJsonFormat[DailyScore] =
    jsonFormat3(DailyScore)

}

