package com.knoldus.resources

import akka.http.scaladsl.server.Route
import com.knoldus.model._
import com.knoldus.routing.ResourceManager
import com.knoldus.service.scoring.ScoringService
import com.typesafe.scalalogging.LazyLogging
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs._



trait ContributionResource extends ResourceManager with JsonSupport with LazyLogging {

  val scoringService: ScoringService

  def scoringRoutes: Route = {

    healthRoute ~ calculateScoreForContribution ~ monthlyScores ~ getAllTimeIndividualScores ~
      getMonthlyIndividualScores ~ getAllMonthlyStudioScores ~ getAllTimeStudioScores ~ allTimeContributionScores ~
      getDailyTopScorers
  }

  /**
   * Checks Health of Scoring Service.
   *
   * @return OK if the service is working fine.
   */
  @GET
  @Path("/health")
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Health Check for Service",
    description = "Get Health of the Service",
    method = "GET",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Ok")
    )
  )
  def healthRoute: Route = path("health") {
    get {
      complete("OK")
    }
  }

  @POST
  @Path("/contribution")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Calculate Score for contribution",
    description = "Calculate Score for a particular contribution",
    method = "POST",
    requestBody = new RequestBody(required = true,
      content = Array(new Content(schema = new Schema(implementation = classOf[Contribution])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Score")
    )
  )
  def calculateScoreForContribution: Route = path("contribution") {
    post {
      headerValueByName("TenantId") {
        tenantId => {
          entity(as[ContributionWithStatus]) { contribution =>
            logger.info(contribution.toString)
            val result = for {
              result <- scoringService.calculateScore(contribution)(tenantId.toInt)
            } yield Status(result)
            complete(result)

          }
        }
      }
    }
  }

  @GET
  @Path("/contribution")
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Get ContributionType Scores for all individuals for a month",
    description = "Get scores aggregated by type of contribution for all individuals for a particular month",
    method = "GET",
    parameters = Array(
      new Parameter(name = "month", in = ParameterIn.QUERY, required = true, description = "Month",
        content = Array(new Content(schema = new Schema(implementation = classOf[String], allowableValues = Array())))),
      new Parameter(name = "year", in = ParameterIn.QUERY, required = true, description = "Year",
        content = Array(new Content(schema = new Schema(implementation = classOf[String], allowableValues = Array()))))
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Individual Contribution Type Scores",
        content = Array(new Content(array = new ArraySchema(schema =
          new Schema(implementation = classOf[AllContributionTypeScores]))))),
      new ApiResponse(responseCode = "500", description = "Internal Server Error")
    )
  )
  def monthlyScores: Route = path("contribution") {
    get {
      headerValueByName("TenantId") {
        tenantId => {
          parameters("month", "year") {
            case (month, year) => complete(scoringService.getMonthlyScores(month.toInt, year.toInt)(tenantId.toInt))
          }
        }
      }
    }
  }


  @GET
  @Path("/contribution/getAllTimeMonthlyContributionScores")
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Get lifetime ContributionType Scores for all individuals",
    description = "Get lifetime scores aggregated by type of contribution for all individuals",
    method = "GET",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Individual Contribution Type Scores",
        content = Array(new Content(array = new ArraySchema(schema =
          new Schema(implementation = classOf[AllMonthlyContributionTypeScores]))))),
      new ApiResponse(responseCode = "500", description = "Internal Server Error")
    )
  )
  def allTimeContributionScores: Route = path("contribution" / "getAllTimeMonthlyContributionScores") {
    get {
      headerValueByName("TenantId") {
        tenantId => {
          complete(scoringService.getAllTimeMonthlyScores()(tenantId.toInt))
        }
      }
    }
  }


  @GET
  @Path("/contribution")
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Get All Time Individual Scores",
    description = "Get all time scores of all individuals",
    method = "GET",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "All Time Individual Scores",
        content = Array(new Content(array = new ArraySchema(schema =
          new Schema(implementation = classOf[AllTimeIndividualScore]))))),
      new ApiResponse(responseCode = "200", description = "Ok")
    )
  )
  def getAllTimeIndividualScores: Route = path("contribution") {
    get {
      headerValueByName("TenantId") {
        tenantId =>
          complete(scoringService.getAllTimeIndividualScores()(tenantId.toInt))
      }
    }
  }


  @GET
  @Path("/contribution/dailytopscorers")
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Get Daily Top Scorer",
    description = "Get Daily Top Scorer",
    method = "GET",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Daily Top Scorers",
        content = Array(new Content(array = new ArraySchema(schema =
          new Schema(implementation = classOf[DailyScore]))))),
      new ApiResponse(responseCode = "200", description = "Ok")
    )
  )
  def getDailyTopScorers: Route = path("contribution" / "dailytopscorers") {
    get {
      {
        headerValueByName("TenantId") {
          tenantId => {
            complete(scoringService.getTopDailyScores()(tenantId.toInt))
          }
        }
      }
    }
  }

  @GET
  @Path("/contribution/individual")
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Get Monthly Individual Scores",
    description = "Get monthly scores of all individuals for a particular month",
    method = "GET",
    parameters = Array(
      new Parameter(name = "month", in = ParameterIn.QUERY, required = true, description = "Month",
        content = Array(new Content(schema = new Schema(implementation = classOf[String], allowableValues = Array())))),
      new Parameter(name = "year", in = ParameterIn.QUERY, required = true, description = "Year",
        content = Array(new Content(schema = new Schema(implementation = classOf[String], allowableValues = Array()))))
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Monthly Individual Scores",
        content = Array(new Content(array = new ArraySchema(schema =
          new Schema(implementation = classOf[MonthlyIndividualScore]))))),
      new ApiResponse(responseCode = "500", description = "Internal Server Error")
    )
  )
  def getMonthlyIndividualScores: Route = path("contribution" / "individual") {
    get {
      headerValueByName("TenantId") {
        tenantId => {
          parameters("month", "year") {
            case (month, year) => complete(scoringService.getAllMonthlyIndividualScores(month.toInt, year.toInt)(tenantId.toInt))
          }
        }
      }
    }
  }

  @GET
  @Path("/contribution/studio")
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Get Monthly Studio Scores",
    description = "Get monthly aggregated scores of all studios for a particular month",
    method = "GET",
    parameters = Array(
      new Parameter(name = "month", in = ParameterIn.QUERY, required = true, description = "Month",
        content = Array(new Content(schema = new Schema(implementation = classOf[String], allowableValues = Array())))),
      new Parameter(name = "year", in = ParameterIn.QUERY, required = true, description = "Year",
        content = Array(new Content(schema = new Schema(implementation = classOf[String], allowableValues = Array()))))
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Monthy Studio Scores",
        content = Array(new Content(array = new ArraySchema(schema =
          new Schema(implementation = classOf[MonthlyStudioScore]))))),
      new ApiResponse(responseCode = "500", description = "Internal Server Error")
    )
  )
  def getAllMonthlyStudioScores: Route = path("contribution" / "studio") {
    get {
      headerValueByName("TenantId") {
        tenantId => {
          parameters("month", "year") {
            case (month, year) => complete(scoringService.getAllMonthlyStudioScores(month.toInt, year.toInt)(tenantId.toInt))
          }

        }
      }
    }
  }

  @GET
  @Path("/contribution/studio")
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Get All Time Studio Scores",
    description = "Get aggregated scores for all studio",
    method = "GET",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "All Time Studio Scores",
        content = Array(new Content(array = new ArraySchema(schema =
          new Schema(implementation = classOf[AllTimeStudioScore]))))),
      new ApiResponse(responseCode = "500", description = "Internal Server Error")
    )
  )
  def getAllTimeStudioScores: Route = path("contribution" / "studio") {
    get {
      {
        headerValueByName("TenantId") {
          tenantId => {
            complete(scoringService.getAllTimeStudioScores()(tenantId.toInt))
          }
        }
      }
    }
  }
}