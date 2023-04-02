package com.knoldus.swagger

import akka.http.scaladsl.server.{Directives, Route}

trait SwaggerSite extends Directives  {
  val swaggerSite: Route =
    path("swagger") { getFromResource("swagger/index.html") } ~
      getFromResourceDirectory("swagger")
}
