package com.chukimmuoi.couchbaselitereturn

import android.app.Application
import com.chukimmuoi.couchbaselitereturn.util.StringUtil
import com.couchbase.lite.*
import com.couchbase.lite.android.AndroidContext
import com.couchbase.lite.auth.Authenticator
import com.couchbase.lite.auth.AuthenticatorFactory
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
        const val TAG = "Application"

        // Storage Type: .SQLITE_STORAGE or .FORESTDB_STORAGE
        private const val STORAGE_TYPE = Manager.SQLITE_STORAGE

        // Encryption (Don't store encryption key in the source code. We are doing it here just as an example):
        private const val ENCRYPTION_KEY = "!\$~&1d2E3xp0^S(3_6*5)#@"

        // Logging:
        private const val LOGGING_ENABLED = true

        private const val TYPE = "type"
        private const val CONTENT = "content"
    }

    private fun formatNameDB(branchName: String) = "db${StringUtil.MD5(branchName)}Pos365"

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
            options.encryptionKey = ENCRYPTION_KEY
            return mManager.openDatabase(dbName, options)
        } catch (e: CouchbaseLiteException) {
            Log.e(TAG, "Cannot create database for name: db${branchName}Pos365", e)
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
    private fun readDocument(uuid: String): String {
        mDatabase?.let {
            val doc = it.getDocument(uuid)
            return try {
                doc.getProperty(CONTENT).toString()
            } catch (e: CouchbaseLiteException) {
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
    private fun deleteDocument(uuid: String) : Boolean{
        mDatabase?.let {
            val doc = it.getDocument(uuid)
            return try {
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
    private val mSyncUrl : URL by lazy { URL("") }
    private var mPull: Replication? = null
    private var mPush: Replication? = null

    private fun startReplication(username: String, password: String) {
        val  auth = AuthenticatorFactory.createBasicAuthenticator(username, password)
        startReplication(auth)
    }
    private fun startReplication(auth: Authenticator) {
        if (mPull == null) {
            mDatabase?.let {
                mPull = it.createPullReplication(mSyncUrl)
                mPull?.let {
                    it.isContinuous = true
                    it.authenticator = auth
                    it.addChangeListener(this)
                }
            }
        }

        if (mPush == null) {
            mDatabase?.let {
                mPush = it.createPushReplication(mSyncUrl)
                mPush?.let {
                    it.isContinuous = true
                    it.authenticator = auth
                    it.addChangeListener(this)
                }
            }
        }

        mPush?.let {
            it.stop()
            it.start()
        }

        mPush?.let {
            it.stop()
            it.start()
        }
    }

    private fun stopReplication() {
        mPull?.let {
            it.removeChangeListener(this)
            it.stop()
        }

        mPush?.let {
            it.removeChangeListener(this)
            it.stop()
        }
    }

    override fun changed(event: Replication.ChangeEvent) {

    }
}