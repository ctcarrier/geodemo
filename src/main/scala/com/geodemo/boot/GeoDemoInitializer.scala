package com.geodemo.boot

import util.Properties
import cc.spray.can.server.HttpServer
import cc.spray.io.pipelining.MessageHandlerDispatch
import akka.actor.{ActorSystem, Props}
import com.weiglewilczek.slf4s.Logging
import com.typesafe.config.ConfigFactory
import akka.dispatch.ExecutionContext
import com.mongodb.casbah.commons.MongoDBObject
import com.geodemo.mongo.MongoSettings
import com.geodemo.endpoint.LocationEndpoint
import cc.spray.routing.HttpService
import cc.spray.io.IOBridge
import cc.spray.io.pipelining.MessageHandlerDispatch
import java.util.concurrent.{ExecutorService, Executors}
import com.geodemo.dao.{MongoLocationDao, LocationDao}

/**
 * @author chris_carrier
 */

object GeoDemoInitializer extends App with Logging {

  logger.info("Running Initializer")

  val system = ActorSystem("geodem")

  val executionContext: ExecutionContext with ExecutorService = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  val config = ConfigFactory.load()

  val host = "0.0.0.0"
  val port = Option(System.getenv("PORT")).getOrElse("8080").toInt

  val mongoUrl = config.getString("mongodb.url")
  val mongoDbName = config.getString("mongodb.database")

//  val urlList = mongoUrl.split(",").toList.map(new ServerAddress(_))

  val MongoSettings(db) = Properties.envOrNone("MONGOHQ_URL")

  val locationCollection = db(config.getString("geodemo.location.collection"))

  val locationDao = new MongoLocationDao(locationCollection)
  // ///////////// INDEXES for collections go here (include all lookup fields)
  //  configsCollection.ensureIndex(MongoDBObject("customerId" -> 1), "idx_customerId")

  val locationHandler = system.actorOf(
    Props(new LocationEndpoint {
      val dao = locationDao
    }),
    name = "location-service"
  )

  // every spray-can HttpServer (and HttpClient) needs an IoWorker for low-level network IO
  // (but several servers and/or clients can share one)
  val ioBridge = new IOBridge(system).start()

  // create and start the spray-can HttpServer, telling it that we want requests to be
  // handled by the root service actor
  val sprayCanServer = system.actorOf(
    Props(new HttpServer(ioBridge, MessageHandlerDispatch.SingletonHandler(locationHandler))),
    name = "http-server"
  )

  // a running HttpServer can be bound, unbound and rebound
  // initially to need to tell it where to bind to
  sprayCanServer ! HttpServer.Bind(host, port)

  // finally we drop the main thread but hook the shutdown of
  // our IoWorker into the shutdown of the applications ActorSystem
  system.registerOnTermination {
    ioBridge.stop()
  }
}

