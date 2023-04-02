package com.knoldus.swagger

import com.knoldus.common.Configuration
import com.knoldus.resources.ContributionResource
import io.swagger.v3.oas.models.security.SecurityScheme

object SwaggerDocService extends SwaggerHttpUiService {

  override val apiClasses: Set[Class[_]] = Set(classOf[ContributionResource])
  val config = Configuration.serviceConf
  val schemeForUrl = config.swaggerConf.scheme
  override val host = config.swaggerConf.url //the url of your api, not swagger's json endpoint
  override val schemes = List(schemeForUrl)
  val securityScheme = new SecurityScheme
  securityScheme.setType(SecurityScheme.Type.APIKEY)
  securityScheme.setScheme("bearer")
  securityScheme.bearerFormat("JWT")
  securityScheme.setIn(SecurityScheme.In.HEADER)
  override val securitySchemes = Map("bearerAuth" -> securityScheme)
  override val unwantedDefinitions = Seq("Function1", "Function1RequestContextFutureRouteResult")
}
