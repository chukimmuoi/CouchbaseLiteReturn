package com.chukimmuoi.couchbaselitereturn

import android.app.Application
import com.couchbase.lite.*
import com.couchbase.lite.android.AndroidContext
import com.couchbase.lite.auth.Authenticator
import com.couchbase.lite.auth.AuthenticatorFactory
import com.couchbase.lite.listener.Credentials
import com.couchbase.lite.listener.LiteListener
import com.couchbase.lite.replicator.Replication
import com.couchbase.lite.util.Log
import java.io.IOException
import java.net.URL

/**
 * @author  : Pos365
 * @Skype   : chukimmuoi
 * @Mobile  : +84 167 367 2505
 * @Email   : chukimmuoi@gmail.com
 * @Website : https://cafe365.pos365.vn/
 * @Project : CouchbaseLiteReturn
 * Created by chukimmuoi on 10/10/2018.
 */
class Application: Application(), Replication.ChangeListener {

    companion object {
        private val TAG = this::class.java.simpleName

        // Storage Type: .SQLITE_STORAGE or .FORESTDB_STORAGE
        private const val STORAGE_TYPE = Manager.SQLITE_STORAGE

        // Encryption (Don't store encryption key in the source code. We are doing it here just as an example):
        private const val PASSWORD_SYNC_DEFAULT = "!\$~&1d2E3xp0^S(3_6*5)#@"
        private const val USERNAME_SYNC_DEFAULT = "p0s365"

        private const val TYPE    = "type"
        private const val CONTENT = "content"

        private const val PORT_SYNC_DEFAULT = 55000

        private const val FORMAT_URL_SYNC_GATEWAY = "http://%1\$s:%2\$s/%3\$s/"

        private const val MAX_REVS = 5
    }

    private fun formatNameDB(branchName: String) = "db$branchName"

    // Creating a manager.
    // https://docs.couchbase.com/couchbase-lite/1.4/java.html#creating-a-manager
    private val mManager: Manager by lazy { getManager() }
    private fun getManager(): Manager {
        val context = AndroidContext(applicationContext)
        return Manager(context, Manager.DEFAULT_OPTIONS)
    }

    // Global logging settings
    // https://docs.couchbase.com/couchbase-lite/1.4/java.html#global-logging-settings
    private fun enableLogging() {
        if (BuildConfig.DEBUG) {
            Manager.enableLogging(TAG, Log.VERBOSE)
            Manager.enableLogging(Log.TAG, Log.VERBOSE)
            Manager.enableLogging(Log.TAG_SYNC_ASYNC_TASK, Log.VERBOSE)
            Manager.enableLogging(Log.TAG_SYNC, Log.VERBOSE)
            Manager.enableLogging(Log.TAG_QUERY, Log.VERBOSE)
            Manager.enableLogging(Log.TAG_VIEW, Log.VERBOSE)
            Manager.enableLogging(Log.TAG_DATABASE, Log.VERBOSE)
        }
    }

    override fun onCreate() {
        super.onCreate()
        enableLogging()
    }

    // Database
    // https://docs.couchbase.com/couchbase-lite/1.4/java.html#database
    private var mDatabase: Database? = null
    private fun getBranchDatabase(branchName: String): Database? {
        try {
            val dbName = formatNameDB(branchName)
            val options = DatabaseOptions()
            options.isCreate = true
            options.storageType = STORAGE_TYPE
            options.encryptionKey = null
            return mManager.openDatabase(dbName, options)
        } catch (e: CouchbaseLiteException) {
            Log.e(TAG, "Cannot create database for name: db$branchName", e)
        }
        return null
    }

    // Database notifications
    // https://docs.couchbase.com/couchbase-lite/1.4/java.html#database-notifications
    private fun registerNotifications(branchName: String) {
        try {
            val dbName = formatNameDB(branchName)
            val db = mManager.getExistingDatabase(dbName)
            db?.let {
                db.addChangeListener {
                    it.changes.forEach {
                        /* Access the document revision related to that change. */
                        val properties = it.addedRevision.body.properties
//                        val table = properties[TYPE].toString()
//                        val content = properties[CONTENT].toString()
//                        when (table) {
//                            ServerEvents.TABLE_NAME -> {
//                                val serverEvents = GsonBuilder().create().fromJson(content, object : TypeToken<ServerEvents>() {}.type) as ServerEvents
//                                val realTime = RealTime("SyncGateway", "Update", listOf(serverEvents))
//                                (mContext as CashierApplication).sendSyncGateway(realTime)
//                            }
//                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Cannot delete database", e)
        }
    }

    // Deleting a database
    // https://docs.couchbase.com/couchbase-lite/1.4/java.html#deleting-a-database
    private fun deleteDatabase() : Boolean {
        try {
            mDatabase?.let { it.delete() }
            mDatabase = null
            return true
        } catch (e: IOException) {
            Log.e(TAG, "Cannot delete database", e)
        }
        return false
    }

    fun insertOrUpdateDocument(typeName: String, uuid: String = "", content: Any) : Boolean {
        mDatabase?.let {
            val doc = it.getExistingDocument(uuid)
            return if (doc == null) {
                createDocument(typeName, uuid, content)
            } else {
                updateDocument(uuid, content)
            }
        }
        return false
    }

    // Creating documents
    // https://docs.couchbase.com/couchbase-lite/1.4/java.html#creating-documents
    private fun createDocument(typeName: String, uuid: String = "", content: Any) : Boolean {
        mDatabase?.let {
            val properties = HashMap<String, Any>()
            properties[TYPE]    = typeName
            properties[CONTENT] = content.toString()

            val document = if (uuid.isNullOrEmpty()) it.createDocument() else it.getDocument(uuid)
            return try {
                document.putProperties(properties)
                true
            } catch (e: CouchbaseLiteException) {
                Log.e(TAG, "Cannot save document", e)
                false
            }
        }
        return false
    }

    // Reading documents
    // https://docs.couchbase.com/couchbase-lite/1.4/java.html#reading-documents
    fun readDocument(uuid: String): String {
        mDatabase?.let {
            return try {
                val doc = it.getExistingDocument(uuid)
                val result = doc?.getProperty(CONTENT)?.toString() ?: ""
                result
            } catch (e: Exception) {
                Log.e(TAG, "Cannot read document", e)
                ""
            }
        }
        return ""
    }

    // Updating documents
    // https://docs.couchbase.com/couchbase-lite/1.4/java.html#updating-documents
    private fun updateDocument(uuid: String, content: Any) : Boolean {
        mDatabase?.let {
            val doc = it.getDocument(uuid)
            return try {
                doc.update {
                    val properties = it.properties
                    properties[CONTENT] = content.toString()
                    it.userProperties = properties
                    true
                }
                true
            } catch (e: CouchbaseLiteException) {
                Log.e(TAG, "Cannot update document", e)
                false
            }
        }
        return false
    }

    // Deleting documents
    // https://docs.couchbase.com/couchbase-lite/1.4/java.html#deleting-documents
    fun deleteDocument(uuid: String) : Boolean{
        mDatabase?.let {
            return try {
                val doc = it.getDocument(uuid)
                doc.delete()
                true
            } catch (e: CouchbaseLiteException) {
                false
            }
        }
        return false
    }

    // Document change notifications
    // https://docs.couchbase.com/couchbase-lite/1.4/java.html#document-change-notifications
    private fun documentChangeNotifications(doc: Document) {
        doc.addChangeListener {
            val docChange = it.change
            var msg = "New revision added: %s. Conflict: %s"
            msg = String.format(msg, docChange.addedRevision, docChange.isConflict)
            Log.d(TAG, msg)
            //docChange.countDown()
        }
        doc.createRevision().save()
    }

    // Replication
    // https://docs.couchbase.com/couchbase-lite/1.4/java.html#replication
    private var mPull: Replication? = null
    private var mPush: Replication? = null
    private var mCurrentURL = ""

    /**
     * @ipAddress ip server
     * @port PORT_SYNC_DEFAULT = 55000
     * @dbName name db
     * */
    private fun getSyncGatewayURL(ipAddress: String, port: Int, dbName: String) : URL {
        val syncString = String.format(FORMAT_URL_SYNC_GATEWAY, ipAddress, port, dbName)
        if (mCurrentURL != syncString) {
            stopReplication()
            mCurrentURL = syncString
        }
        return URL(syncString)
    }

    /**
     * @ipAddress ip server
     * @port PORT_SYNC_DEFAULT = 55000
     * @dbName name db
     * @username name db
     * @password ENCRYPTION_KEY
     * */
    private fun startReplication(ipAddress: String, port: Int, dbName: String, username: String, password: String) {
        val syncURL = getSyncGatewayURL(ipAddress, port, dbName)

        val  auth = AuthenticatorFactory.createBasicAuthenticator(username, password)

        startReplication(auth = auth, syncURL = syncURL)
    }

    private fun startReplication(auth: Authenticator, syncURL: URL) {
        try {
            if (mPull == null) {
                mDatabase?.let {
                    mPull = it.createPullReplication(syncURL)
                    mPull?.let {
                        it.isContinuous = true
                        it.authenticator = auth
                        it.addChangeListener(this)
                    }
                }
            }

            if (mPush == null) {
                mDatabase?.let {
                    mPush = it.createPushReplication(syncURL)
                    mPush?.let {
                        it.isContinuous = true
                        it.authenticator = auth
                        it.addChangeListener(this)
                    }
                }
            }

            mPull?.let {
                it.stop()
                it.start()
            }

            mPush?.let {
                it.stop()
                it.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopReplication() {
        try {
            mPull?.let {
                it.removeChangeListener(this)
                it.stop()
            }
            mPull = null

            mPush?.let {
                it.removeChangeListener(this)
                it.stop()
            }
            mPush = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun changed(event: Replication.ChangeEvent) {
        Log.e(TAG, "changed", "Do something")
    }

    // Peer-to-Peer
    // https://docs.couchbase.com/couchbase-lite/1.4/java.html#peer-to-peer
    private var mListenerSyncGateway: LiteListener? = null
    /**
     * @port PORT_SYNC_DEFAULT = 55000
     * @username name db
     * @password ENCRYPTION_KEY
     * */
    private fun startSyncGateway(port: Int, username: String, password: String) {
        try {
            stopSyncGateway()

            val credentials = Credentials(username, password)
            mListenerSyncGateway = LiteListener(mManager, port, credentials)
            mListenerSyncGateway?.let {
                val thread = Thread(it)
                thread.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopSyncGateway() {
        try {
            mListenerSyncGateway?.let { it.stop() }
            mListenerSyncGateway = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearCacheData() {
        stopListener()
        deleteDatabase()

        mDatabase = null
    }

    private fun onDestroyData() {
        stopListener()

        mDatabase = null
    }

    /**
     * @ipAddress ip server
     * @port PORT_SYNC_DEFAULT = 55000
     * @dbName name db
     * @username name db
     * @password ENCRYPTION_KEY
     * */
    private fun startListener(ipAddress: String = "", port: Int = PORT_SYNC_DEFAULT, dbName: String,
                              username: String = USERNAME_SYNC_DEFAULT,
                              password: String = PASSWORD_SYNC_DEFAULT) {
        if (ipAddress.isNullOrEmpty())  {
            startSyncGateway(port, username, password)
        } else {
            startReplication(ipAddress, port, dbName, username, password)
        }
    }

    private fun startListener(dbName: String) {
//        val screenType = mPreferencesHelper.getScreenType()
//        when(screenType) {
//            PreferencesHelper.VALUE_SCREEN_ORDER -> {
//                val ipAddress = mPreferencesHelper.getTypeSyncGateway()
//                if (!ipAddress.isNullOrEmpty()) {
//                    startListener(ipAddress = ipAddress, dbName = dbName, username = "$dbName$USERNAME_SYNC_DEFAULT")
//                }
//            }
//            PreferencesHelper.VALUE_SCREEN_CASHIERS -> {
//                startListener(dbName = dbName, username = "$dbName$USERNAME_SYNC_DEFAULT")
//            }
//        }
    }

    private fun stopListener() {
        stopReplication()
        stopSyncGateway()
    }

    fun useDbFollowBranchId() {
//        val currentBranch = mPreferencesHelper.getCurrentBranchId()
//
//        mDatabase = getBranchDatabase("$currentBranch")
//        mDatabase?.let {
//            val name = it.name
//            Timber.e("Db name = $name")
//
//            registerNotifications("$currentBranch")
//            startListener(name)
//        }
    }
}