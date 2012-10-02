package com.geodemo.dao

import com.mongodb.casbah.Imports._
import com.novus.salat._
import com.novus.salat.global._
import com.geodemo.model.Location
import akka.dispatch.ExecutionContext
import com.geodemo.mongo.RandomId

/**
 * @author chris_carrier
 * @version 7/25/12
 */

trait LocationDao {

  def saveLocation(l: Location): Location
  def getNearbyLocations(distance: Int, lat: Int, long: Int): List[Location]

}

class MongoLocationDao(defaultCollection: MongoCollection) extends LocationDao {

  def saveLocation(l: Location): Location = {
    val randomId = RandomId.getNextValue
    val toSave = MongoDBObject("_id" -> randomId, "name" -> l.name, "location" -> List(l.lat, l.long))
    defaultCollection.insert(toSave)

    val toReturn = l.copy(_id = randomId)
    toReturn
  }

  def getNearbyLocations(distance: Int, lat: Int, long: Int): List[Location] = {
    val query = MongoDBObject("location" -> MongoDBObject("$near" -> (lat, long)))
    val res = defaultCollection.find(query).map(f => {
      Location(f.getAs[String]("_id"), f.getAs[String]("name").getOrElse(""), 1, 1)
    })

    res.toList
  }

}
