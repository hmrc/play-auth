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

package uk.gov.hmrc.auth.otac

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import play.api.mvc.Session
import uk.gov.hmrc.auth.Await
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpResponse}

import scala.concurrent.Future


class OtacAuthorisationConnectorSpec extends WordSpec with ScalaFutures with Matchers with Await with MockitoSugar {

  val httpMock: HttpGet = mock[HttpGet]

  class StubOtacAuthorisationConnectorFunctions extends PlayOtacAuthConnector {
    val http: HttpGet = httpMock
    override val serviceUrl: String = ""
  }
  val headerCarrier = HeaderCarrier()

  "PlayOtacAuthConnector" should {

    "return NoOtacTokenInSession if there is no otacToken in the session" in {

      val session: Session = Session(Map("" -> ""))
      val re = await(new StubOtacAuthorisationConnectorFunctions().authorise("testService", headerCarrier, session))

      re shouldBe NoOtacTokenInSession
    }

    "return Authorised if has valid token" in {

      when(httpMock.GET[HttpResponse](any())(any(), any[HeaderCarrier], any())).thenReturn(Future.successful(HttpResponse(200)))

      val session: Session = Session(Map("otacToken" -> "some_otacToken"))
      val re = await(new StubOtacAuthorisationConnectorFunctions().authorise("testService", headerCarrier, session))

      re shouldBe Authorised
    }

    "return Unauthorised if has invalid token" in {

      when(httpMock.GET[HttpResponse](any())(any(), any[HeaderCarrier], any())).thenReturn(Future.successful(HttpResponse(401)))

      val session: Session = Session(Map("otacToken" -> "some_otacToken"))
      val re = await(new StubOtacAuthorisationConnectorFunctions().authorise("testService", headerCarrier, session))

      re shouldBe Unauthorised
    }

  }

}
