package com.knoldus.resources


import akka.http.scaladsl.server.Route
import com.knoldus.model.{Pessoa, Status}
import com.knoldus.routing.ResourceManager
import com.knoldus.service.PessoaService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse

import javax.ws.rs.core.MediaType
import javax.ws.rs.{POST, Path, Produces}
import scala.concurrent.duration._

trait PessoaResource extends ResourceManager with JsonSupport {

  val pessoaService: PessoaService

  def pessoaRoutes: Route =
    registerRoute ~ path("register") {
      post {
        entity(as[Pessoa]) { pessoa =>
          val result = for {
            result <- pessoaService.upsert(pessoa)
          } yield Status(result)
          complete(result)
        }
      }
    } ~ listRoute ~ path("list") {
      post {
        complete(pessoaService.list)
      }
    } ~ searchRoute ~ path("search") {
      post {
        extractStrictEntity(1.second) { entity =>
          val id = entity.data.utf8String.toLong
          complete(pessoaService.select(id))
        }
      }
    } ~ removeRoute ~ path("remove") {
      post {
        extractStrictEntity(1.second) { entity =>
          val id = entity.data.utf8String.toLong
          val result = for {
            result <- pessoaService.remove(id)
          } yield Status(result)
          complete(result)
        }
      }
    }

  @Path("/register")
  @POST
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Store Manager Service",
    description = "Store Manager Service.",
    method = "POST",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Ok"),
      new ApiResponse(responseCode = "500", description = "Service Not Available")
    )
  )
  val registerRoute: Route =    path("register") {
    post {
      complete("Ok")
    }
  }

  @Path("/list")
  @POST
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "list Service",
    description = "List Manager Service.",
    method = "POST",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Ok"),
      new ApiResponse(responseCode = "500", description = "Service Not Available")
    )
  )
  val listRoute: Route = path("list"){
    post {
      complete("Ok")
    }
  }

  @Path("/search")
  @POST
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Search Service",
    description = "Search Service.",
    method = "POST",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Ok"),
      new ApiResponse(responseCode = "500", description = "Service Not Available")
    )
  )
  val searchRoute: Route = path("search"){
    post {
      complete("Ok")
    }
  }


  @Path("/remove")
  @POST
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(summary = "Remove Service",
    description = "Remove Service.",
    method = "POST",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Ok"),
      new ApiResponse(responseCode = "500", description = "Service Not Available")
    )
  )
  val removeRoute: Route = path("remove"){
    post {
      complete("Ok")
    }
  }
}
