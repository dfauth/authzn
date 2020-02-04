package com.github.dfauth.auth

import java.security.KeyPair

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import com.github.dfauth.jwt.{JWTBuilder, JWTVerifier, KeyPairFactory}
import com.github.dfauth.auth.Directives._
import com.github.dfauth.authzn.{AuthorizationPolicyImpl, AuthorizationPolicyMonadImpl}
import com.github.dfauth.authzn
import com.github.dfauth.authzn.PrincipalType._
import com.github.dfauth.authzn
import com.typesafe.scalalogging.LazyLogging

import scala.util.{Failure, Success}

object Routes extends LazyLogging {

  val issuer = "me"
  val keyPair: KeyPair = KeyPairFactory.createKeyPair("RSA", 2048)
  val jwtBuilder = new JWTBuilder(issuer,keyPair.getPrivate)
  val jwtVerifier = new JWTVerifier(keyPair.getPublic, issuer)
  val policy = AuthorizationPolicyMonadImpl(new authzn.Directive(ROLE.of("user")))
  val permission = new TestPermission()


  def hello(jwtVerifier: JWTVerifier) =
    path("hello") {
      get {
        authenticate(jwtVerifier) { userCtx =>
          policy.permit(userCtx.payload(), permission) {
            // extract username
            userCtx.userId()
          } match {
            case Success(s) => complete(HttpEntity(ContentTypes.`application/json`, s"""{"say": "hello to authenticated ${s}"}"""))
            case Failure(t) => complete(StatusCodes.Unauthorized)
          }
        }
      }
    }

}

class TestPermission() extends authzn.Permission