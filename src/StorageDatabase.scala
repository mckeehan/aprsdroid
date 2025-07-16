package org.aprsdroid.app

import _root_.android.content.Context
import _root_.android.content.ContentValues
import _root_.android.database.sqlite.SQLiteOpenHelper
import _root_.android.database.sqlite.SQLiteDatabase
import _root_.android.database.Cursor
import _root_.android.util.Log
import _root_.android.widget.FilterQueryProvider

import _root_.net.ab0oo.aprs.parser._

import _root_.scala.math.{cos, Pi}

object StorageDatabase {
	val TAG = "APRSdroid.Storage"
	val DB_VERSION = 6
	val DB_NAME = "storage.db"

	val TSS_COL = "DATETIME(TS/1000, 'unixepoch', 'localtime') as TSS"
	val TABLE_INDEX = "CREATE INDEX idx_%1$s_%2$s ON %1$s (%2$s)"

	object Post {
		val TABLE = "posts"
		val _ID = "_id"
		val TS = "ts"
		val TYPE = "type"
		val STATUS = "status"
		val MESSAGE = "message"
		lazy val TABLE_CREATE = "CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s LONG, %s INTEGER, %s TEXT, %s TEXT)"
					.format(TABLE, _ID, TS, TYPE, STATUS, MESSAGE);
		lazy val COLUMNS = Array(_ID, TS, TSS_COL, TYPE, STATUS, MESSAGE);

		val TYPE_POST	= 0
		val TYPE_INFO	= 1
		val TYPE_ERROR	= 2
		val TYPE_INCMG	= 3
		val TYPE_TX	= 4

		val COLUMN_TS		= 1
		val COLUMN_TSS		= 2
		val COLUMN_TYPE		= 3
		val COLUMN_MESSAGE	= 5

		var trimCounter	= 0
	}

	object Station {
		val TABLE = "stations"
		val _ID = "_id"
		val TS = "ts"
		val CALL = "call"
		val LAT = "lat"
		val LON = "lon"
		val SPEED = "speed"
		val COURSE = "course"
		val ALT = "alt"
		val SYMBOL = "symbol"
		val COMMENT = "comment"
		val ORIGIN = "origin"	// originator call for object/item
		val QRG = "qrg"		// voice frequency
		val FLAGS = "flags"	// bitmask for attributes like "messaging capable"
		lazy val TABLE_CREATE = """CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s LONG,
			%s TEXT UNIQUE, %s INTEGER, %s INTEGER,
			%s INTEGER, %s INTEGER, %s INTEGER,
			%s TEXT, %s TEXT, %s TEXT, %s TEXT, %s INTEGER)"""
			.format(TABLE, _ID, TS,
				CALL, LAT, LON,
				SPEED, COURSE, ALT,
				SYMBOL, COMMENT, ORIGIN, QRG, FLAGS)
		lazy val TABLE_DROP = "DROP TABLE %s".format(TABLE)
		lazy val COLUMNS = Array(_ID, TS, CALL, LAT, LON, SYMBOL, COMMENT, SPEED, COURSE, ALT, ORIGIN, QRG)
		lazy val COL_DIST = "((lat - %d)*(lat - %d) + (lon - %d)*(lon - %d)*%d/100) as dist"

		val COLUMN_TS		= 1
		val COLUMN_CALL		= 2
		val COLUMN_LAT		= 3
		val COLUMN_LON		= 4
		val COLUMN_SYMBOL	= 5
		val COLUMN_COMMENT	= 6
		val COLUMN_SPEED	= 7
		val COLUMN_COURSE	= 8
		val COLUMN_ALT		= 9
		val COLUMN_ORIGIN	= 10
		val COLUMN_QRG		= 11
		val COLUMN_FLAGS	= 12

		lazy val COLUMNS_MAP = Array(_ID, CALL, LAT, LON, SYMBOL, ORIGIN, QRG, COMMENT, SPEED, COURSE)
		val COLUMN_MAP_CALL	= 1
		val COLUMN_MAP_LAT	= 2
		val COLUMN_MAP_LON	= 3
		val COLUMN_MAP_SYMBOL	= 4
		val COLUMN_MAP_ORIGIN	= 5
		val COLUMN_MAP_QRG	= 6
		val COLUMN_MAP_COMMENT	= 7
		val COLUMN_MAP_SPEED	= 8
		val COLUMN_MAP_CSE	= 9

		// binary flags used for symbol coloring
		val FLAG_MSGCAPABLE	= 1
		val FLAG_OBJECT		= 2
		val FLAG_MOVING		= 4
	}

	object Position {
		val TABLE = "positions"
		val _ID = "_id"
		val TS = "ts"
		val CALL = "call"
		val LAT = "lat"
		val LON = "lon"
		lazy val TABLE_CREATE = """CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s LONG,
			%s TEXT, %s INTEGER, %s INTEGER)"""
			.format(TABLE, _ID, TS,
				CALL, LAT, LON)
		lazy val COLUMNS = Array(_ID, TS, CALL, LAT, LON)
		val COLUMN_TS		= 1
		val COLUMN_CALL		= 2
		val COLUMN_LAT		= 3
		val COLUMN_LON		= 4
	}

	object Message {
		val TABLE = "messages"
		val _ID = "_id"
		val TS = "ts"			// timestamp of RX or first TX
		val RETRYCNT = "retrycnt"	// attemp number for sending msg
		val CALL = "call"		// callsign of comms partner
		val MSGID = "msgid"		// message id (up to 5 alphanumeric symbols)
		val TYPE = "type"		// incoming / out-new / out-acked
		val TEXT = "text"		// message text
		lazy val TABLE_CREATE = """CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT,
			%s LONG, %s INT,
			%s TEXT, %s TEXT,
			%s INTEGER, %s TEXT)"""
			.format(TABLE, _ID, TS, RETRYCNT,
				CALL, MSGID,
				TYPE, TEXT)
		lazy val COLUMNS = Array(_ID, TS, TSS_COL, RETRYCNT, CALL, MSGID, TYPE, TEXT)
		val COLUMN_TS		= 1
		val COLUMN_TTS		= 2
		val COLUMN_RETRYCNT	= 3
		val COLUMN_CALL		= 4
		val COLUMN_MSGID	= 5
		val COLUMN_TYPE		= 6
		val COLUMN_TEXT		= 7


		val TYPE_INCOMING	= 1
		val TYPE_OUT_NEW	= 2
		val TYPE_OUT_ACKED	= 3
		val TYPE_OUT_REJECTED	= 4
		val TYPE_OUT_ABORTED	= 5

	}

	object GPXWaypoint {
		val TABLE = "gpx_waypoints"
		val _ID = "_id"
		val TS = "ts"
		val NAME = "name"
		val LAT = "lat"
		val LON = "lon"
		val SYMBOL = "symbol"
		val COMMENT = "comment"
		val ELEVATION = "elevation"
		lazy val TABLE_CREATE = """CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s LONG,
			%s TEXT, %s INTEGER, %s INTEGER, %s TEXT, %s TEXT, %s INTEGER)"""
			.format(TABLE, _ID, TS, NAME, LAT, LON, SYMBOL, COMMENT, ELEVATION)
		lazy val COLUMNS = Array(_ID, TS, NAME, LAT, LON, SYMBOL, COMMENT, ELEVATION)
		val COLUMN_TS = 1
		val COLUMN_NAME = 2
		val COLUMN_LAT = 3
		val COLUMN_LON = 4
		val COLUMN_SYMBOL = 5
		val COLUMN_COMMENT = 6
		val COLUMN_ELEVATION = 7
	}

	object GPXTrack {
		val TABLE = "gpx_tracks"
		val _ID = "_id"
		val TS = "ts"
		val NAME = "name"
		val LAT = "lat"
		val LON = "lon"
		val ELEVATION = "elevation"
		lazy val TABLE_CREATE = """CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s LONG,
			%s TEXT, %s INTEGER, %s INTEGER, %s INTEGER)"""
			.format(TABLE, _ID, TS, NAME, LAT, LON, ELEVATION)
		lazy val COLUMNS = Array(_ID, TS, NAME, LAT, LON, ELEVATION)
		val COLUMN_TS = 1
		val COLUMN_NAME = 2
		val COLUMN_LAT = 3
		val COLUMN_LON = 4
		val COLUMN_ELEVATION = 5
	}

	object GPXMetadata {
		val TABLE = "gpx_metadata"
		val _ID = "_id"
		val TS = "ts"
		val VISIBLE = "visible"
		lazy val TABLE_CREATE = """CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s LONG UNIQUE,
			%s INTEGER DEFAULT 1)"""
			.format(TABLE, _ID, TS, VISIBLE)
		lazy val COLUMNS = Array(_ID, TS, VISIBLE)
		val COLUMN_TS = 1
		val COLUMN_VISIBLE = 2
	}

	var singleton : StorageDatabase = null
	def open(context : Context) : StorageDatabase = {
		if (singleton == null) {
			Log.d(TAG, "open(): instanciating StorageDatabase")
			singleton = new StorageDatabase(context.getApplicationContext())
		}
		singleton
	}

	def cursor2call(c : Cursor) : String = {
		val msgidx = c.getColumnIndex(Post.MESSAGE)
		val callidx = c.getColumnIndex(Station.CALL)
		if (msgidx != -1 && callidx == -1) { // Post table
			val t = c.getInt(Post.COLUMN_TYPE)
			if (t == Post.TYPE_POST || t == Post.TYPE_INCMG)
				c.getString(msgidx).split(">")(0)
			else
				null
		} else
			c.getString(callidx)
	}
}

class StorageDatabase(context : Context) extends
		SQLiteOpenHelper(context, StorageDatabase.DB_NAME,
			null, StorageDatabase.DB_VERSION) {
	import StorageDatabase._

	override def onCreate(db: SQLiteDatabase) {
		Log.d(TAG, "onCreate(): creating new database " + DB_NAME);
		db.execSQL(Post.TABLE_CREATE);
		db.execSQL(Station.TABLE_CREATE)
		// index on call is implicit due to UNIQUE
		Array("lat", "lon").map(col => db.execSQL(TABLE_INDEX.format(Station.TABLE, col)))
		db.execSQL(Position.TABLE_CREATE)
		db.execSQL(Message.TABLE_CREATE)
		// version 4
		Array(Position.TABLE, Station.TABLE).map(tab => db.execSQL(TABLE_INDEX.format(tab, "ts")))
		Array("call", "type").map(col => db.execSQL(TABLE_INDEX.format(Message.TABLE, col)))
		// version 5 - GPX support
		db.execSQL(GPXWaypoint.TABLE_CREATE)
		db.execSQL(GPXTrack.TABLE_CREATE)
		Array(GPXWaypoint.TABLE, GPXTrack.TABLE).map(tab => db.execSQL(TABLE_INDEX.format(tab, "ts")))
		// version 6 - GPX management metadata
		db.execSQL(GPXMetadata.TABLE_CREATE)
		db.execSQL(TABLE_INDEX.format(GPXMetadata.TABLE, "ts"))
	}

	override def onUpgrade(db: SQLiteDatabase, from : Int, to : Int) {
		if (from <= 1 && to <= 3) {
			db.execSQL(Message.TABLE_CREATE)
		}
		if (from == 2 && to <= 3) {
			db.execSQL("ALTER TABLE message RENAME TO messages") // make names consistent
		}
		if (from <= 2 && to <= 3) {
			db.execSQL("DROP TABLE position") // old name
			db.execSQL(Station.TABLE_CREATE)
			db.execSQL(Position.TABLE_CREATE)
		}
		if (to <= 4) {
			Array(Position.TABLE, Station.TABLE).map(tab => db.execSQL(TABLE_INDEX.format(tab, "ts")))
			Array("call", "type").map(col => db.execSQL(TABLE_INDEX.format(Message.TABLE, col)))
		}
		if (from <= 4 && to >= 5) {
			// version 5 - GPX support
			db.execSQL(GPXWaypoint.TABLE_CREATE)
			db.execSQL(GPXTrack.TABLE_CREATE)
			Array(GPXWaypoint.TABLE, GPXTrack.TABLE).map(tab => db.execSQL(TABLE_INDEX.format(tab, "ts")))
		}
		if (from <= 5 && to >= 6) {
			// version 6 - GPX management metadata
			db.execSQL(GPXMetadata.TABLE_CREATE)
			db.execSQL(TABLE_INDEX.format(GPXMetadata.TABLE, "ts"))
		}
	}

	def trimPosts(ts : Long) = Benchmark("trimPosts") {
		//Log.d(TAG, "StorageDatabase.trimPosts")
		getWritableDatabase().execSQL("DELETE FROM %s WHERE %s < ?".format(Post.TABLE, Post.TS),
			Array(long2Long(ts)))
		getWritableDatabase().execSQL("DELETE FROM %s WHERE %s < ?".format(Position.TABLE, Position.TS),
			Array(long2Long(ts)))
		// only trim stations on explicit request
		if (ts == Long.MaxValue)
			getWritableDatabase().execSQL("DELETE FROM %s WHERE %s < ?".format(Station.TABLE, Station.TS),
				Array(long2Long(ts)))
	}

	// default trim filter: 2 days in [ms]
	def trimPosts() : Unit = trimPosts(System.currentTimeMillis - 2L * 24 * 3600 * 1000)

	def addPosition(ts : Long, ap : APRSPacket, pos : Position, cse : CourseAndSpeedExtension, objectname : String) {
		import Station._
		val cv = new ContentValues()
		val call = ap.getSourceCall()
		val lat = (pos.getLatitude()*1000000).asInstanceOf[Int]
		val lon = (pos.getLongitude()*1000000).asInstanceOf[Int]
		val sym = "%s%s".format(pos.getSymbolTable(), pos.getSymbolCode())
		val comment = ap.getAprsInformation().getComment()
		val qrg = AprsPacket.parseQrg(comment)
		cv.put(TS, ts.asInstanceOf[java.lang.Long])
		cv.put(CALL, if (objectname != null) objectname else call)
		cv.put(LAT, lat.asInstanceOf[java.lang.Integer])
		cv.put(LON, lon.asInstanceOf[java.lang.Integer])
		// add the position into positions table
		getWritableDatabase().insertOrThrow(Position.TABLE, CALL, cv)

		if (objectname != null)
			cv.put(ORIGIN, call)
		cv.put(SYMBOL, sym)
		cv.put(COMMENT, comment)
		cv.put(QRG, qrg)
		if (cse != null) {
			cv.put(SPEED, cse.getSpeed().asInstanceOf[java.lang.Integer])
			cv.put(COURSE, cse.getCourse().asInstanceOf[java.lang.Integer])
		}
		Log.d(TAG, "got %s(%d, %d)%s -> %s".formatLocal(null, call, lat, lon, sym, comment))
		// replace the full station info in stations table
		getWritableDatabase().replaceOrThrow(TABLE, CALL, cv)
	}

	def isMessageDuplicate(call : String, msgid : String, text : String) : Boolean = {
		val c = getReadableDatabase().query(Message.TABLE, Message.COLUMNS,
			"type = 1 AND call = ? AND msgid = ? AND text = ?",
			Array(call, msgid, text),
			null, null,
			null, null)
		val result = (c.getCount() > 0)
		c.close()
		result
	}

	// add an incoming message, returns false if duplicate
	def addMessage(ts : Long, srccall : String, msg : MessagePacket) : Boolean = {
		import Message._
		if (isMessageDuplicate(srccall, msg.getMessageNumber(), msg.getMessageBody())) {
			Log.i(TAG, "received duplicate message from %s: %s".format(srccall, msg))
			return false
		}
		val cv = new ContentValues()
		cv.put(TS, ts.asInstanceOf[java.lang.Long])
		cv.put(RETRYCNT, 0.asInstanceOf[java.lang.Integer])
		cv.put(CALL, srccall)
		cv.put(MSGID, msg.getMessageNumber())
		cv.put(TYPE, TYPE_INCOMING.asInstanceOf[java.lang.Integer])
		cv.put(TEXT, msg.getMessageBody())
		addMessage(cv)
		true
	}

	def getStations(sel : String, selArgs : Array[String], limit : String) : Cursor = {
		getReadableDatabase().query(Station.TABLE, Station.COLUMNS_MAP,
			sel, selArgs,
			null, null, "CALL", limit)
	}

	def getRectStations(lat1 : Int, lon1 : Int, lat2 : Int, lon2 : Int, limit : String) : Cursor = {
		Log.d(TAG, "StorageDatabase.getRectStations: %d,%d - %d,%d".formatLocal(null, lat1, lon1, lat2, lon2))
		// check for areas overflowing between +180 and -180 degrees
		val QUERY = if (lon1 <= lon2) "LAT >= ? AND LAT <= ? AND LON >= ? AND LON <= ?"
					else  "LAT >= ? AND LAT <= ? AND (LON <= ? OR LON >= ?)"
		getStations(QUERY,
			Array(lat1, lat2, lon1, lon2).map(_.toString), limit)
	}

	def getStaPosition(call : String) : Cursor = {
		getReadableDatabase().query(Station.TABLE, Station.COLUMNS,
			"call LIKE ?", Array(call),
			null, null, "_ID DESC", "1")
	}
	def getAllStaPositions(limit : String) : Cursor = {
		getReadableDatabase().query(Position.TABLE, Position.COLUMNS,
			"TS > ?", Array(limit),
			null, null, "CALL, _ID", null)
	}
	def getAllSsids(call : String) : Cursor = {
		val barecall = call.split("[- _]+")(0)
		val wildcard = barecall + "-%"
		getReadableDatabase().query(Station.TABLE, Station.COLUMNS,
			"call = ? OR call LIKE ? OR origin = ? OR origin LIKE ?", Array(barecall, wildcard, barecall, wildcard),
			null, null, null, null)
	}
	def getNeighbors(mycall : String, lat : Int, lon : Int, ts : Long, limit : String) : Cursor = {
		// calculate latitude correction
		val corr = (cos(Pi*lat/180000000.0)*cos(Pi*lat/180000000.0)*100).toInt
		//Log.d(TAG, "getNeighbors: correcting by %d".formatLocal(null, corr))
		// add a distance column to the query
		val newcols = Station.COLUMNS :+ Station.COL_DIST.formatLocal(null, lat, lat, lon, lon, corr)
		getReadableDatabase().query(Station.TABLE, newcols,
			"ts > ? or call = ?", Array(ts.toString, mycall),
			null, null, "dist", limit)
	}

	def getNeighborsLike(call : String, lat : Int, lon : Int, ts : Long, limit : String) : Cursor = {
		// calculate latitude correction
		val corr = (cos(Pi*lat/180000000.0)*cos(Pi*lat/180000000.0)*100).toInt
		Log.d(TAG, "getNeighborsLike: correcting by %d".formatLocal(null, corr))
		// add a distance column to the query
		val newcols = Station.COLUMNS :+ Station.COL_DIST.formatLocal(null, lat, lat, lon, lon, corr)
		getReadableDatabase().query(Station.TABLE, newcols,
			"call like ?", Array(call),
			null, null, "dist", limit)
	}

	def addPost(ts : Long, posttype : Int, status : String, message : String) {
		val cv = new ContentValues()
		cv.put(Post.TS, ts.asInstanceOf[java.lang.Long])
		cv.put(Post.TYPE, posttype.asInstanceOf[java.lang.Integer])
		cv.put(Post.STATUS, status)
		cv.put(Post.MESSAGE, message)
		getWritableDatabase().insertOrThrow(Post.TABLE, Post.MESSAGE, cv)
		if (Post.trimCounter == 0) {
			trimPosts()
			Post.trimCounter = 100
		} else Post.trimCounter -= 1
	}

	def getPosts(sel : String, selArgs : Array[String], limit : String) : Cursor = {
		getWritableDatabase().query(Post.TABLE, Post.COLUMNS,
			sel, selArgs,
			null, null, "_ID DESC", limit)
	}

	def getPosts(limit : String) : Cursor = getPosts(null, null, limit)

	def getPosts() : Cursor = getPosts(null)

	def getStaPosts(call : String, limit : String) : Cursor = {
		val start = "%s%%".format(call)		// match for call-originated messages
		val obj1 = "%%;%s%%".format(call)	// ;call - object
		val obj2 = "%%)%s%%".format(call)	// )call - item
		getPosts("message LIKE ? OR message LIKE ? OR message LIKE ?",
			Array(start, obj1, obj2), limit)
	}

	def getExportPosts(call : String) : Cursor = {
                if (call != null)
                        getWritableDatabase().query(Post.TABLE, Post.COLUMNS,
                                "type in (0, 3) and message LIKE ?",
                                Array("%s%%".format(call)),
                                null, null, null, null)
                else
                        getWritableDatabase().query(Post.TABLE, Post.COLUMNS,
                                "type in (0, 3)", null,
                                null, null, null, null)
	}

	def getPostFilter(limit : String) : FilterQueryProvider = {
		new FilterQueryProvider() {
			def runQuery(constraint : CharSequence) : Cursor = {
				getPosts("MESSAGE LIKE ?", Array("%%%s%%".format(constraint)),
					limit)
			}

		}
	}

	def getMessages(call : String) = {
		getReadableDatabase().query(Message.TABLE, Message.COLUMNS,
			"call = ?", Array(call),
			null, null,
			null, null)
	}

	def getPendingMessages(retries : Int) = {
		getReadableDatabase().query(Message.TABLE, Message.COLUMNS,
			"type = 2 and retrycnt <= ?", Array(retries.toString),
			null, null,
			null, null)
	}

	def addMessage(cv : ContentValues) = {
		getWritableDatabase().insertOrThrow(Message.TABLE, "_id", cv)
	}
	def updateMessage(id : Long, cv : ContentValues) = {
		getWritableDatabase().update(Message.TABLE, cv, "_id = ?", Array(id.toString))
	}
	def updateMessageType(id : Long, msg_type : Int) = {
		val cv = new ContentValues()
		cv.put(Message.TYPE, msg_type.asInstanceOf[java.lang.Integer])
		updateMessage(id, cv)
	}

	def updateMessageAcked(call : String, msgid : String, new_type : Int) = {
		val cv = new ContentValues()
		cv.put(Message.TYPE, new_type.asInstanceOf[java.lang.Integer])
		getWritableDatabase().update(Message.TABLE, cv, "type = 2 AND call = ? AND msgid = ?",
			Array(call, msgid))
	}

	def createMsgId(call : String) = {
		val c = getReadableDatabase().query(Message.TABLE, Array("MAX(CAST(msgid AS INTEGER))"),
			"call = ? AND type != ?", Array(call, Message.TYPE_INCOMING.toString),
			null, null,
			null, null)
		c.moveToFirst()
		val result = if (c.getCount() == 0)
			0
		else c.getInt(0) + 1
		Log.d(TAG, "createMsgId(%s) = %d".formatLocal(null, call, result))
		c.close()
		result
	}
	def deleteMessages(call : String) {
		getWritableDatabase().execSQL("DELETE FROM %s WHERE %s = ?".format(Message.TABLE, Message.CALL),
			Array(call))
	}

	def getConversations() = {
		getReadableDatabase().query("(SELECT * FROM messages ORDER BY _id DESC)", Message.COLUMNS,
			null, null,
			"call", null,
			"_id DESC", null)
	}

	def addGPXWaypoint(ts: Long, name: String, lat: Int, lon: Int, symbol: String, comment: String, elevation: Int) {
		import GPXWaypoint._
		val cv = new ContentValues()
		cv.put(TS, ts.asInstanceOf[java.lang.Long])
		cv.put(NAME, name)
		cv.put(LAT, lat.asInstanceOf[java.lang.Integer])
		cv.put(LON, lon.asInstanceOf[java.lang.Integer])
		cv.put(SYMBOL, symbol)
		cv.put(COMMENT, comment)
		cv.put(ELEVATION, elevation.asInstanceOf[java.lang.Integer])
		getWritableDatabase().insertOrThrow(TABLE, NAME, cv)
	}

	def addGPXTrackPoint(ts: Long, name: String, lat: Int, lon: Int, elevation: Int) {
		import GPXTrack._
		val cv = new ContentValues()
		cv.put(TS, ts.asInstanceOf[java.lang.Long])
		cv.put(NAME, name)
		cv.put(LAT, lat.asInstanceOf[java.lang.Integer])
		cv.put(LON, lon.asInstanceOf[java.lang.Integer])
		cv.put(ELEVATION, elevation.asInstanceOf[java.lang.Integer])
		getWritableDatabase().insertOrThrow(TABLE, NAME, cv)
	}

	def getGPXWaypoints() = {
		getReadableDatabase().query(GPXWaypoint.TABLE, GPXWaypoint.COLUMNS,
			null, null, null, null, GPXWaypoint.TS + " DESC", null)
	}

	def getGPXTracks() = {
		getReadableDatabase().query(GPXTrack.TABLE, GPXTrack.COLUMNS,
			null, null, null, null, GPXTrack.TS + " ASC", null)
	}

	def clearGPXData() {
		getWritableDatabase().execSQL("DELETE FROM " + GPXWaypoint.TABLE)
		getWritableDatabase().execSQL("DELETE FROM " + GPXTrack.TABLE)
		getWritableDatabase().execSQL("DELETE FROM " + GPXMetadata.TABLE)
	}

	def getGPXWaypointCount(timestamp: Long): Int = {
		val cursor = getReadableDatabase().rawQuery(
			"SELECT COUNT(*) FROM " + GPXWaypoint.TABLE + " WHERE " + GPXWaypoint.TS + " = ?",
			Array(timestamp.toString)
		)
		try {
			cursor.moveToFirst()
			cursor.getInt(0)
		} finally {
			cursor.close()
		}
	}

	def getGPXTrackCount(timestamp: Long): Int = {
		val cursor = getReadableDatabase().rawQuery(
			"SELECT COUNT(DISTINCT " + GPXTrack.NAME + ") FROM " + GPXTrack.TABLE + " WHERE " + GPXTrack.TS + " = ?",
			Array(timestamp.toString)
		)
		try {
			cursor.moveToFirst()
			cursor.getInt(0)
		} finally {
			cursor.close()
		}
	}

	def getGPXVisibility(timestamp: Long): Boolean = {
		val cursor = getReadableDatabase().query(
			GPXMetadata.TABLE, GPXMetadata.COLUMNS,
			GPXMetadata.TS + " = ?", Array(timestamp.toString),
			null, null, null
		)
		try {
			if (cursor.moveToFirst()) {
				cursor.getInt(GPXMetadata.COLUMN_VISIBLE) == 1
			} else {
				// Default to visible if no metadata exists
				true
			}
		} finally {
			cursor.close()
		}
	}

	def setGPXVisibility(timestamp: Long, visible: Boolean) {
		import GPXMetadata._
		val cv = new ContentValues()
		cv.put(TS, timestamp.asInstanceOf[java.lang.Long])
		cv.put(VISIBLE, (if (visible) 1 else 0).asInstanceOf[java.lang.Integer])
		
		val db = getWritableDatabase()
		val rows = db.update(TABLE, cv, TS + " = ?", Array(timestamp.toString))
		if (rows == 0) {
			db.insert(TABLE, null, cv)
		}
	}

	def removeGPXData(timestamp: Long) {
		getWritableDatabase().execSQL(
			"DELETE FROM " + GPXWaypoint.TABLE + " WHERE " + GPXWaypoint.TS + " = ?",
			Array(timestamp.toString)
		)
		getWritableDatabase().execSQL(
			"DELETE FROM " + GPXTrack.TABLE + " WHERE " + GPXTrack.TS + " = ?",
			Array(timestamp.toString)
		)
		getWritableDatabase().execSQL(
			"DELETE FROM " + GPXMetadata.TABLE + " WHERE " + GPXMetadata.TS + " = ?",
			Array(timestamp.toString)
		)
	}

	def getVisibleGPXWaypoints() = {
		getReadableDatabase().rawQuery(
			"SELECT w._id, w.ts, w.name, w.lat, w.lon, w.symbol, w.comment, w.elevation FROM " + GPXWaypoint.TABLE + " w " +
			"LEFT JOIN " + GPXMetadata.TABLE + " m ON w." + GPXWaypoint.TS + " = m." + GPXMetadata.TS + " " +
			"WHERE m." + GPXMetadata.VISIBLE + " = 1 OR m." + GPXMetadata.VISIBLE + " IS NULL " +
			"ORDER BY w." + GPXWaypoint.TS + " DESC",
			null
		)
	}

	def getVisibleGPXTracks() = {
		getReadableDatabase().rawQuery(
			"SELECT t._id, t.ts, t.name, t.lat, t.lon, t.elevation FROM " + GPXTrack.TABLE + " t " +
			"LEFT JOIN " + GPXMetadata.TABLE + " m ON t." + GPXTrack.TS + " = m." + GPXMetadata.TS + " " +
			"WHERE m." + GPXMetadata.VISIBLE + " = 1 OR m." + GPXMetadata.VISIBLE + " IS NULL " +
			"ORDER BY t." + GPXTrack.TS + " ASC",
			null
		)
	}

}
