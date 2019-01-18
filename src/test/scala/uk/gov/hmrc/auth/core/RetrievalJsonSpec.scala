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

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.{JsError, Json}
import uk.gov.hmrc.auth.core.authorise._
import uk.gov.hmrc.auth.core.retrieve._

class RetrievalJsonSpec extends WordSpec with ScalaFutures {

  "The JSON reads for the internalId retrieval" should {

    "read a populated id" in {
      val json = Json.parse("""{ "internalId": "xyz" }""")

      Retrievals.internalId.reads.reads(json).get shouldBe Some("xyz")
    }

    "read a null property as None" in {
      val json = Json.parse("""{ "internalId": null }""")

      Retrievals.internalId.reads.reads(json).get shouldBe None
    }

    "read a missing property as None" in {
      val json = Json.parse("""{ "internalXX": "xyz" }""")

      Retrievals.internalId.reads.reads(json).get shouldBe None
    }
  }

  "The JSON reads for the externalId retrieval" should {

    "read a populated id" in {
      val json = Json.parse("""{ "externalId": "xyz" }""")

      Retrievals.externalId.reads.reads(json).get shouldBe Some("xyz")
    }

    "read a null property as None" in {
      val json = Json.parse("""{ "externalId": null }""")

      Retrievals.externalId.reads.reads(json).get shouldBe None
    }

    "read a missing property as None" in {
      val json = Json.parse("""{ "externalXX": "xyz" }""")

      Retrievals.externalId.reads.reads(json).get shouldBe None
    }
  }

  "The JSON reads for the authProviderId retrieval" should {

    "read a GG credId" in {
      val json = Json.parse("""{ "authProviderId": { "ggCredId": "xyz" }}""")

      Retrievals.authProviderId.reads.reads(json).get shouldBe GGCredId("xyz")
    }

    "read a Verify pid" in {
      val json = Json.parse("""{ "authProviderId": { "verifyPid": "xyz" }}""")

      Retrievals.authProviderId.reads.reads(json).get shouldBe VerifyPid("xyz")
    }

    "read a PAC clientId" in {
      val json = Json.parse("""{ "authProviderId": { "paClientId": "xyz" }}""")

      Retrievals.authProviderId.reads.reads(json).get shouldBe PAClientId("xyz")
    }

    "read a OneTimeLogin" in {
      val json = Json.parse("""{ "authProviderId": { "oneTimeLogin": "" }}""")

      Retrievals.authProviderId.reads.reads(json).get shouldBe OneTimeLogin
    }

    "produce an error for unknown credential types" in {
      val json = Json.parse("""{ "authProviderId": { "fooBar": "xyz" }}""")

      Retrievals.authProviderId.reads.reads(json) shouldBe a[JsError]
    }
  }

  "The JSON reads for the userDetailsUri retrieval" should {

    "read a populated id" in {
      val json = Json.parse("""{ "userDetailsUri": "/user-details" }""")

      Retrievals.userDetailsUri.reads.reads(json).get shouldBe Some("/user-details")
    }

    "read a null property as None" in {
      val json = Json.parse("""{ "userDetailsUri": null }""")

      Retrievals.userDetailsUri.reads.reads(json).get shouldBe None
    }

    "read a missing property as None" in {
      val json = Json.parse("""{ "userDetailsXXX": "/user-details" }""")

      Retrievals.userDetailsUri.reads.reads(json).get shouldBe None
    }
  }

  "The JSON reads for the affinityGroup retrieval" should {

    "read an Individual affinity group" in {
      val json = Json.parse("""{ "affinityGroup": "Individual" }""")

      Retrievals.affinityGroup.reads.reads(json).get shouldBe Some(AffinityGroup.Individual)
    }

    "read an Organisation affinity group" in {
      val json = Json.parse("""{ "affinityGroup": "Organisation" }""")

      Retrievals.affinityGroup.reads.reads(json).get shouldBe Some(AffinityGroup.Organisation)
    }

    "read an Agent affinity group" in {
      val json = Json.parse("""{ "affinityGroup": "Agent" }""")

      Retrievals.affinityGroup.reads.reads(json).get shouldBe Some(AffinityGroup.Agent)
    }

    "produce an error for unknown credential types" in {
      val json = Json.parse("""{ "affinityGroup": "Bartender" }""")

      Retrievals.affinityGroup.reads.reads(json) shouldBe a[JsError]
    }

  }

  "The JSON reads for the loginTimes retrieval" should {

    val currentLogin = new DateTime(2015, 1, 1, 12, 0).withZone(DateTimeZone.UTC)
    val previousLogin = new DateTime(2012, 1, 1, 12, 0).withZone(DateTimeZone.UTC)

    "read login times with a previous login" in {
      val json = Json.parse("""{ "loginTimes": { "currentLogin": "2015-01-01T12:00:00.000Z", "previousLogin": "2012-01-01T12:00:00.000Z" }}""")

      Retrievals.loginTimes.reads.reads(json).get shouldBe LoginTimes(currentLogin, Some(previousLogin))
    }

    "read login times without a previous login" in {
      val json = Json.parse("""{ "loginTimes": { "currentLogin": "2015-01-01T12:00:00.000Z" }}""")

      Retrievals.loginTimes.reads.reads(json).get shouldBe LoginTimes(currentLogin, None)
    }

    "read login times without a previous login as null" in {
      val json = Json.parse("""{ "loginTimes": { "currentLogin": "2015-01-01T12:00:00.000Z", "previousLogin": null }}""")

      Retrievals.loginTimes.reads.reads(json).get shouldBe LoginTimes(currentLogin, None)
    }

  }

  "The JSON reads for the enrolments retrieval" should {

    val enrolments = Set(
      Enrolment("ENROL-A", Seq(EnrolmentIdentifier("ID-A", "123")), "Activated", ConfidenceLevel.L100),
      Enrolment("ENROL-B", Seq(EnrolmentIdentifier("ID-B", "456")), "Activated", ConfidenceLevel.L0)
    )

    def enrolmentsJson(retrieve: String) = Json.parse(
      s"""
         |{ "$retrieve": [
         |  {
         |    "key": "ENROL-A",
         |    "identifiers": [{"key":"ID-A","value":"123"}],
         |    "state": "Activated",
         |    "confidenceLevel": 100
         |  },
         |  {
         |    "key": "ENROL-B",
         |    "identifiers": [{"key":"ID-B","value":"456"}],
         |    "state": "Activated"
         |  }
         |]}
         |""".stripMargin)

    "read all enrolments" in {
      val json = enrolmentsJson("allEnrolments")

      Retrievals.allEnrolments.reads.reads(json).get shouldBe Enrolments(enrolments)
    }

    "read authorised enrolments" in {
      val json = enrolmentsJson("authorisedEnrolments")

      Retrievals.authorisedEnrolments.reads.reads(json).get shouldBe Enrolments(enrolments)
    }


  }

  "The JSON reads for multiple retrievals" should {

    "read multiple result" in {

      val json = Json.parse(
        """
          |{
          |  "internalId": "123",
          |  "externalId": "456",
          |  "userDetailsUri": "/user-details"
          |}
        """.stripMargin)

      val retrieval = Retrievals.internalId and Retrievals.externalId and Retrievals.userDetailsUri

      retrieval.reads.reads(json).get match {
        case Some(internalId) ~ Some(externalId) ~ Some(userDetailsUri) =>
          internalId shouldBe "123"
          externalId shouldBe "456"
          userDetailsUri shouldBe "/user-details"
      }
    }

  }


}
