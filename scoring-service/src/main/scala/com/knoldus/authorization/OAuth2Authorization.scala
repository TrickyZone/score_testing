package com.knoldus.authorization

import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.Directives.{reject, _}
import akka.http.scaladsl.server._
import com.knoldus.authorization.OAuth2Authorization.keycloakDeployment
import com.typesafe.scalalogging.Logger
import org.keycloak.adapters.{KeycloakDeployment, KeycloakDeploymentBuilder}

import scala.jdk.CollectionConverters.CollectionHasAsScala

class OAuth2Authorization(logger: Logger, tokenVerifier: TokenVerifier) {

  /**
   * Authorizes token with user role.
   *
   * @param role to authorize user.
   * @return  a verified token.
   */
  def authorizeTokenWithRole(role: String): Directive1[VerifiedToken] = {
    authorizeToken flatMap {
      case t if t.roles.contains(role) => provide(t)
      case _ => reject(AuthorizationFailedRejection).toDirective[Tuple1[VerifiedToken]]
    }
  }

  /**
   * Authorizes token.
   *
   * @return  a verified token.
   */
  def authorizeToken: Directive1[VerifiedToken] = {
    bearerToken.flatMap {
      case Some(token) =>
        onComplete(tokenVerifier.verifyToken(token)).flatMap {
          _.map { t =>
            val clientRoles = t.getResourceAccess(keycloakDeployment.getResourceName).getRoles
            provide(VerifiedToken(token, t.getId, t.getName, t.getPreferredUsername, t.getEmail,
              clientRoles.asScala.toSeq))
          }.recover {
            case ex: Throwable =>
              logger.error("Authorization Token could not be verified", ex)
              reject(AuthorizationFailedRejection).toDirective[Tuple1[VerifiedToken]]
          }.getOrElse(reject(AuthorizationFailedRejection))
        }
      case None =>
        reject(AuthorizationFailedRejection)
    }
  }

  /**
   * Generates a bearer token.
   *
   * @return response in the form of optional string.
   */
  private def bearerToken: Directive1[Option[String]] =
    for {
      authBearerHeader <- optionalHeaderValueByType(classOf[Authorization]).map(extractBearerToken)
      xAuthCookie <- optionalCookie("X-Authorization-Token").map(_.map(_.value))
    } yield authBearerHeader.orElse(xAuthCookie)

  /**
   * Extracting bearer token.
   *
   * @param authHeader take auth header to extract bearer token.
   * @return response in the form of optional string.
   */
  private def extractBearerToken(authHeader: Option[Authorization]): Option[String] =
    authHeader.collect {
      case Authorization(OAuth2BearerToken(token)) => token
    }

}

object OAuth2Authorization {
  def apply(l: Logger, tv: TokenVerifier): OAuth2Authorization =
    new OAuth2Authorization(l, tv)

  val keycloakDeployment: KeycloakDeployment = KeycloakDeploymentBuilder.build(
    getClass.getResourceAsStream("/keycloak.json"))

}

