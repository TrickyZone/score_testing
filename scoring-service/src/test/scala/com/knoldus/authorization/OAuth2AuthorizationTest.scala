package com.knoldus.authorization

import akka.http.scaladsl.server
import com.knoldus.dbTest.TestEmbeddedPostgres
import com.typesafe.scalalogging.Logger
import org.keycloak.adapters.KeycloakDeployment
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class OAuth2AuthorizationTest extends AnyFlatSpec with Matchers with TestEmbeddedPostgres {
  val keycloakDeployment = new KeycloakDeployment
  val keycloakTokenVerifier = new KeycloakTokenVerifier(keycloakDeployment)
  val oAuth2Authorization = new OAuth2Authorization(logger: Logger, keycloakTokenVerifier)

  it should "authorizeTokenWithRole" in {
    val authorizedToken = oAuth2Authorization.authorizeTokenWithRole("admin")
    assert(authorizedToken.isInstanceOf[server.Directive1[VerifiedToken]])
  }
}
