package org.aprsdroid.app

import android.app.Activity
import android.content._
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import java.io.IOException

class GPXImportActivity extends Activity {
  val TAG = "APRSdroid.GPXImport"
  
  lazy val db = StorageDatabase.open(this)
  
  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    Log.d(TAG, "created: " + getIntent())
    importGPX()
  }
  
  def importGPX() {
    try {
      val inputStream = getContentResolver().openInputStream(getIntent.getData())
      val parser = new GPXParser()
      val gpxData = parser.parse(inputStream)
      inputStream.close()
      
      // Store GPX data in database
      val currentTime = System.currentTimeMillis()
      storeGPXData(gpxData, currentTime)
      
      // Create metadata entry for visibility control and name
      val gpxName = gpxData.metadata.flatMap(_.name).getOrElse("GPX Import")
      db.setGPXMetadata(currentTime, true, gpxName)
      
      // Notify user of successful import
      val totalItems = gpxData.waypoints.size + gpxData.tracks.size + gpxData.routes.size
      val msg = getString(R.string.gpx_import_done, totalItems.toString, getIntent.getData().getPath())
      Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
      
      // Add log entry
      db.addPost(System.currentTimeMillis(), StorageDatabase.Post.TYPE_INFO,
        getString(R.string.gpx_import_activity), msg)
      
      // Send broadcast to refresh map
      sendBroadcast(new Intent(AprsService.UPDATE))
      
      // Start map activity to show imported data
      startActivity(new Intent(this, classOf[MapAct]))
      
    } catch {
      case e: IOException =>
        val errmsg = getString(R.string.gpx_import_error, e.getMessage())
        Toast.makeText(this, errmsg, Toast.LENGTH_LONG).show()
        db.addPost(System.currentTimeMillis(), StorageDatabase.Post.TYPE_ERROR,
          getString(R.string.gpx_import_activity), errmsg)
        Log.e(TAG, "GPX import error", e)
      case e: Exception =>
        val errmsg = getString(R.string.gpx_import_error, e.getMessage())
        Toast.makeText(this, errmsg, Toast.LENGTH_LONG).show()
        db.addPost(System.currentTimeMillis(), StorageDatabase.Post.TYPE_ERROR,
          getString(R.string.gpx_import_activity), errmsg)
        Log.e(TAG, "GPX import error", e)
    }
    finish()
  }
  
  private def storeGPXData(gpxData: GPXData, currentTime: Long) {
    
    // Store waypoints
    gpxData.waypoints.foreach { waypoint =>
      val name = waypoint.name.getOrElse("GPX Waypoint")
      val comment = waypoint.desc.orElse(waypoint.cmt).getOrElse("")
      val symbol = waypoint.sym.getOrElse("GPX")
      
      db.addGPXWaypoint(currentTime, name, 
        (waypoint.lat * 1000000).toInt, (waypoint.lon * 1000000).toInt,
        symbol, comment, waypoint.ele.getOrElse(0.0).toInt)
    }
    
    // Store tracks
    gpxData.tracks.foreach { track =>
      val trackName = track.name.getOrElse("GPX Track")
      track.segments.foreach { segment =>
        segment.points.foreach { point =>
          db.addGPXTrackPoint(currentTime, trackName,
            (point.lat * 1000000).toInt, (point.lon * 1000000).toInt,
            point.ele.getOrElse(0.0).toInt)
        }
      }
    }
    
    // Store routes
    gpxData.routes.foreach { route =>
      val routeName = route.name.getOrElse("GPX Route")
      route.points.foreach { point =>
        val name = point.name.getOrElse(routeName)
        val comment = point.desc.orElse(point.cmt).getOrElse("")
        val symbol = point.sym.getOrElse("GPX")
        
        db.addGPXWaypoint(currentTime, name,
          (point.lat * 1000000).toInt, (point.lon * 1000000).toInt,
          symbol, comment, point.ele.getOrElse(0.0).toInt)
      }
    }
    
    Log.d(TAG, s"Stored ${gpxData.waypoints.size} waypoints, ${gpxData.tracks.size} tracks, ${gpxData.routes.size} routes")
  }
}