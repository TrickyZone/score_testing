package com.knoldus.model

import com.knoldus.model
import com.knoldus.model.ContributionStatus.ContributionStatus
import com.knoldus.model.ContributionType.ContributionType
import com.knoldus.model.DeliverableType.DeliverableType
import doobie.Read
import doobie.util.Write

case class Contribution(contributionId: String,  fullName: Option[String],
                        email: String, contributionType: ContributionType,
                        title: String, contributionDate: String, technologyDetails: Option[String],
                        urlDetails: Option[String], studioId: Int,
                        studioName: Option[String])

case class DeliverableTypeWithCount(count: Int, deliverableType: String)

case class ContributionWithStatus(contributionId: String, fullName: Option[String],
                                  email: String, contributionType: ContributionType,
                                  title: String, contributionDate: String, technologyDetails: Option[String], status: ContributionStatus,
                                  urlDetails: Option[String], studioId: Int,
                                  studioName: Option[String],deliverableContributioncount: Option[Int],
                                  deliverableType: Option[DeliverableType])

case class ContributionScore(id : Option[Int], contributionId: Option[String],  fullName: Option[String],
                        email: String, contributionType: ContributionType,
                        title: String, contributionDate: String, technologyDetails: Option[String],
                        urlDetails: Option[String], studioId: Int,
                        studioName: Option[String], score : Double, md5Hash : String)



object ContributionScore {

  implicit val contributionScoreRead : Read[ContributionScore] = Read[(Int, String, String, String, String, String,
    String, String, String,  Int, String, Double, String  )].map {
    case(id,contributionId, fullName, email, contributionType, title, date, tech,url, studioId, studioName, score, md5Hash) =>
      ContributionScore(Some(id), Some(contributionId), Some(fullName), email,
        ContributionType.withName(contributionType),title, date, Some(tech), Some(url), studioId, Some(studioName), score, md5Hash )
  }

  implicit val contributionScoreWrite: Write[ContributionScore] = Write[(Int, String, String, String, String, String,
    String, String, String,  Int, String, Double, String  )].contramap { contriScore =>
    (contriScore.id.getOrElse(0), contriScore.contributionId.getOrElse("NA"), contriScore.fullName.getOrElse("NA"), contriScore.email,
      contriScore.contributionType.toString, contriScore.title, contriScore.contributionDate,
    contriScore.technologyDetails.getOrElse("Unknown"), contriScore.urlDetails.getOrElse("Unknown"),
      contriScore.studioId, contriScore.studioName.getOrElse("Unknown"),
      contriScore.score, contriScore.md5Hash)
  }
}

case class IndividualContributionTypeScore(email: String, contributionType: ContributionType.Value, score: Double)

case class IndividualMonthlyContributionTypeScore(email: String, month: Int, year: Int,
                                                  contributionType: ContributionType.Value, score: Double)

case class ContributionTypeScore (contributionType: ContributionType.Value, score : Double)

case class AllContributionTypeScores(email: String, scores: List[ContributionTypeScore])

case class AllMonthlyContributionTypeScores(email: String,month : Int, year :Int, scores: List[ContributionTypeScore])

case class IndividualContributionScores(email: String, studioId: Int,
                                        contributionTypeScores : Map[ContributionType.Value,Double])

final case class updateContribution(contribution: ContributionWithStatus,tenantId:Int)




object ContributionType extends Enumeration {

  type ContributionType = Value

  val BLOG: model.ContributionType.Value = Value("Blog")
  val KNOLX: model.ContributionType.Value = Value("Knolx")
  val WEBINAR: model.ContributionType.Value = Value("Webinar")
  val TECHHUB: model.ContributionType.Value = Value("Tech hub")
  val PODCAST: model.ContributionType.Value = Value("Podcast")
  val OPEN_SOURCE: model.ContributionType.Value = Value("Open source")
  val CONFERENCE: model.ContributionType.Value = Value("Conference")
  val BOOKS: model.ContributionType.Value = Value("Book")
  val RESEARCH_PAPER: model.ContributionType.Value = Value("Research paper")
  val MEETUP: model.ContributionType.Value = Value("Meetup")
  val PMO_TEMPLATES: model.ContributionType.Value = Value("PMO Template")
  val PROCESSED_DOCUMENTS: model.ContributionType.Value = Value("Process Document")
  val PROPOSAL: model.ContributionType.Value = Value("Proposal")
  val CERTIFICATION : model.ContributionType.Value= Value("Certification")
  val KNOLX_SPOTLIGHT_MEMBER : model.ContributionType.Value= Value("Spotlight Member")
  val KNOLX_ATTENDEE : model.ContributionType.Value= Value("Knolx Attendee")
  val OTHER: model.ContributionType.Value = Value("Other")
  val INVALID: model.ContributionType.Value = Value("INVALID CONTRIBUTION TYPE")
  val ONLINE_COURSE : model.ContributionType.Value= Value("Online Course")

}

object ContributionStatus extends Enumeration {

  type ContributionStatus = Value

  val APPROVED: model.ContributionStatus.Value = Value("APPROVED")
  val PENDING: model.ContributionStatus.Value = Value("PENDING")
  val REJECTED: model.ContributionStatus.Value = Value("REJECTED")

}

object DeliverableType extends Enumeration {

  type DeliverableType = Value
  val VIDEOS = Value("Videos")
  val PRESENTATION = Value("Presentation")
  val DOCUMENT = Value("Document")
  val PODCAST = Value("Podcast")
  val INVALID = Value("INVALID DELIVERABLE TYPE")

}