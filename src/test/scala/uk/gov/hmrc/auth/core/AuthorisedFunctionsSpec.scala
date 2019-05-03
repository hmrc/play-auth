/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.auth.core

import org.scalatest.Matchers._
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, SimpleRetrieval, ~}
import uk.gov.hmrc.auth.{Bar, Foo, TestPredicate1}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class AuthorisedFunctionsSpec extends WordSpec with ScalaFutures {

  private trait Setup extends AuthorisedFunctions {

    implicit lazy val hc: HeaderCarrier = HeaderCarrier()

    def success: Any = ()

    def exception: Option[AuthorisationException] = None

    val authConnector: AuthConnector = new AuthConnector {
      def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier): Future[A] = {
        exception.fold(Future.successful(success.asInstanceOf[A]))(Future.failed(_))
      }
    }
  }

  "AuthorisedFunction" should {
    "execute the supplied body when authorisation succeeds and no predicates supplied" in new Setup {
      def testFunction(): Future[String] = authorised() {
        Future("works")
      }

      val result: Future[String] = testFunction()
      whenReady(result) {
        str => str shouldBe "works"
      }
    }

    "execute the supplied body when authorisation succeeds for a supplied predicate" in new Setup {
      def testFunction(): Future[String] = authorised(TestPredicate1("someValue")) {
        Future("works")
      }

      val result: Future[String] = testFunction()
      whenReady(result) {
        str => str shouldBe "works"
      }
    }

    "return a failed future when authorisation fails" in new Setup {

      override def exception = Some(new MissingBearerToken)

      def testFunction(): Future[String] = authorised(TestPredicate1("someValue")) {
        Future("works")
      }

      val result: Future[String] = testFunction()
      whenReady(result.failed) {
        e => e shouldBe a[MissingBearerToken]
      }
    }
  }

  "AuthorisedFunctionWithResult" should {

    val simplePredicate = TestPredicate1("someValue")
    val fooRetrieval = SimpleRetrieval("fooProperty", Foo.reads)
    val barRetrieval = SimpleRetrieval("barProperty", Bar.reads)

    "execute the supplied body with retrieved object when authorisation succeeds and no predicates supplied" in new Setup {
      override val success = Foo("foo message")

      def testFunction(): Future[Foo] = authorised().retrieve(fooRetrieval) {
        implicit foo =>
          Future(foo)
      }

      val result: Future[Foo] = testFunction()
      whenReady(result) {
        _ shouldBe Foo("foo message")
      }
    }

    "execute the supplied body with retrieved object when authorisation succeeds and predicates supplied" in new Setup {
      override val success = Foo("foo message")

      def testFunction(): Future[Foo] = authorised(simplePredicate).retrieve(fooRetrieval) {
        implicit foo =>
          Future(foo)
      }

      val result: Future[Foo] = testFunction()
      whenReady(result) {
        _ shouldBe Foo("foo message")
      }
    }

    "execute the supplied body with retrieved objects when authorisation succeeds and predicates supplied" in new Setup {
      override def success = new ~(Foo("foo message"), Bar("bar message", 123))

      def testFunction(): Future[(Foo, Bar)] = authorised(simplePredicate).retrieve(fooRetrieval and barRetrieval) {
        case foo ~ bar => Future((foo, bar))
      }

      val result: Future[(Foo, Bar)] = testFunction()
      whenReady(result) {
        case (foo, bar) =>
          foo shouldBe Foo("foo message")
          bar shouldBe Bar("bar message", 123)
      }
    }

    "return a failed future when authorisation fails" in new Setup {

      override def exception = Some(new MissingBearerToken)

      def testFunction(): Future[Foo] = authorised(simplePredicate).retrieve(fooRetrieval) {
        implicit foo =>
          Future(foo)
      }

      val result: Future[Foo] = testFunction()
      whenReady(result.failed) {
        e => e shouldBe a[MissingBearerToken]
      }
    }
  }
}
