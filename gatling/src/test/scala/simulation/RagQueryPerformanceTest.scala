package simulation

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class RagQueryPerformanceTest extends Simulation {
  
  val httpProtocol = http
    .baseUrl("http://localhost:8083")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Performance Test")
    .acceptEncodingHeader("gzip, deflate")

  val questions = List(
    """{"question":"什么是机器学习？","contextId":"ml-001"}""",
    """{"question":"如何优化数据库查询性能？","contextId":"db-001"}""",
    """{"question":"Spring Boot最佳实践有哪些？","contextId":"spring-001"}"""
  )

  val scn = scenario("RAG Query Load Test")
    .exec(http("rag_query")
      .post("/api/v1/rag/query")
      .body(StringBody(questions(random.nextInt(questions.length))))
      .asJson
      .check(status.is(200))
      .check(responseTimeInMillis.lte(3000))
      .check(jsonPath("$.answer").exists)
    )

  setUp(
    scn.inject(
      constantUsersPerSec(10) during (60 seconds),
      rampUsersPerSec(10) to 100 during (120 seconds)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.max.lt(3000),
      global.responseTime.percentile3.lt(2500),
      global.successfulRequests.percent.gt(95),
      global.requestsPerSec.gt(50)
    )
    .maxDuration(300 seconds)
}
