package com.github.dfauth.auth

import java.time.ZonedDateTime

import com.github.dfauth.jwt.{JWTBuilder, JWTVerifier, KeyPairFactory}
import com.github.dfauth.auth.RestEndPointServer._
import com.github.dfauth.authzn.Role._
import com.github.dfauth.authzn.User
import com.typesafe.scalalogging.LazyLogging
import io.restassured.RestAssured
import org.testng.annotations.Test
import io.restassured.RestAssured._
import org.hamcrest.Matchers._
import org.scalatest.testng.TestNGSuite

import scala.concurrent.Await
import scala.concurrent.duration._

class ServerSpec extends TestNGSuite with LazyLogging {

  val host = "localhost"
  val port = 0
  val issuer = "me"

  RestAssured.useRelaxedHTTPSValidation()

  @Test(groups = Array("rest"))
  def testToken():Unit = {

    val testKeyPair = KeyPairFactory.createKeyPair("RSA", 2048)
    val jwtVerifier = new JWTVerifier(testKeyPair.getPublic, issuer)


    import com.github.dfauth.auth.Routes._
    val endPoint = RestEndPointServer(hello(jwtVerifier), host, port)
    val bindingFuture = endPoint.start()

    val binding = Await.result(bindingFuture, 5.seconds)

    try {
      val userId: String = "fred"

      val jwtBuilder = new JWTBuilder(this.issuer, testKeyPair.getPrivate)
      val user = User.of(userId, "flintstone", role("test:admin"), role("test:user"))
      val token = jwtBuilder.forSubject(user.getUserId).withClaim("roles", user.getRoles).withClaim("companyId", "flintstone").withExpiry(ZonedDateTime.now().plusMinutes(20)).build()
      given().header("Authorization", "Bearer "+token).
        when().log().headers().
        get(endPointUrl(binding, "hello")).
        then().
        statusCode(200).
        body("say",equalTo(s"hello to authenticated ${user.getUserId}"))
    } finally {
      endPoint.stop(bindingFuture)
    }
  }

  @Test(groups = Array("rest"))
  def testForbidden():Unit = {

    val testKeyPair = KeyPairFactory.createKeyPair("RSA", 2048)
    val jwtVerifier = new JWTVerifier(testKeyPair.getPublic, issuer)


    import com.github.dfauth.auth.Routes._
    val endPoint = RestEndPointServer(hello(jwtVerifier), host, port)
    val bindingFuture = endPoint.start()

    val binding = Await.result(bindingFuture, 5.seconds)

    try {
      val userId: String = "fred"

      val jwtBuilder = new JWTBuilder(this.issuer, testKeyPair.getPrivate)
      val user = User.of(userId, "flintstone", role("test:user")) // this endpoint requires the 'admin' role
      val token = jwtBuilder.forSubject(user.getUserId).withClaim("roles", user.getRoles).withClaim("companyId", "flintstone").withExpiry(ZonedDateTime.now().plusMinutes(20)).build()
      given().header("Authorization", "Bearer "+token).
        when().log().headers().
        get(endPointUrl(binding, "hello")).
        then().
        statusCode(403)
    } finally {
      endPoint.stop(bindingFuture)
    }
  }

  @Test
  def testTokenFail():Unit = {

    val testKeyPair = KeyPairFactory.createKeyPair("RSA", 2048)
    val jwtVerifier = new JWTVerifier(testKeyPair.getPublic, issuer)


    import com.github.dfauth.auth.Routes._
    val endPoint = RestEndPointServer(hello(jwtVerifier), host, port)
    val bindingFuture = endPoint.start()

    val binding = Await.result(bindingFuture, 5.seconds)

    try {
      val userId: String = "fred"

      val jwtBuilder = new JWTBuilder(this.issuer,testKeyPair.getPrivate)
      val user = User.of(userId, "flintstone", role("test:admin"), role("test:user"))
      val token = jwtBuilder.forSubject(user.getUserId).withClaim("roles", user.getRoles).withClaim("companyId", "flintstone").build()
      val token1 = token.map(_ match {
        case 'a' => 'z'
        case 'z' => 'a'
        case c => c
      })
      given().header("Authorization", "Bearer "+token1).
        when().log().headers().
        get(endPointUrl(binding, "hello")).
        then().
        statusCode(401)
    } finally {
      endPoint.stop(bindingFuture)
    }
  }

}

