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

import akka.actor.ActorSystem
import com.typesafe.config.Config
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures
import play.api.http.{Status, HeaderNames => PlayHeaderNames}
import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.auth.core.retrieve.{CompositeRetrieval, EmptyRetrieval, SimpleRetrieval, ~}
import uk.gov.hmrc.auth.{Bar, Foo, TestPredicate1}
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost, HttpResponse}

import scala.concurrent.Future


class AuthConnectorSpec extends WordSpec with ScalaFutures {

  private trait Setup {

    implicit lazy val hc: HeaderCarrier = HeaderCarrier()

    def withStatus: Int = Status.OK

    def withHeaders: Map[String, String] = Map.empty

    def withBody: Option[JsValue] = None

    val authConnector: PlayAuthConnector = new PlayAuthConnector {

      override lazy val http: HttpPost = new HttpPost {

        override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
          val httpResponse = HttpResponse(withStatus, responseJson = withBody, responseHeaders = withHeaders.mapValues(Seq(_)))
          Future.successful(httpResponse)
        }

        override protected def actorSystem: ActorSystem = ActorSystem()

        override protected def configuration: Option[Config] = None

        override val hooks: Seq[HttpHook] = NoneRequired

        override def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

        override def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

        override def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???
      }

      override val serviceUrl: String = "/some-service"

    }

    def exceptionHeaders(value: String): Map[String, String] = Map(PlayHeaderNames.WWW_AUTHENTICATE -> s"""MDTP detail="$value"""")
  }

  private trait UnauthorisedSetup extends Setup {

    def headerMsg: String

    override def withStatus: Int = Status.UNAUTHORIZED

    override def withHeaders: Map[String, String] = exceptionHeaders(headerMsg)

  }

  "authorise" should {

    val fooRetrieval = SimpleRetrieval("fooProperty", Foo.reads)
    val barRetrieval = SimpleRetrieval("barProperty", Bar.reads)

    "return a successful future when a 200 is returned and no retrievals are supplied" in new Setup {
      val result: Future[Unit] = authConnector.authorise(TestPredicate1("aValue"), EmptyRetrieval)

      whenReady(result) { _ => () }
    }

    "return the correctly typed object when a 200 is returned and a retrieval is supplied" in new Setup {
      override val withBody = Some(Json.parse(
        """{
          | "fooProperty": {
          |   "value": "someValue"
          |  }
          |}
        """.stripMargin
      ))

      val result: Future[Foo] = authConnector.authorise(TestPredicate1("aValue"), fooRetrieval)

      whenReady(result) {
        theFoo => theFoo shouldBe Foo("someValue")
      }
    }

    "return the multiple correctly typed object when a 200 is returned and multiple retrievals are supplied" in new Setup {
      override val withBody = Some(Json.parse(
        """{
          | "fooProperty": {
          |   "value": "someValue"
          |  },
          | "barProperty": {
          |   "value": "someOtherValue",
          |   "number": 123
          |  }
          |}
        """.stripMargin
      ))

      val result: Future[Foo ~ Bar] = authConnector.authorise(TestPredicate1("aValue"), CompositeRetrieval(fooRetrieval, barRetrieval))

      whenReady(result) {
        case theFoo ~ theBar =>
          theFoo shouldBe Foo("someValue")
          theBar shouldBe Bar("someOtherValue", 123)

      }
    }

    "throw InsufficientConfidenceLevel on failed authorisation with appropriate header" in new UnauthorisedSetup {
      val headerMsg = "InsufficientConfidenceLevel"

      val result: Future[Unit] = authConnector.authorise(TestPredicate1("aValue"), EmptyRetrieval)

      whenReady(result.failed) {
        e => e shouldBe a[InsufficientConfidenceLevel]
      }
    }

    "throw InsufficientEnrolments on failed authorisation with appropriate header" in new UnauthorisedSetup {
      val headerMsg = "InsufficientEnrolments"

      val result: Future[Unit] = authConnector.authorise(TestPredicate1("aValue"), EmptyRetrieval)

      whenReady(result.failed) {
        e => e shouldBe a[InsufficientEnrolments]
      }
    }

    "throw BearerTokenExpired on failed authorisation with appropriate header" in new UnauthorisedSetup {
      val headerMsg = "BearerTokenExpired"

      val result: Future[Unit] = authConnector.authorise(TestPredicate1("aValue"), EmptyRetrieval)

      whenReady(result.failed) {
        e => e shouldBe a[BearerTokenExpired]
      }
    }

    "throw MissingBearerToken on failed authorisation with appropriate header" in new UnauthorisedSetup {
      val headerMsg = "MissingBearerToken"

      val result: Future[Unit] = authConnector.authorise(TestPredicate1("aValue"), EmptyRetrieval)

      whenReady(result.failed) {
        e => e shouldBe a[MissingBearerToken]
      }
    }

    "throw InvalidBearerToken on failed authorisation with appropriate header" in new UnauthorisedSetup {
      val headerMsg = "InvalidBearerToken"

      val result: Future[Unit] = authConnector.authorise(TestPredicate1("aValue"), EmptyRetrieval)

      whenReady(result.failed) {
        e => e shouldBe a[InvalidBearerToken]
      }
    }

    "throw SessionRecordNotFound on failed authorisation with appropriate header" in new UnauthorisedSetup {
      val headerMsg = "SessionRecordNotFound"

      val result: Future[Unit] = authConnector.authorise(TestPredicate1("aValue"), EmptyRetrieval)

      whenReady(result.failed) {
        e => e shouldBe a[SessionRecordNotFound]
      }
    }

    "throw InternalError on failed authorisation with unknown header message" in new UnauthorisedSetup {
      val headerMsg = "some-unknown-header-message"

      val result: Future[Unit] = authConnector.authorise(TestPredicate1("aValue"), EmptyRetrieval)

      whenReady(result.failed) {
        e => {
          e shouldBe a[InternalError]
          val internalError = e.asInstanceOf[InternalError]
          internalError.getMessage should include(headerMsg)
        }
      }
    }

    "throw InternalError on failed authorisation with invalid header" in new UnauthorisedSetup {
      val headerMsg = "some-invalid-header-value"

      override def exceptionHeaders(value: String): Map[String, String] = Map(PlayHeaderNames.WWW_AUTHENTICATE -> headerMsg)

      val result: Future[Unit] = authConnector.authorise(TestPredicate1("aValue"), EmptyRetrieval)

      whenReady(result.failed) {
        e => {
          e shouldBe a[InternalError]
          val internalError = e.asInstanceOf[InternalError]
          internalError.getMessage should include("InvalidResponseHeader")
        }
      }
    }

    "throw InternalError on failed authorisation with missing header" in new Setup {

      override val withStatus: Int = Status.UNAUTHORIZED

      val result: Future[Unit] = authConnector.authorise(TestPredicate1("aValue"), EmptyRetrieval)

      whenReady(result.failed) {
        e => {
          e shouldBe a[InternalError]
          val internalError = e.asInstanceOf[InternalError]
          internalError.getMessage should include("MissingResponseHeader")
        }
      }
    }
  }
}
