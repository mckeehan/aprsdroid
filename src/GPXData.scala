package org.aprsdroid.app

import java.util.Date

// GPX data model classes
case class GPXWaypoint(
  lat: Double,
  lon: Double,
  ele: Option[Double] = None,
  time: Option[Date] = None,
  name: Option[String] = None,
  desc: Option[String] = None,
  cmt: Option[String] = None,
  sym: Option[String] = None
)

case class GPXTrackPoint(
  lat: Double,
  lon: Double,
  ele: Option[Double] = None,
  time: Option[Date] = None
)

case class GPXTrackSegment(
  points: List[GPXTrackPoint]
)

case class GPXTrack(
  name: Option[String] = None,
  desc: Option[String] = None,
  segments: List[GPXTrackSegment]
)

case class GPXRoute(
  name: Option[String] = None,
  desc: Option[String] = None,
  points: List[GPXWaypoint]
)

case class GPXMetadata(
  name: Option[String] = None,
  desc: Option[String] = None,
  author: Option[String] = None,
  time: Option[Date] = None
)

case class GPXData(
  metadata: Option[GPXMetadata] = None,
  waypoints: List[GPXWaypoint] = List(),
  tracks: List[GPXTrack] = List(),
  routes: List[GPXRoute] = List()
) {
  def getAllPoints(): List[(Double, Double)] = {
    val waypointPoints = waypoints.map(w => (w.lat, w.lon))
    val trackPoints = tracks.flatMap(_.segments).flatMap(_.points).map(p => (p.lat, p.lon))
    val routePoints = routes.flatMap(_.points).map(p => (p.lat, p.lon))
    waypointPoints ++ trackPoints ++ routePoints
  }
}