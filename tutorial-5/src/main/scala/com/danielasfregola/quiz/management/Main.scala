package com.danielasfregola.quiz.management

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.danielasfregola.twitter4s.TwitterClient
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken}
import com.typesafe.config.ConfigFactory
import spray.can.Http

import scala.concurrent.duration._

object Main extends App {
  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  implicit val system = ActorSystem("quiz-management-service")
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(10 seconds)

  val api = system.actorOf(Props(new RestInterface))

  IO(Http).ask(Http.Bind(listener = api, interface = host, port = port))
    .mapTo[Http.Event]
    .map {
      case Http.Bound(address) =>
        println(s"REST interface bound to $address")
      case Http.CommandFailed(cmd) =>
        println("REST interface could not bind to " +
          s"$host:$port, ${cmd.failureMessage}")
        system.shutdown()
    }

  val consumerToken = ConsumerToken(key = "my-consumer-key", secret = "my-consumer-secret")
  val accessToken = AccessToken(key = "my-access-key", secret = "my-access-secret")  
  val client = new TwitterClient(consumerToken, accessToken)

  val tweets = client.getUserTimelineForUser()
}