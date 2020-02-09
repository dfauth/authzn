package com.github.dfauth.authzn;

import akka.http.scaladsl.Http;
import akka.http.scaladsl.server.RequestContext;
import akka.http.scaladsl.server.RouteResult;
import com.github.dfauth.auth.jwrap.RestEndPointServer;
import com.github.dfauth.jwt.JWTBuilder;
import com.github.dfauth.jwt.JWTVerifier;
import com.github.dfauth.jwt.KeyPairFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import scala.Function1;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import java.security.KeyPair;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import static com.github.dfauth.authzn.Role.role;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;


public class TestCase {

    private static final Logger logger = LoggerFactory.getLogger(TestCase.class);

    String host = "localhost";
    int port = 0;
    String issuer = "me";

    KeyPair testKeyPair = KeyPairFactory.createKeyPair("RSA", 2048);
    JWTVerifier jwtVerifier = new JWTVerifier(testKeyPair.getPublic(), issuer);

    @Test
    public void testIt() throws Exception {

     Function1<RequestContext, Future<RouteResult>> route = com.github.dfauth.auth.Routes.hello(jwtVerifier);
     RestEndPointServer endPoint = new RestEndPointServer(route, host, port);
     Future<Http.ServerBinding> bindingFuture = endPoint.start();

     Http.ServerBinding binding = (Http.ServerBinding) Await.result(bindingFuture, new FiniteDuration(5, TimeUnit.SECONDS));


     try {
      String userId = "fred";

      JWTBuilder jwtBuilder = new JWTBuilder(this.issuer, testKeyPair.getPrivate());
      User user = User.of(userId, "flintstone", role("test:admin"), role("test:user"));
      String token = jwtBuilder.forSubject(user.getUserId()).withClaim("roles", user.getRoles()).withExpiry(ZonedDateTime.now().plusMinutes(20)).build();
      given().header("Authorization", "Bearer "+token).
              when().log().headers()
              .get(com.github.dfauth.auth.RestEndPointServer.endPointUrl(binding, "hello", "http"))
              .then()
              .statusCode(200)
              .body("say",equalTo("hello to authenticated "+user.getUserId()));
     } finally {
      endPoint.stop(bindingFuture);
     }
    }
}


/**

 val host = "localhost"
 val port = 0
 val issuer = "me"

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
 val user = User.of(userId, role("test:admin"), role("test:user"))
 val token = jwtBuilder.forSubject(user.getUserId).withClaim("roles", user.getRoles).withExpiry(ZonedDateTime.now().plusMinutes(20)).build()
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
 val user = User.of(userId, role("test:user")) // this endpoint requires the 'admin' role
 val token = jwtBuilder.forSubject(user.getUserId).withClaim("roles", user.getRoles).withExpiry(ZonedDateTime.now().plusMinutes(20)).build()
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
 val user = User.of(userId, role("test:admin"), role("test:user"))
 val token = jwtBuilder.forSubject(user.getUserId).withClaim("roles", user.getRoles).build()
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



 */