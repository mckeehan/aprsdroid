package org.aprsdroid.app

import android.util.Log
import org.xmlpull.v1.{XmlPullParser, XmlPullParserFactory}
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}
import scala.collection.mutable.ArrayBuffer

class GPXParser {
  val TAG = "APRSdroid.GPXParser"
  
  // ISO 8601 date format for GPX
  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
  dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
  
  def parse(inputStream: InputStream): GPXData = {
    val factory = XmlPullParserFactory.newInstance()
    val parser = factory.newPullParser()
    parser.setInput(inputStream, null)
    
    var eventType = parser.getEventType()
    var gpxData = GPXData()
    
    while (eventType != XmlPullParser.END_DOCUMENT) {
      eventType match {
        case XmlPullParser.START_TAG =>
          parser.getName() match {
            case "gpx" => 
              Log.d(TAG, "Found GPX root element")
            case "metadata" =>
              gpxData = gpxData.copy(metadata = Some(parseMetadata(parser)))
            case "wpt" =>
              val waypoint = parseWaypoint(parser)
              gpxData = gpxData.copy(waypoints = gpxData.waypoints :+ waypoint)
            case "trk" =>
              val track = parseTrack(parser)
              gpxData = gpxData.copy(tracks = gpxData.tracks :+ track)
            case "rte" =>
              val route = parseRoute(parser)
              gpxData = gpxData.copy(routes = gpxData.routes :+ route)
            case _ => // ignore other elements
          }
        case _ => // ignore other events
      }
      eventType = parser.next()
    }
    
    Log.d(TAG, s"Parsed GPX: ${gpxData.waypoints.size} waypoints, ${gpxData.tracks.size} tracks, ${gpxData.routes.size} routes")
    gpxData
  }
  
  private def parseMetadata(parser: XmlPullParser): GPXMetadata = {
    var metadata = GPXMetadata()
    var eventType = parser.next()
    
    while (eventType != XmlPullParser.END_TAG || parser.getName() != "metadata") {
      if (eventType == XmlPullParser.START_TAG) {
        parser.getName() match {
          case "name" => metadata = metadata.copy(name = Some(readText(parser)))
          case "desc" => metadata = metadata.copy(desc = Some(readText(parser)))
          case "author" => metadata = metadata.copy(author = Some(readText(parser)))
          case "time" => metadata = metadata.copy(time = parseDate(readText(parser)))
          case _ => skip(parser)
        }
      }
      eventType = parser.next()
    }
    metadata
  }
  
  private def parseWaypoint(parser: XmlPullParser): GPXWaypoint = {
    val lat = parser.getAttributeValue(null, "lat").toDouble
    val lon = parser.getAttributeValue(null, "lon").toDouble
    
    var waypoint = GPXWaypoint(lat, lon)
    var eventType = parser.next()
    
    while (eventType != XmlPullParser.END_TAG || parser.getName() != "wpt") {
      if (eventType == XmlPullParser.START_TAG) {
        parser.getName() match {
          case "ele" => waypoint = waypoint.copy(ele = Some(readText(parser).toDouble))
          case "time" => waypoint = waypoint.copy(time = parseDate(readText(parser)))
          case "name" => waypoint = waypoint.copy(name = Some(readText(parser)))
          case "desc" => waypoint = waypoint.copy(desc = Some(readText(parser)))
          case "cmt" => waypoint = waypoint.copy(cmt = Some(readText(parser)))
          case "sym" => waypoint = waypoint.copy(sym = Some(readText(parser)))
          case _ => skip(parser)
        }
      }
      eventType = parser.next()
    }
    waypoint
  }
  
  private def parseTrack(parser: XmlPullParser): GPXTrack = {
    var track = GPXTrack(segments = List())
    var eventType = parser.next()
    
    while (eventType != XmlPullParser.END_TAG || parser.getName() != "trk") {
      if (eventType == XmlPullParser.START_TAG) {
        parser.getName() match {
          case "name" => track = track.copy(name = Some(readText(parser)))
          case "desc" => track = track.copy(desc = Some(readText(parser)))
          case "trkseg" => 
            val segment = parseTrackSegment(parser)
            track = track.copy(segments = track.segments :+ segment)
          case _ => skip(parser)
        }
      }
      eventType = parser.next()
    }
    track
  }
  
  private def parseTrackSegment(parser: XmlPullParser): GPXTrackSegment = {
    var points = List[GPXTrackPoint]()
    var eventType = parser.next()
    
    while (eventType != XmlPullParser.END_TAG || parser.getName() != "trkseg") {
      if (eventType == XmlPullParser.START_TAG) {
        parser.getName() match {
          case "trkpt" =>
            val point = parseTrackPoint(parser)
            points = points :+ point
          case _ => skip(parser)
        }
      }
      eventType = parser.next()
    }
    GPXTrackSegment(points)
  }
  
  private def parseTrackPoint(parser: XmlPullParser): GPXTrackPoint = {
    val lat = parser.getAttributeValue(null, "lat").toDouble
    val lon = parser.getAttributeValue(null, "lon").toDouble
    
    var point = GPXTrackPoint(lat, lon)
    var eventType = parser.next()
    
    while (eventType != XmlPullParser.END_TAG || parser.getName() != "trkpt") {
      if (eventType == XmlPullParser.START_TAG) {
        parser.getName() match {
          case "ele" => point = point.copy(ele = Some(readText(parser).toDouble))
          case "time" => point = point.copy(time = parseDate(readText(parser)))
          case _ => skip(parser)
        }
      }
      eventType = parser.next()
    }
    point
  }
  
  private def parseRoute(parser: XmlPullParser): GPXRoute = {
    var route = GPXRoute(points = List())
    var eventType = parser.next()
    
    while (eventType != XmlPullParser.END_TAG || parser.getName() != "rte") {
      if (eventType == XmlPullParser.START_TAG) {
        parser.getName() match {
          case "name" => route = route.copy(name = Some(readText(parser)))
          case "desc" => route = route.copy(desc = Some(readText(parser)))
          case "rtept" =>
            val point = parseRoutePoint(parser)
            route = route.copy(points = route.points :+ point)
          case _ => skip(parser)
        }
      }
      eventType = parser.next()
    }
    route
  }
  
  private def parseRoutePoint(parser: XmlPullParser): GPXWaypoint = {
    val lat = parser.getAttributeValue(null, "lat").toDouble
    val lon = parser.getAttributeValue(null, "lon").toDouble
    
    var point = GPXWaypoint(lat, lon)
    var eventType = parser.next()
    
    while (eventType != XmlPullParser.END_TAG || parser.getName() != "rtept") {
      if (eventType == XmlPullParser.START_TAG) {
        parser.getName() match {
          case "ele" => point = point.copy(ele = Some(readText(parser).toDouble))
          case "time" => point = point.copy(time = parseDate(readText(parser)))
          case "name" => point = point.copy(name = Some(readText(parser)))
          case "desc" => point = point.copy(desc = Some(readText(parser)))
          case "cmt" => point = point.copy(cmt = Some(readText(parser)))
          case "sym" => point = point.copy(sym = Some(readText(parser)))
          case _ => skip(parser)
        }
      }
      eventType = parser.next()
    }
    point
  }
  
  private def readText(parser: XmlPullParser): String = {
    var result = ""
    if (parser.next() == XmlPullParser.TEXT) {
      result = parser.getText()
      parser.nextTag()
    }
    result
  }
  
  private def parseDate(dateString: String): Option[Date] = {
    try {
      Some(dateFormat.parse(dateString))
    } catch {
      case _: Exception => None
    }
  }
  
  private def skip(parser: XmlPullParser): Unit = {
    if (parser.getEventType() != XmlPullParser.START_TAG) {
      throw new IllegalStateException()
    }
    var depth = 1
    while (depth != 0) {
      parser.next() match {
        case XmlPullParser.END_TAG => depth -= 1
        case XmlPullParser.START_TAG => depth += 1
        case _ =>
      }
    }
  }
}