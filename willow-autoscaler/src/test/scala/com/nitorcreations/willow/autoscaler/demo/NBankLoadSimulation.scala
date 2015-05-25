package com.nitorcreations.willow.autoscaler.demo;

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
//import io.gatling.jdbc.Predef._

class NBankLoadSimulation extends Simulation {

	val httpProtocol = http
		.baseURL("http://nbank-willow-795063877.eu-west-1.elb.amazonaws.com")
		.inferHtmlResources(BlackList(""".*\.js""", """.*\.css""", """.*\.gif""", """.*\.jpeg""", """.*\.jpg""", """.*\.ico""", """.*\.woff""", """.*\.(t|o)tf""", """.*\.png"""), WhiteList(""".*nbank-willow-795063877.eu-west-1.elb.amazonaws.com.*"""))
		.acceptHeader("application/json, text/plain, */*")
		.acceptEncodingHeader("gzip, deflate, sdch")
		.acceptLanguageHeader("en-US,en;q=0.8,fi;q=0.6")
		.contentTypeHeader("application/json;charset=UTF-8")
		.userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.65 Safari/537.36")

	val headers_0 = Map(
		"Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
		"Pragma" -> "no-cache")

	val headers_1 = Map(
		"Accept" -> "image/webp,*/*;q=0.8",
		"Pragma" -> "no-cache")

	val headers_2 = Map("Pragma" -> "no-cache")

	val headers_3 = Map(
		"Accept" -> "*/*",
		"Accept-Encoding" -> "identity;q=1, *;q=0",
		"Pragma" -> "no-cache",
		"Range" -> "bytes=0-")

	val headers_5 = Map(
		"Origin" -> "http://nbank-willow-795063877.eu-west-1.elb.amazonaws.com",
		"Pragma" -> "no-cache")

	val headers_55 = Map(
		"Accept" -> "text/html, */*; q=0.01",
		"Pragma" -> "no-cache",
		"X-Requested-With" -> "XMLHttpRequest")

    val uri1 = "http://www.google-analytics.com/collect"
    val uri2 = "http://nbank-willow-795063877.eu-west-1.elb.amazonaws.com"

	val scn = scenario("RecordedSimulation")
		.forever() {
		 exec(http("request_0")
			.get("/")
			.headers(headers_0)
			.resources(http("request_1")
			.get(uri1 + "?v=1&_v=j36&a=382507126&t=pageview&_s=1&dl=http%3A%2F%2Fnbank-willow-795063877.eu-west-1.elb.amazonaws.com%2F&ul=en-us&de=UTF-8&sd=24-bit&sr=1440x900&vp=1440x437&je=1&fl=17.0%20r0&_u=AACAAAABI~&jid=&cid=1867679005.1432490104&tid=UA-XXXXX-X&z=1668566792")
			.headers(headers_1),
            http("request_2")
			.get(uri2 + "/views/login.html")
			.headers(headers_2),
            http("request_3")
			.get(uri2 + "/sounds/rejected.wav")
			.headers(headers_3),
            http("request_4")
			.get(uri2 + "/sounds/money.wav")
			.headers(headers_3)))
		.pause(1)
		.exec(http("request_5")
			.put("/authenticationsystem/v1/login")
			.headers(headers_5)
			.body(RawFileBody("RecordedSimulation_0005_request.txt"))
			.resources(http("request_6")
			.get(uri2 + "/clientsystem/v1/clients/2")
			.headers(headers_2),
            http("request_7")
			.get(uri2 + "/views/main.html")
			.headers(headers_2),
            http("request_8")
			.get(uri2 + "/loansystem/v1/loans?clientId=2")
			.headers(headers_2),
            http("request_9")
			.get(uri2 + "/creditapplicationsystem/v1/applications?clientId=2")
			.headers(headers_2),
            http("request_10")
			.get(uri2 + "/depositsystem/v1/accounts?clientId=2")
			.headers(headers_2)))
		.pause(1)
		.exec(http("request_11")
			.get("/creditapplicationsystem/v1/applications?clientId=2")
			.headers(headers_2)
			.resources(http("request_12")
			.get(uri2 + "/depositsystem/v1/accounts?clientId=2")
			.headers(headers_2),
            http("request_13")
			.get(uri2 + "/loansystem/v1/loans?clientId=2")
			.headers(headers_2)))
		.pause(1)
		.exec(http("request_14")
			.get("/creditapplicationsystem/v1/applications?clientId=2")
			.headers(headers_2)
			.resources(http("request_15")
			.get(uri2 + "/depositsystem/v1/accounts?clientId=2")
			.headers(headers_2),
            http("request_16")
			.get(uri2 + "/loansystem/v1/loans?clientId=2")
			.headers(headers_2)))
		.pause(1)
		.exec(http("request_17")
			.get("/creditapplicationsystem/v1/applications?clientId=2")
			.headers(headers_2)
			.resources(http("request_18")
			.get(uri2 + "/loansystem/v1/loans?clientId=2")
			.headers(headers_2),
            http("request_19")
			.get(uri2 + "/depositsystem/v1/accounts?clientId=2")
			.headers(headers_2),
            http("request_20")
			.get(uri2 + "/views/creditapplication/new.html")
			.headers(headers_2)))
		.pause(4)
		.exec(http("request_21")
			.get("/creditapplicationsystem/v1/applications?clientId=2")
			.headers(headers_2)
			.resources(http("request_22")
			.get(uri2 + "/depositsystem/v1/accounts?clientId=2")
			.headers(headers_2),
            http("request_23")
			.get(uri2 + "/loansystem/v1/loans?clientId=2")
			.headers(headers_2)))
		.pause(1)
		.exec(http("request_24")
			.put("/nflow/v1/workflow-instance")
			.headers(headers_5)
			.body(RawFileBody("RecordedSimulation_0024_request.txt"))
			.resources(http("request_25")
			.get(uri2 + "/views/creditapplication/done.html")
			.headers(headers_2)))
		.pause(1)
		.exec(http("request_26")
			.get("/creditapplicationsystem/v1/applications?clientId=2")
			.headers(headers_2)
			.resources(http("request_27")
			.get(uri2 + "/loansystem/v1/loans?clientId=2")
			.headers(headers_2),
            http("request_28")
			.get(uri2 + "/depositsystem/v1/accounts?clientId=2")
			.headers(headers_2)))
		.pause(1)
		.exec(http("request_29")
			.get("/creditapplicationsystem/v1/applications?clientId=2")
			.headers(headers_2)
			.resources(http("request_30")
			.get(uri2 + "/depositsystem/v1/accounts?clientId=2")
			.headers(headers_2),
            http("request_31")
			.get(uri2 + "/loansystem/v1/loans?clientId=2")
			.headers(headers_2)))
		.pause(1)
		.exec(http("request_32")
			.get("/loansystem/v1/loans?clientId=2")
			.headers(headers_2)
			.resources(http("request_33")
			.get(uri2 + "/creditapplicationsystem/v1/applications?clientId=2")
			.headers(headers_2),
            http("request_34")
			.get(uri2 + "/depositsystem/v1/accounts?clientId=2")
			.headers(headers_2)))
		.pause(1)
		.exec(http("request_35")
			.get("/clerk")
			.headers(headers_0)
			.resources(http("request_36")
			.get(uri1 + "?v=1&_v=j36&a=1266569885&t=pageview&_s=1&dl=http%3A%2F%2Fnbank-willow-795063877.eu-west-1.elb.amazonaws.com%2Fclerk%2F&ul=en-us&de=UTF-8&sd=24-bit&sr=1440x900&vp=1440x437&je=1&fl=17.0%20r0&_u=AACAAAABI~&jid=&cid=1867679005.1432490104&tid=UA-XXXXX-X&z=1132050296")
			.headers(headers_1),
            http("request_37")
			.get(uri2 + "/clerk/views/main.html")
			.headers(headers_2),
            http("request_38")
			.get(uri2 + "/clerk/views/login.html")
			.headers(headers_2),
            http("request_39")
			.get(uri2 + "/clerk/sounds/notice.wav")
			.headers(headers_3)))
		.pause(1)
		.exec(http("request_40")
			.put("/authenticationsystem/v1/login")
			.headers(headers_5)
			.body(RawFileBody("RecordedSimulation_0040_request.txt"))
			.resources(http("request_41")
			.get(uri2 + "/clerksystem/v1/clerks/1")
			.headers(headers_2),
            http("request_42")
			.get(uri2 + "/creditapplicationsystem/v1/applications")
			.headers(headers_2),
            http("request_43")
			.get(uri2 + "/clientsystem/v1/clients")
			.headers(headers_2),
            http("request_44")
			.get(uri2 + "/nflow/v1/workflow-instance?type=creditDecision&state=manualDecision&businessKey=3")
			.headers(headers_2)))
		.pause(4)
		.exec(http("request_45")
			.get("/creditapplicationsystem/v1/applications")
			.headers(headers_2)
			.resources(http("request_46")
			.get(uri2 + "/clientsystem/v1/clients")
			.headers(headers_2),
            http("request_47")
			.get(uri2 + "/nflow/v1/workflow-instance?type=creditDecision&state=manualDecision&businessKey=3")
			.headers(headers_2)))
		.pause(3)
		.exec(http("request_48")
			.put("/nflow/v1/workflow-instance/6")
			.headers(headers_5)
			.body(RawFileBody("RecordedSimulation_0048_request.txt"))
			.resources(http("request_49")
			.get(uri2 + "/creditapplicationsystem/v1/applications")
			.headers(headers_2),
            http("request_50")
			.get(uri2 + "/clientsystem/v1/clients")
			.headers(headers_2),
            http("request_51")
			.get(uri2 + "/nflow/v1/workflow-instance?type=creditDecision&state=manualDecision&businessKey=3")
			.headers(headers_2)))
		.pause(4)
		.exec(http("request_52")
			.get("/clerk/views/workflow.html")
			.headers(headers_2)
			.resources(http("request_53")
			.get(uri2 + "/creditapplicationsystem/v1/applications/2")
			.headers(headers_2),
            http("request_54")
			.get(uri2 + "/nflow/v1/workflow-definition?type=processCreditApplication")
			.headers(headers_2),
            http("request_55")
			.get(uri2 + "/nflowsystem/v1/workflow-graph")
			.headers(headers_55),
            http("request_56")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_57")
			.get(uri2 + "/creditapplicationsystem/v1/applications")
			.headers(headers_2),
            http("request_58")
			.get(uri2 + "/clientsystem/v1/clients")
			.headers(headers_2),
            http("request_59")
			.get(uri2 + "/creditapplicationsystem/v1/applications/2")
			.headers(headers_2),
            http("request_60")
			.get(uri2 + "/nflow/v1/workflow-instance/3?include=actions")
			.headers(headers_2),
            http("request_61")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_62")
			.get(uri2 + "/creditapplicationsystem/v1/applications/2")
			.headers(headers_2),
            http("request_63")
			.get(uri2 + "/nflow/v1/workflow-instance/3?include=actions")
			.headers(headers_2),
            http("request_64")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_65")
			.get(uri2 + "/creditapplicationsystem/v1/applications/2")
			.headers(headers_2),
            http("request_66")
			.get(uri2 + "/nflow/v1/workflow-instance/3?include=actions")
			.headers(headers_2),
            http("request_67")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_68")
			.get(uri2 + "/creditapplicationsystem/v1/applications/2")
			.headers(headers_2),
            http("request_69")
			.get(uri2 + "/nflow/v1/workflow-instance/3?include=actions")
			.headers(headers_2),
            http("request_70")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_71")
			.get(uri2 + "/creditapplicationsystem/v1/applications/2")
			.headers(headers_2),
            http("request_72")
			.get(uri2 + "/nflow/v1/workflow-instance/3?include=actions")
			.headers(headers_2),
            http("request_73")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_74")
			.get(uri2 + "/creditapplicationsystem/v1/applications/2")
			.headers(headers_2),
            http("request_75")
			.get(uri2 + "/nflow/v1/workflow-instance/3?include=actions")
			.headers(headers_2),
            http("request_76")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_77")
			.get(uri2 + "/creditapplicationsystem/v1/applications/2")
			.headers(headers_2),
            http("request_78")
			.get(uri2 + "/nflow/v1/workflow-instance/3?include=actions")
			.headers(headers_2),
            http("request_79")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_80")
			.get(uri2 + "/creditapplicationsystem/v1/applications/2")
			.headers(headers_2),
            http("request_81")
			.get(uri2 + "/nflow/v1/workflow-instance/3?include=actions")
			.headers(headers_2),
            http("request_82")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_83")
			.get(uri2 + "/creditapplicationsystem/v1/applications/2")
			.headers(headers_2),
            http("request_84")
			.get(uri2 + "/nflow/v1/workflow-instance/3?include=actions")
			.headers(headers_2),
            http("request_85")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_86")
			.get(uri2 + "/creditapplicationsystem/v1/applications")
			.headers(headers_2),
            http("request_87")
			.get(uri2 + "/clientsystem/v1/clients")
			.headers(headers_2),
            http("request_88")
			.get(uri2 + "/creditapplicationsystem/v1/applications/2")
			.headers(headers_2),
            http("request_89")
			.get(uri2 + "/nflow/v1/workflow-instance/3?include=actions")
			.headers(headers_2),
            http("request_90")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2)))
		.pause(1)
		.exec(http("request_91")
			.get("/creditapplicationsystem/v1/applications/1")
			.headers(headers_2)
			.resources(http("request_92")
			.get(uri2 + "/nflow/v1/workflow-definition?type=processCreditApplication")
			.headers(headers_2),
            http("request_93")
			.get(uri2 + "/nflowsystem/v1/workflow-graph")
			.headers(headers_55),
            http("request_94")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_95")
			.get(uri2 + "/creditapplicationsystem/v1/applications/1")
			.headers(headers_2),
            http("request_96")
			.get(uri2 + "/nflow/v1/workflow-instance/1?include=actions")
			.headers(headers_2),
            http("request_97")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_98")
			.get(uri2 + "/creditapplicationsystem/v1/applications/1")
			.headers(headers_2),
            http("request_99")
			.get(uri2 + "/nflow/v1/workflow-instance/1?include=actions")
			.headers(headers_2),
            http("request_100")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_101")
			.get(uri2 + "/creditapplicationsystem/v1/applications/1")
			.headers(headers_2),
            http("request_102")
			.get(uri2 + "/nflow/v1/workflow-instance/1?include=actions")
			.headers(headers_2),
            http("request_103")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_104")
			.get(uri2 + "/creditapplicationsystem/v1/applications/1")
			.headers(headers_2),
            http("request_105")
			.get(uri2 + "/nflow/v1/workflow-instance/1?include=actions")
			.headers(headers_2),
            http("request_106")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_107")
			.get(uri2 + "/creditapplicationsystem/v1/applications/1")
			.headers(headers_2),
            http("request_108")
			.get(uri2 + "/nflow/v1/workflow-instance/1?include=actions")
			.headers(headers_2),
            http("request_109")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_110")
			.get(uri2 + "/creditapplicationsystem/v1/applications/1")
			.headers(headers_2),
            http("request_111")
			.get(uri2 + "/nflow/v1/workflow-instance/1?include=actions")
			.headers(headers_2),
            http("request_112")
			.get(uri2 + "/creditapplicationsystem/v1/applications")
			.headers(headers_2),
            http("request_113")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_114")
			.get(uri2 + "/clientsystem/v1/clients")
			.headers(headers_2),
            http("request_115")
			.get(uri2 + "/creditapplicationsystem/v1/applications/1")
			.headers(headers_2),
            http("request_116")
			.get(uri2 + "/nflow/v1/workflow-instance/1?include=actions")
			.headers(headers_2),
            http("request_117")
			.get(uri2 + "/creditapplicationsystem/v1/applications")
			.headers(headers_2),
            http("request_118")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_119")
			.get(uri2 + "/clientsystem/v1/clients")
			.headers(headers_2),
            http("request_120")
			.get(uri2 + "/creditapplicationsystem/v1/applications/1")
			.headers(headers_2),
            http("request_121")
			.get(uri2 + "/nflow/v1/workflow-instance/1?include=actions")
			.headers(headers_2),
            http("request_122")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_123")
			.get(uri2 + "/creditapplicationsystem/v1/applications/3")
			.headers(headers_2),
            http("request_124")
			.get(uri2 + "/nflow/v1/workflow-definition?type=processCreditApplication")
			.headers(headers_2),
            http("request_125")
			.get(uri2 + "/nflowsystem/v1/workflow-graph")
			.headers(headers_55),
            http("request_126")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_127")
			.get(uri2 + "/creditapplicationsystem/v1/applications/3")
			.headers(headers_2),
            http("request_128")
			.get(uri2 + "/nflow/v1/workflow-instance/5?include=actions")
			.headers(headers_2),
            http("request_129")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_130")
			.get(uri2 + "/creditapplicationsystem/v1/applications/3")
			.headers(headers_2),
            http("request_131")
			.get(uri2 + "/nflow/v1/workflow-instance/5?include=actions")
			.headers(headers_2),
            http("request_132")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_133")
			.get(uri2 + "/creditapplicationsystem/v1/applications/3")
			.headers(headers_2),
            http("request_134")
			.get(uri2 + "/nflow/v1/workflow-instance/5?include=actions")
			.headers(headers_2),
            http("request_135")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_136")
			.get(uri2 + "/creditapplicationsystem/v1/applications/3")
			.headers(headers_2),
            http("request_137")
			.get(uri2 + "/nflow/v1/workflow-instance/5?include=actions")
			.headers(headers_2),
            http("request_138")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_139")
			.get(uri2 + "/creditapplicationsystem/v1/applications/3")
			.headers(headers_2),
            http("request_140")
			.get(uri2 + "/nflow/v1/workflow-instance/5?include=actions")
			.headers(headers_2),
            http("request_141")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_142")
			.get(uri2 + "/creditapplicationsystem/v1/applications/3")
			.headers(headers_2),
            http("request_143")
			.get(uri2 + "/nflow/v1/workflow-instance/5?include=actions")
			.headers(headers_2),
            http("request_144")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_145")
			.get(uri2 + "/creditapplicationsystem/v1/applications/3")
			.headers(headers_2),
            http("request_146")
			.get(uri2 + "/nflow/v1/workflow-instance/5?include=actions")
			.headers(headers_2),
            http("request_147")
			.get(uri2 + "/depositsystem/v1/accounts?iban=FI1111222200000017")
			.headers(headers_2),
            http("request_148")
			.get(uri2 + "/creditapplicationsystem/v1/applications")
			.headers(headers_2),
            http("request_149")
			.get(uri2 + "/clientsystem/v1/clients")
			.headers(headers_2),
            http("request_150")
			.get(uri2 + "/creditapplicationsystem/v1/applications")
			.headers(headers_2),
            http("request_151")
			.get(uri2 + "/clerk/")
			.headers(headers_0),
            http("request_152")
			.get(uri1 + "?v=1&_v=j36&a=1697207073&t=pageview&_s=1&dl=http%3A%2F%2Fnbank-willow-795063877.eu-west-1.elb.amazonaws.com%2Fclerk%2F&ul=en-us&de=UTF-8&sd=24-bit&sr=1440x900&vp=1440x401&je=1&fl=17.0%20r0&_u=AACAAAABI~&jid=&cid=1867679005.1432490104&tid=UA-XXXXX-X&z=531622475")
			.headers(headers_1),
            http("request_153")
			.get(uri2 + "/clerk/views/main.html")
			.headers(headers_2),
            http("request_154")
			.get(uri2 + "/clerk/sounds/notice.wav")
			.headers(headers_3),
            http("request_155")
			.get(uri2 + "/creditapplicationsystem/v1/applications")
			.headers(headers_2),
            http("request_156")
			.get(uri2 + "/clientsystem/v1/clients")
			.headers(headers_2)))
			}

	setUp(scn.inject(atOnceUsers(10), rampUsers(100) over(30 seconds))).protocols(httpProtocol)
}