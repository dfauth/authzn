package com.github.dfauth.authzn

import com.github.dfauth.authzn.PrincipalType._
import com.github.dfauth.authzn.Assertions.WasRunAssertion
import com.github.dfauth.authzn.TestUtils.TestAction._
import com.github.dfauth.authzn.TestUtils.{TestAction, TestPermission}
import com.typesafe.scalalogging.LazyLogging
import org.testng.Assert
import org.testng.annotations.Test

import scala.util.{Failure, Success}

class AuthorizationSpec extends LazyLogging {

  @Test
  def testIt() = {

    val resource: String = "/a/b/c/d"
    val subject = ImmutableSubject.of(USER.of("fred"), ROLE.of("admin"), ROLE.of("user"))
    val perm = new TestPermission(resource, TestAction.READ)
    val directive = new Directive(ROLE.of("superuser"), new ResourcePath(resource), ActionSet.ALL_ACTIONS)
    val policy = AuthorizationPolicyMonadImpl(directive)

    val testPerm = new TestPermission("/a/b/c/d/e/f/g", READ)

    {
      policy.permit(subject, testPerm) {
        new WasRunAssertion().run
      } match {
        case Success(a) => {
          logger.error(s"expected authzn failure, received: ${a}")
          Assert.fail(s"expected authzn failure, received: ${a}")
        }
        case Failure(t) => {
          Assert.assertEquals(t.getMessage, "user: fred roles: [admin, user] is not authorized to perform actions [READ] on resource /a/b/c/d/e/f/g")
        }
      }
    }

    {
      // add super user role
      val subject1 = subject.`with`(ROLE.of("superuser"))
      policy.permit(subject1, testPerm) {
        new WasRunAssertion().run
      } match {
        case Success(a) => Assert.assertTrue(a.wasRun())
        case Failure(t) => Assert.fail(s"Expected assertion to run, instead received exception: ${t}", t)
      }
    }
  }
}
