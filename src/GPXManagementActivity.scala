package org.aprsdroid.app

import android.app.{Activity, AlertDialog}
import android.content.{Context, DialogInterface, Intent}
import android.os.Bundle
import android.util.Log
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{BaseAdapter, Button, CheckBox, ListView, TextView, Toast}
import scala.collection.mutable.ArrayBuffer
import java.text.SimpleDateFormat
import java.util.Date

class GPXManagementActivity extends Activity {
  val TAG = "APRSdroid.GPXManagement"
  
  lazy val db = StorageDatabase.open(this)
  lazy val listView = findViewById(R.id.gpx_list).asInstanceOf[ListView]
  lazy val clearAllButton = findViewById(R.id.clear_all_gpx).asInstanceOf[Button]
  
  private var gpxFiles = ArrayBuffer[GPXFileInfo]()
  private var adapter: GPXAdapter = null
  
  case class GPXFileInfo(
    timestamp: Long,
    name: String,
    waypointCount: Int,
    trackCount: Int,
    visible: Boolean
  )
  
  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.gpx_management)
    
    adapter = new GPXAdapter(this)
    listView.setAdapter(adapter)
    
    clearAllButton.setOnClickListener(new View.OnClickListener() {
      override def onClick(v: View) {
        confirmClearAll()
      }
    })
    
    loadGPXFiles()
  }
  
  private def loadGPXFiles() {
    Log.d(TAG, "Loading GPX files from database")
    gpxFiles.clear()
    
    // Get unique timestamps from GPX data
    val timestamps = scala.collection.mutable.Set[Long]()
    
    val waypointCursor = db.getGPXWaypoints()
    try {
      while (waypointCursor.moveToNext()) {
        val ts = waypointCursor.getLong(StorageDatabase.GPXWaypoint.COLUMN_TS)
        timestamps += ts
      }
    } finally {
      waypointCursor.close()
    }
    
    val trackCursor = db.getGPXTracks()
    try {
      while (trackCursor.moveToNext()) {
        val ts = trackCursor.getLong(StorageDatabase.GPXTrack.COLUMN_TS)
        timestamps += ts
      }
    } finally {
      trackCursor.close()
    }
    
    // Create GPX file info for each timestamp
    timestamps.foreach { ts =>
      val waypointCount = db.getGPXWaypointCount(ts)
      val trackCount = db.getGPXTrackCount(ts)
      val visible = db.getGPXVisibility(ts)
      
      val name = db.getGPXName(ts) match {
        case null => 
          val dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm")
          s"GPX Import - ${dateFormat.format(new Date(ts))}"
        case gpxName => gpxName
      }
      
      gpxFiles += GPXFileInfo(ts, name, waypointCount, trackCount, visible)
    }
    
    gpxFiles.sortBy(-_.timestamp) // Sort by timestamp descending
    adapter.notifyDataSetChanged()
    
    Log.d(TAG, s"Loaded ${gpxFiles.size} GPX files")
  }
  
  private def confirmClearAll() {
    new AlertDialog.Builder(this)
      .setTitle(R.string.gpx_clear_all_title)
      .setMessage(R.string.gpx_clear_all_message)
      .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
        override def onClick(dialog: DialogInterface, which: Int) {
          db.clearGPXData()
          loadGPXFiles()
          sendBroadcast(new Intent(AprsService.UPDATE))
          Toast.makeText(GPXManagementActivity.this, R.string.gpx_cleared, Toast.LENGTH_SHORT).show()
        }
      })
      .setNegativeButton(R.string.no, null)
      .show()
  }
  
  private def toggleGPXVisibility(timestamp: Long, visible: Boolean) {
    db.setGPXVisibility(timestamp, visible)
    sendBroadcast(new Intent(AprsService.UPDATE))
  }
  
  private def removeGPXFile(timestamp: Long) {
    new AlertDialog.Builder(this)
      .setTitle(R.string.gpx_remove_title)
      .setMessage(R.string.gpx_remove_message)
      .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
        override def onClick(dialog: DialogInterface, which: Int) {
          db.removeGPXData(timestamp)
          loadGPXFiles()
          sendBroadcast(new Intent(AprsService.UPDATE))
          Toast.makeText(GPXManagementActivity.this, R.string.gpx_removed, Toast.LENGTH_SHORT).show()
        }
      })
      .setNegativeButton(R.string.no, null)
      .show()
  }
  
  private class GPXAdapter(context: Context) extends BaseAdapter {
    private val inflater = LayoutInflater.from(context)
    
    override def getCount(): Int = gpxFiles.size
    override def getItem(position: Int): Object = gpxFiles(position)
    override def getItemId(position: Int): Long = position
    
    override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
      val view = if (convertView == null) {
        inflater.inflate(R.layout.gpx_item, parent, false)
      } else {
        convertView
      }
      
      val gpxFile = gpxFiles(position)
      
      val nameText = view.findViewById(R.id.gpx_name).asInstanceOf[TextView]
      val infoText = view.findViewById(R.id.gpx_info).asInstanceOf[TextView]
      val visibilityCheckbox = view.findViewById(R.id.gpx_visibility).asInstanceOf[CheckBox]
      val removeButton = view.findViewById(R.id.gpx_remove).asInstanceOf[Button]
      
      nameText.setText(gpxFile.name)
      infoText.setText(s"${gpxFile.waypointCount} waypoints, ${gpxFile.trackCount} tracks")
      
      visibilityCheckbox.setChecked(gpxFile.visible)
      visibilityCheckbox.setOnClickListener(new View.OnClickListener() {
        override def onClick(v: View) {
          val isVisible = visibilityCheckbox.isChecked()
          gpxFiles(position) = gpxFile.copy(visible = isVisible)
          toggleGPXVisibility(gpxFile.timestamp, isVisible)
        }
      })
      
      removeButton.setOnClickListener(new View.OnClickListener() {
        override def onClick(v: View) {
          removeGPXFile(gpxFile.timestamp)
        }
      })
      
      view
    }
  }
}