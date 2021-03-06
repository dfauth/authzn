package com.github.dfauth.auth

import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Directive1, Rejection}
import akka.http.scaladsl.server.Directives._
import com.github.dfauth.authzn.{CompanyImpl, ImmutableSubject, Subject, User, AuthenticationContext, AuthenticationContextImpl, UserModel, UserModelImpl}
import com.github.dfauth.authzn.PrincipalType._
import com.github.dfauth.jwt.JWTVerifier
import com.typesafe.scalalogging.LazyLogging
import io.vavr.control.Try.{Failure, Success}

import scala.collection.JavaConverters._

object Directives extends LazyLogging {

  private def extractBearerToken(authHeader: Option[String]): Option[String] =
    authHeader.filter(_.startsWith("Bearer ")).map(token => token.substring("Bearer ".length))

  private def bearerToken: Directive1[Option[String]] =
    for {
      authBearerHeader <- optionalHeaderValueByName("Authorization").map(extractBearerToken)
//      xAuthCookie <- optionalCookie("X-Authorization-Token").map(_.map(_.value))
    } yield authBearerHeader //.orElse(xAuthCookie)

  def authRejection: Rejection = AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, HttpChallenge("", ""))

  def authenticate(verifier:JWTVerifier): Directive1[AuthenticationContext[_ <:UserModel]] = {
    var user:User = null
    bearerToken.flatMap {
      case Some(token) => {
        verifier.authenticateToken(token, verifier.asUser) match {
          case s:Success[User] => provide(new AuthenticationContextImpl(token,
            new UserModelImpl(s.get.getUserId,
              new CompanyImpl(s.get.getCompanyId),
              s.get.getRoles)));
          case f:Failure[User] => reject(authRejection)
        }
      }
      case None => reject(authRejection)
    }
  }

  val toSubject: User => Subject = u => {
    val s = ImmutableSubject.of(USER.of(u.getUserId))
    u.getRoles.asScala.map(r => ROLE.of(r.getRolename)).foldLeft(s) { (s, p) =>
      s.`with`(p)
    }
  }
}
