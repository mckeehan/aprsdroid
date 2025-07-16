package org.aprsdroid.app

import android.content.Context
import android.graphics.Color
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.{BitmapDescriptorFactory, LatLng, Marker, MarkerOptions, Polyline, PolylineOptions}
import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConversions._

class GoogleGPXOverlay(context: Context, db: StorageDatabase, googleMap: GoogleMap) {
  val TAG = "APRSdroid.GoogleGPXOverlay"
  
  private var gpxMarkers = ArrayBuffer[Marker]()
  private var gpxPolylines = ArrayBuffer[Polyline]()
  private var visible = true
  
  def setVisible(visible: Boolean) {
    this.visible = visible
    gpxMarkers.foreach(_.setVisible(visible))
    gpxPolylines.foreach(_.setVisible(visible))
  }
  
  def clearOverlay() {
    gpxMarkers.foreach(_.remove())
    gpxPolylines.foreach(_.remove())
    gpxMarkers.clear()
    gpxPolylines.clear()
  }
  
  def loadGPXData() {
    Log.d(TAG, "Loading GPX data from database")
    
    // Clear existing overlay data
    clearOverlay()
    
    try {
      // Load waypoints (only visible ones)
      val waypointCursor = db.getVisibleGPXWaypoints()
      try {
        while (waypointCursor.moveToNext()) {
          val lat = waypointCursor.getInt(StorageDatabase.GPXWaypoint.COLUMN_LAT) / 1000000.0
          val lon = waypointCursor.getInt(StorageDatabase.GPXWaypoint.COLUMN_LON) / 1000000.0
          val name = waypointCursor.getString(StorageDatabase.GPXWaypoint.COLUMN_NAME)
          val comment = waypointCursor.getString(StorageDatabase.GPXWaypoint.COLUMN_COMMENT)
          
          val markerOptions = new MarkerOptions()
            .position(new LatLng(lat, lon))
            .title(name)
            .snippet(comment)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
          
          val marker = googleMap.addMarker(markerOptions)
          marker.setVisible(visible)
          gpxMarkers += marker
        }
      } finally {
        waypointCursor.close()
      }
      
      // Load tracks (only visible ones)
      val trackCursor = db.getVisibleGPXTracks()
      try {
        var currentTrackName: String = null
        var currentTrackPoints = ArrayBuffer[LatLng]()
        
        while (trackCursor.moveToNext()) {
          val lat = trackCursor.getInt(StorageDatabase.GPXTrack.COLUMN_LAT) / 1000000.0
          val lon = trackCursor.getInt(StorageDatabase.GPXTrack.COLUMN_LON) / 1000000.0
          val name = trackCursor.getString(StorageDatabase.GPXTrack.COLUMN_NAME)
          
          if (currentTrackName != null && currentTrackName != name) {
            // Create polyline for previous track
            if (currentTrackPoints.size > 1) {
              val polylineOptions = new PolylineOptions()
                .addAll(currentTrackPoints)
                .color(Color.RED)
                .width(4.0f)
              
              val polyline = googleMap.addPolyline(polylineOptions)
              polyline.setVisible(visible)
              gpxPolylines += polyline
            }
            
            // Start new track
            currentTrackPoints.clear()
          }
          
          currentTrackName = name
          currentTrackPoints += new LatLng(lat, lon)
        }
        
        // Add final track if exists
        if (currentTrackPoints.size > 1) {
          val polylineOptions = new PolylineOptions()
            .addAll(currentTrackPoints)
            .color(Color.RED)
            .width(4.0f)
          
          val polyline = googleMap.addPolyline(polylineOptions)
          polyline.setVisible(visible)
          gpxPolylines += polyline
        }
        
      } finally {
        trackCursor.close()
      }
      
      Log.d(TAG, s"Loaded ${gpxMarkers.size} waypoints and ${gpxPolylines.size} tracks")
      Log.d(TAG, s"GoogleGPXOverlay visibility: $visible")
    } catch {
      case e: Exception =>
        Log.e(TAG, "Error loading GPX data: " + e.getMessage)
        clearOverlay()
    }
  }
  
  def refresh() {
    loadGPXData()
  }
}