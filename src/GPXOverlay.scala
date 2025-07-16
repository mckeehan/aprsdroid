package org.aprsdroid.app

import android.content.Context
import android.database.Cursor
import android.graphics.{Canvas, Paint, Path, Point}
import android.util.Log
import org.mapsforge.v3.android.maps.overlay.Overlay
import org.mapsforge.v3.android.maps.Projection
import org.mapsforge.v3.core.GeoPoint
import scala.collection.mutable.ArrayBuffer

class GPXOverlay(context: Context, db: StorageDatabase) extends Overlay {
  val TAG = "APRSdroid.GPXOverlay"
  
  private var gpxWaypoints = ArrayBuffer[GPXWaypointData]()
  private var gpxTracks = ArrayBuffer[GPXTrackData]()
  private var visible = true

  lazy val drawSize = (context.getResources().getDisplayMetrics().density * 24).toInt

  case class GPXWaypointData(lat: Double, lon: Double, name: String, symbol: String, comment: String)
  case class GPXTrackData(name: String, points: ArrayBuffer[GeoPoint])
  
  def setVisible(visible: Boolean) {
    this.visible = visible
  }
  
  def loadGPXData() {
    Log.d(TAG, "Loading GPX data from database")
    
    // Clear existing data
    gpxWaypoints.clear()
    gpxTracks.clear()
    
    try {
      // Load waypoints (only visible ones)
      val waypointCursor = db.getVisibleGPXWaypoints()
      try {
        while (waypointCursor.moveToNext()) {
          val lat = waypointCursor.getInt(StorageDatabase.GPXWaypoint.COLUMN_LAT) / 1000000.0
          val lon = waypointCursor.getInt(StorageDatabase.GPXWaypoint.COLUMN_LON) / 1000000.0
          val name = waypointCursor.getString(StorageDatabase.GPXWaypoint.COLUMN_NAME)
          val symbol = waypointCursor.getString(StorageDatabase.GPXWaypoint.COLUMN_SYMBOL)
          val comment = waypointCursor.getString(StorageDatabase.GPXWaypoint.COLUMN_COMMENT)
          
          gpxWaypoints += GPXWaypointData(lat, lon, name, symbol, comment)
        }
      } finally {
        waypointCursor.close()
      }
      
      // Load tracks (only visible ones)
      val trackCursor = db.getVisibleGPXTracks()
      try {
        var currentTrack: GPXTrackData = null
        while (trackCursor.moveToNext()) {
          val lat = trackCursor.getInt(StorageDatabase.GPXTrack.COLUMN_LAT) / 1000000.0
          val lon = trackCursor.getInt(StorageDatabase.GPXTrack.COLUMN_LON) / 1000000.0
          val name = trackCursor.getString(StorageDatabase.GPXTrack.COLUMN_NAME)
          
          if (currentTrack == null || currentTrack.name != name) {
            currentTrack = GPXTrackData(name, ArrayBuffer[GeoPoint]())
            gpxTracks += currentTrack
          }
          
          currentTrack.points += new GeoPoint((lat * 1000000).toInt, (lon * 1000000).toInt)
        }
      } finally {
        trackCursor.close()
      }
      
      Log.d(TAG, s"Loaded ${gpxWaypoints.size} waypoints and ${gpxTracks.size} tracks")
      if (gpxWaypoints.nonEmpty) {
        Log.d(TAG, s"First waypoint: ${gpxWaypoints(0)}")
      }
      if (gpxTracks.nonEmpty) {
        Log.d(TAG, s"First track: ${gpxTracks(0).name} with ${gpxTracks(0).points.size} points")
      }
    } catch {
      case e: Exception =>
        Log.e(TAG, "Error loading GPX data: " + e.getMessage)
        gpxWaypoints.clear()
        gpxTracks.clear()
    }
  }
  
  override def drawOverlayBitmap(canvas: Canvas, drawPosition: Point, projection: Projection, drawZoomLevel: Byte) {
    if (!visible) {
      Log.d(TAG, "GPX overlay not visible, skipping draw")
      return
    }
    
    Log.d(TAG, s"Drawing GPX overlay: ${gpxWaypoints.size} waypoints, ${gpxTracks.size} tracks")
    drawTracks(canvas, projection)
    drawWaypoints(canvas, projection)
  }
  
  private def drawTracks(canvas: Canvas, projection: Projection) {
    val trackPaint = new Paint()
    trackPaint.setARGB(200, 255, 0, 0) // Red tracks
    trackPaint.setStyle(Paint.Style.STROKE)
    trackPaint.setStrokeJoin(Paint.Join.ROUND)
    trackPaint.setStrokeCap(Paint.Cap.ROUND)
    trackPaint.setStrokeWidth(4.0f)
    trackPaint.setAntiAlias(true)
    
    val point = new Point()
    
    gpxTracks.foreach { track =>
      if (track.points.size > 1) {
        val path = new Path()
        var first = true
        
        track.points.foreach { geoPoint =>
          projection.toPixels(geoPoint, point)
          if (first) {
            path.moveTo(point.x, point.y)
            first = false
          } else {
            path.lineTo(point.x, point.y)
          }
        }
        
        canvas.drawPath(path, trackPaint)
      }
    }
  }
  
  private def drawWaypoints(canvas: Canvas, projection: Projection) {
    val fontSize = drawSize*7/8

    val waypointPaint = new Paint()
    waypointPaint.setARGB(255, 0, 255, 0) // Green waypoints
    waypointPaint.setStyle(Paint.Style.FILL)
    waypointPaint.setAntiAlias(true)
    
    val textPaint = new Paint()
    textPaint.setARGB(255, 0, 0, 0) // Black text
    textPaint.setTextSize(fontSize/2)
    textPaint.setAntiAlias(true)
    
    val point = new Point()
    
    gpxWaypoints.foreach { waypoint =>
      val geoPoint = new GeoPoint((waypoint.lat * 1000000).toInt, (waypoint.lon * 1000000).toInt)
      projection.toPixels(geoPoint, point)
      
      // Draw waypoint marker
      canvas.drawCircle(point.x, point.y, 6.0f, waypointPaint)
      
      // Draw waypoint name
      if (waypoint.name != null && waypoint.name.length > 0) {
        canvas.drawText(waypoint.name, point.x + 8, point.y - 8, textPaint)
      }
    }
  }
  
  def refresh() {
    loadGPXData()
    // Force map redraw
    requestRedraw()
  }
}
