package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.AppDatabase
import com.example.data.CallerRule
import com.example.data.FavoriteContact
import com.example.data.IgnoredContact
import com.example.data.LocalContact
import com.example.data.RecentCall
import com.example.data.SpamNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupRestoreResult(
    val success: Boolean,
    val rulesCount: Int = 0,
    val favoritesCount: Int = 0,
    val contactsCount: Int = 0,
    val spamCount: Int = 0,
    val ignoredCount: Int = 0,
    val recentCallsCount: Int = 0,
    val message: String = ""
)

object BackupManager {

    fun generateBackupFileName(): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return "omnidial_backup_${formatter.format(Date())}.bak"
    }

    suspend fun createBackupJson(context: Context): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val dao = db.appDao()
        val prefs = context.getSharedPreferences("kishan_dialer_prefs", Context.MODE_PRIVATE)

        val root = JSONObject()
        root.put("version", 2)
        root.put("schemaVersion", 2)
        root.put("appName", "OmniDial")
        root.put("timestamp", System.currentTimeMillis())
        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))

        // 1. Export Rules
        val rules = dao.getAllRulesList()
        val ruleArray = JSONArray()
        for (r in rules) {
            val obj = JSONObject()
            obj.put("name", r.name)
            obj.put("phoneNumberPattern", r.phoneNumberPattern)
            obj.put("isEnabled", r.isEnabled)
            obj.put("autoAnswer", r.autoAnswer)
            obj.put("answerDelaySec", r.answerDelaySec)
            obj.put("dtmfSequence", r.dtmfSequence)
            obj.put("dtmfDelayMs", r.dtmfDelayMs)
            obj.put("sendSms", r.sendSms)
            obj.put("smsMessage", r.smsMessage)
            obj.put("autoHangup", r.autoHangup)
            obj.put("hangupDelaySec", r.hangupDelaySec)
            obj.put("autoSpeakerphone", r.autoSpeakerphone)
            obj.put("autoMuteMic", r.autoMuteMic)
            obj.put("requiredWifiSsid", r.requiredWifiSsid)
            obj.put("requiredBluetoothDevice", r.requiredBluetoothDevice)
            ruleArray.put(obj)
        }
        root.put("rules", ruleArray)

        // 2. Preferences
        val prefsObj = JSONObject()
        prefsObj.put("theme_mode", prefs.getString("theme_mode", "system"))
        prefsObj.put("whatsapp_call_mode", prefs.getString("whatsapp_call_mode", "ask_learn"))
        prefsObj.put("call_answer_style", prefs.getString("call_answer_style", "swipe_slider"))
        prefsObj.put("favorite_card_style", prefs.getString("favorite_card_style", "bento"))
        prefsObj.put("confirm_fav_calls", prefs.getBoolean("confirm_fav_calls", true))
        prefsObj.put("confirm_speed_dial_call", prefs.getBoolean("confirm_speed_dial_call", true))
        prefsObj.put("ask_assign_unassigned_speed_dial", prefs.getBoolean("ask_assign_unassigned_speed_dial", true))
        prefsObj.put("default_start_tab", prefs.getInt("default_start_tab", 0))
        prefsObj.put("swipe_to_switch_panels", prefs.getBoolean("swipe_to_switch_panels", true))

        fun stringSetToJson(key: String): JSONArray {
            val arr = JSONArray()
            (prefs.getStringSet(key, emptySet()) ?: emptySet()).forEach { arr.put(it) }
            return arr
        }

        prefsObj.put("whatsapp_learned_choices", stringSetToJson("whatsapp_learned_choices"))
        prefsObj.put("not_spam_whitelist", stringSetToJson("not_spam_whitelist"))
        prefsObj.put("favorite_sort_orders", stringSetToJson("favorite_sort_orders"))
        prefsObj.put("speed_dial_assignments", stringSetToJson("speed_dial_assignments"))
        root.put("preferences", prefsObj)

        // 3. Favorites
        val favorites = dao.getAllFavoritesList()
        val favArray = JSONArray()
        for (f in favorites) {
            val obj = JSONObject()
            obj.put("name", f.name)
            obj.put("phoneNumber", f.phoneNumber)
            obj.put("label", f.label)
            obj.put("nickname", f.nickname ?: "")
            obj.put("avatarColor", f.avatarColor)
            obj.put("photoUri", f.photoUri ?: "")
            obj.put("speedDialSlot", f.speedDialSlot ?: -1)
            obj.put("sortOrder", f.sortOrder)
            favArray.put(obj)
        }
        root.put("favorites", favArray)

        // 4. Local Contacts
        val localContacts = dao.getAllLocalContactsList()
        val contactArray = JSONArray()
        for (c in localContacts) {
            val obj = JSONObject()
            obj.put("name", c.name)
            obj.put("phoneNumber", c.phoneNumber)
            obj.put("label", c.label)
            obj.put("nickname", c.nickname ?: "")
            obj.put("photoUri", c.photoUri ?: "")
            contactArray.put(obj)
        }
        root.put("localContacts", contactArray)

        // 5. Spam Numbers
        val spamList = dao.getAllSpamNumbersList()
        val spamArray = JSONArray()
        for (s in spamList) {
            val obj = JSONObject()
            obj.put("phoneNumber", s.phoneNumber)
            obj.put("label", s.label)
            obj.put("reportCount", s.reportCount)
            obj.put("isBlocked", s.isBlocked)
            spamArray.put(obj)
        }
        root.put("spamNumbers", spamArray)

        // 6. Ignored Contacts
        val ignoredList = dao.getAllIgnoredContactsList()
        val ignoredArray = JSONArray()
        for (ig in ignoredList) {
            val obj = JSONObject()
            obj.put("phoneNumber", ig.phoneNumber)
            obj.put("name", ig.name)
            obj.put("category", ig.category)
            obj.put("tag", ig.tag)
            obj.put("timestamp", ig.timestamp)
            ignoredArray.put(obj)
        }
        root.put("ignoredContacts", ignoredArray)

        // 7. Recent Calls
        val recentCalls = dao.getAllRecentCallsList()
        val recentArray = JSONArray()
        for (rc in recentCalls) {
            val obj = JSONObject()
            obj.put("callerName", rc.callerName ?: "")
            obj.put("phoneNumber", rc.phoneNumber)
            obj.put("callType", rc.callType)
            obj.put("timestamp", rc.timestamp)
            obj.put("durationSeconds", rc.durationSeconds)
            obj.put("simSlot", rc.simSlot)
            obj.put("note", rc.note ?: "")
            obj.put("reminderTime", rc.reminderTime ?: 0L)
            obj.put("ruleMatched", rc.ruleMatched ?: "")
            obj.put("callReason", rc.callReason ?: "")
            obj.put("communityTag", rc.communityTag ?: "")
            obj.put("isSpam", rc.isSpam)
            recentArray.put(obj)
        }
        root.put("recentCalls", recentArray)

        // Generate SHA-256 payload integrity checksum
        val signature = computePayloadSignature(
            rulesCount = ruleArray.length(),
            favsCount = favArray.length(),
            contactsCount = contactArray.length(),
            spamCount = spamArray.length(),
            ignoredCount = ignoredArray.length(),
            recentCount = recentArray.length(),
            timestamp = root.optLong("timestamp")
        )
        root.put("payloadSignature", signature)
        root.put("checksum", computeSha256(signature))

        root.toString(2)
    }

    private fun computePayloadSignature(
        rulesCount: Int,
        favsCount: Int,
        contactsCount: Int,
        spamCount: Int,
        ignoredCount: Int,
        recentCount: Int,
        timestamp: Long
    ): String {
        return "r=$rulesCount;f=$favsCount;c=$contactsCount;s=$spamCount;i=$ignoredCount;rc=$recentCount;t=$timestamp"
    }

    private fun computeSha256(input: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            input.hashCode().toString()
        }
    }

    suspend fun writeBackupToUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = createBackupJson(context)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json)
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getUriFileName(context: Context, uri: Uri): String? {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    return cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return uri.path?.substringAfterLast('/')
    }

    suspend fun restoreBackupFromUri(context: Context, uri: Uri): BackupRestoreResult = withContext(Dispatchers.IO) {
        try {
            val fileName = getUriFileName(context, uri)
            if (fileName != null && !fileName.contains("omnidial_backup_") && !fileName.endsWith(".bak") && !fileName.endsWith(".json")) {
                return@withContext BackupRestoreResult(
                    success = false,
                    message = "Selected file is not a valid OmniDial backup."
                )
            }

            val jsonContent = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        jsonContent.append(line)
                        line = reader.readLine()
                    }
                }
            }

            val root = JSONObject(jsonContent.toString())
            restoreBackupFromJsonRoot(context, root)
        } catch (e: Exception) {
            e.printStackTrace()
            BackupRestoreResult(
                success = false,
                message = "Failed to import backup file: ${e.localizedMessage ?: "Invalid format"}"
            )
        }
    }

    suspend fun restoreBackupFromJsonRoot(context: Context, root: JSONObject): BackupRestoreResult = withContext(Dispatchers.IO) {
        try {
            // Validate application identity and schema version
            val appName = root.optString("appName", "")
            if (appName.isNotBlank() && appName != "OmniDial") {
                return@withContext BackupRestoreResult(
                    success = false,
                    message = "Unrecognized backup source: $appName"
                )
            }

            val schemaVersion = root.optInt("schemaVersion", root.optInt("version", 1))
            if (schemaVersion > 2) {
                return@withContext BackupRestoreResult(
                    success = false,
                    message = "Backup format version ($schemaVersion) is newer than supported by this app version."
                )
            }

            // Validate integrity checksum if present (v2 format)
            if (root.has("checksum") && root.has("payloadSignature")) {
                val signature = root.getString("payloadSignature")
                val expectedChecksum = root.getString("checksum")
                val actualChecksum = computeSha256(signature)
                if (expectedChecksum != actualChecksum) {
                    return@withContext BackupRestoreResult(
                        success = false,
                        message = "Backup integrity verification failed: corrupt or modified file."
                    )
                }
            }

            val db = AppDatabase.getInstance(context)
            val dao = db.appDao()
            val prefs = context.getSharedPreferences("kishan_dialer_prefs", Context.MODE_PRIVATE)

            var restoredFavs = 0
            var restoredContacts = 0
            var restoredSpam = 0
            var restoredRules = 0

            // 1. Restore Preferences
            if (root.has("preferences")) {
                val prefsObj = root.getJSONObject("preferences")
                val editor = prefs.edit()
                if (prefsObj.has("theme_mode")) editor.putString("theme_mode", prefsObj.getString("theme_mode"))
                if (prefsObj.has("whatsapp_call_mode")) editor.putString("whatsapp_call_mode", prefsObj.getString("whatsapp_call_mode"))
                if (prefsObj.has("call_answer_style")) editor.putString("call_answer_style", prefsObj.getString("call_answer_style"))
                if (prefsObj.has("favorite_card_style")) editor.putString("favorite_card_style", prefsObj.getString("favorite_card_style"))
                if (prefsObj.has("confirm_fav_calls")) editor.putBoolean("confirm_fav_calls", prefsObj.getBoolean("confirm_fav_calls"))
                if (prefsObj.has("confirm_speed_dial_call")) editor.putBoolean("confirm_speed_dial_call", prefsObj.getBoolean("confirm_speed_dial_call"))
                if (prefsObj.has("ask_assign_unassigned_speed_dial")) editor.putBoolean("ask_assign_unassigned_speed_dial", prefsObj.getBoolean("ask_assign_unassigned_speed_dial"))
                if (prefsObj.has("default_start_tab")) editor.putInt("default_start_tab", prefsObj.getInt("default_start_tab"))
                if (prefsObj.has("swipe_to_switch_panels")) editor.putBoolean("swipe_to_switch_panels", prefsObj.getBoolean("swipe_to_switch_panels"))

                fun jsonToStringSet(key: String): Set<String> {
                    if (!prefsObj.has(key)) return emptySet()
                    val arr = prefsObj.getJSONArray(key)
                    val set = mutableSetOf<String>()
                    for (i in 0 until arr.length()) {
                        set.add(arr.getString(i))
                    }
                    return set
                }

                editor.putStringSet("whatsapp_learned_choices", jsonToStringSet("whatsapp_learned_choices"))
                editor.putStringSet("not_spam_whitelist", jsonToStringSet("not_spam_whitelist"))
                editor.putStringSet("favorite_sort_orders", jsonToStringSet("favorite_sort_orders"))
                editor.putStringSet("speed_dial_assignments", jsonToStringSet("speed_dial_assignments"))
                editor.apply()
            }

            // 2. Restore Favorites
            if (root.has("favorites")) {
                dao.clearAllFavorites()
                val favArray = root.getJSONArray("favorites")
                for (i in 0 until favArray.length()) {
                    val obj = favArray.getJSONObject(i)
                    val speedDial = obj.optInt("speedDialSlot", -1).let { if (it in 1..9) it else null }
                    val fav = FavoriteContact(
                        name = obj.getString("name"),
                        phoneNumber = obj.getString("phoneNumber"),
                        label = obj.optString("label", "Mobile"),
                        nickname = obj.optString("nickname").ifBlank { null },
                        avatarColor = obj.optLong("avatarColor", 0xFF2563EBL),
                        photoUri = obj.optString("photoUri").ifBlank { null },
                        speedDialSlot = speedDial,
                        sortOrder = obj.optInt("sortOrder", i)
                    )
                    dao.insertFavorite(fav)
                    restoredFavs++
                }
            }

            // 3. Restore Local Contacts
            if (root.has("localContacts")) {
                dao.clearAllLocalContacts()
                val contactArray = root.getJSONArray("localContacts")
                for (i in 0 until contactArray.length()) {
                    val obj = contactArray.getJSONObject(i)
                    val contact = LocalContact(
                        name = obj.getString("name"),
                        phoneNumber = obj.getString("phoneNumber"),
                        label = obj.optString("label", "Mobile"),
                        nickname = obj.optString("nickname").ifBlank { null },
                        photoUri = obj.optString("photoUri").ifBlank { null }
                    )
                    dao.insertLocalContact(contact)
                    restoredContacts++
                }
            }

            // 4. Restore Spam Numbers
            if (root.has("spamNumbers")) {
                dao.clearAllSpamNumbers()
                val spamArray = root.getJSONArray("spamNumbers")
                for (i in 0 until spamArray.length()) {
                    val obj = spamArray.getJSONObject(i)
                    val spam = SpamNumber(
                        phoneNumber = obj.getString("phoneNumber"),
                        label = obj.optString("label", "Suspected Spam"),
                        reportCount = obj.optInt("reportCount", 1),
                        isBlocked = obj.optBoolean("isBlocked", true)
                    )
                    dao.insertSpamNumber(spam)
                    restoredSpam++
                }
            }

            // 5. Restore Caller Rules if present
            if (root.has("rules")) {
                dao.clearAllRules()
                val rulesArray = root.getJSONArray("rules")
                for (i in 0 until rulesArray.length()) {
                    val obj = rulesArray.getJSONObject(i)
                    val rule = CallerRule(
                        name = obj.getString("name"),
                        phoneNumberPattern = obj.getString("phoneNumberPattern"),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        autoAnswer = obj.optBoolean("autoAnswer", true),
                        answerDelaySec = obj.optInt("answerDelaySec", 1),
                        dtmfSequence = obj.optString("dtmfSequence", ""),
                        dtmfDelayMs = obj.optLong("dtmfDelayMs", 800L),
                        sendSms = obj.optBoolean("sendSms", false),
                        smsMessage = obj.optString("smsMessage", ""),
                        autoHangup = obj.optBoolean("autoHangup", false),
                        hangupDelaySec = obj.optInt("hangupDelaySec", 2),
                        autoSpeakerphone = obj.optBoolean("autoSpeakerphone", false),
                        autoMuteMic = obj.optBoolean("autoMuteMic", false),
                        requiredWifiSsid = obj.optString("requiredWifiSsid", ""),
                        requiredBluetoothDevice = obj.optString("requiredBluetoothDevice", "")
                    )
                    dao.insertRule(rule)
                    restoredRules++
                }
            }

            // 6. Restore Ignored Contacts
            var restoredIgnored = 0
            if (root.has("ignoredContacts")) {
                dao.clearAllIgnoredContacts()
                val ignoredArray = root.getJSONArray("ignoredContacts")
                for (i in 0 until ignoredArray.length()) {
                    val obj = ignoredArray.getJSONObject(i)
                    val ignored = IgnoredContact(
                        phoneNumber = obj.getString("phoneNumber"),
                        name = obj.optString("name", obj.optString("contactName", "")),
                        category = obj.optString("category", "General"),
                        tag = obj.optString("tag", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                    dao.insertIgnoredContact(ignored)
                    restoredIgnored++
                }
            }

            // 7. Restore Recent Calls if present
            var restoredRecentCalls = 0
            if (root.has("recentCalls")) {
                val callArray = root.getJSONArray("recentCalls")
                for (i in 0 until callArray.length()) {
                    val obj = callArray.getJSONObject(i)
                    val call = RecentCall(
                        callerName = obj.optString("callerName").ifBlank { null },
                        phoneNumber = obj.getString("phoneNumber"),
                        callType = obj.optInt("callType", 1),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        durationSeconds = obj.optLong("durationSeconds", 0L),
                        simSlot = obj.optInt("simSlot", obj.optInt("simSlotIndex", 1)),
                        note = obj.optString("note").ifBlank { null },
                        reminderTime = obj.optLong("reminderTime", 0L).let { if (it > 0L) it else null },
                        ruleMatched = obj.optString("ruleMatched").ifBlank { null },
                        callReason = obj.optString("callReason").ifBlank { null },
                        communityTag = obj.optString("communityTag").ifBlank { null },
                        isSpam = obj.optBoolean("isSpam", false)
                    )
                    dao.insertRecentCall(call)
                    restoredRecentCalls++
                }
            }

            BackupRestoreResult(
                success = true,
                rulesCount = restoredRules,
                favoritesCount = restoredFavs,
                contactsCount = restoredContacts,
                spamCount = restoredSpam,
                ignoredCount = restoredIgnored,
                recentCallsCount = restoredRecentCalls,
                message = "Backup restored successfully ($restoredRules rules, $restoredFavs favorites, $restoredContacts contacts, $restoredRecentCalls recent calls, and preferences restored)."
            )
        } catch (e: Exception) {
            e.printStackTrace()
            BackupRestoreResult(
                success = false,
                message = "Failed to import backup: ${e.localizedMessage ?: "Invalid format"}"
            )
        }
    }

    fun getLocalBackupsDir(context: Context): File {
        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getExternalBackupsDir(context: Context): File? {
        val extDir = context.getExternalFilesDir(null) ?: return null
        val dir = File(extDir, "backups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun saveLocalBackup(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = createBackupJson(context)
            val fileName = generateBackupFileName()
            
            // 1. Save to private internal storage
            val internalDir = getLocalBackupsDir(context)
            val internalFile = File(internalDir, fileName)
            internalFile.writeText(json)

            // 2. Automatically sync to external app directory (Android/data/<package>/files/backups)
            try {
                val externalDir = getExternalBackupsDir(context)
                if (externalDir != null) {
                    val externalFile = File(externalDir, fileName)
                    externalFile.writeText(json)
                }
            } catch (e: Exception) {
                // Secondary external write is best-effort
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun listLocalBackups(context: Context): List<File> {
        val internalDir = getLocalBackupsDir(context)
        val discoveredFiles = mutableListOf<File>()

        // Helper to check if file looks like a backup
        fun isBackupFile(file: File): Boolean {
            if (!file.isFile) return false
            val name = file.name.lowercase()
            return name.endsWith(".bak") || name.endsWith(".json") || name.contains("backup") || name.contains("omnidial")
        }

        // List candidate directories across all potential storage locations
        val candidateDirs = mutableListOf<File>()
        candidateDirs.add(internalDir)
        candidateDirs.add(context.filesDir)

        getExternalBackupsDir(context)?.let { candidateDirs.add(it) }
        context.getExternalFilesDir(null)?.let { candidateDirs.add(it) }
        context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)?.let { candidateDirs.add(it) }
        context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)?.let { candidateDirs.add(it) }

        try {
            val pubDocs = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
            if (pubDocs != null && pubDocs.exists()) {
                candidateDirs.add(pubDocs)
                candidateDirs.add(File(pubDocs, "OmniDial"))
            }
            val pubDownloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (pubDownloads != null && pubDownloads.exists()) {
                candidateDirs.add(pubDownloads)
                candidateDirs.add(File(pubDownloads, "OmniDial"))
            }
        } catch (_: Throwable) {}

        for (dir in candidateDirs) {
            try {
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles() ?: continue
                    for (file in files) {
                        if (isBackupFile(file)) {
                            discoveredFiles.add(file)
                            // If found in another location, mirror it into internalDir so it remains visible
                            if (file.parentFile?.absolutePath != internalDir.absolutePath) {
                                try {
                                    val target = File(internalDir, file.name)
                                    if (!target.exists() || target.lastModified() < file.lastModified()) {
                                        file.copyTo(target, overwrite = true)
                                    }
                                } catch (_: Throwable) {}
                            }
                        }
                    }
                }
            } catch (_: Throwable) {}
        }

        // Return deduplicated by file name, newest first
        return discoveredFiles
            .groupBy { it.name }
            .map { entry -> entry.value.maxByOrNull { it.lastModified() } ?: entry.value.first() }
            .sortedByDescending { it.lastModified() }
    }

    suspend fun restoreBackupFromFile(context: Context, file: File): BackupRestoreResult = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) {
                return@withContext BackupRestoreResult(success = false, message = "File does not exist")
            }
            val jsonContent = file.readText()
            val root = JSONObject(jsonContent)
            restoreBackupFromJsonRoot(context, root)
        } catch (e: Exception) {
            e.printStackTrace()
            BackupRestoreResult(
                success = false,
                message = "Failed to import backup file: ${e.localizedMessage ?: "Invalid format"}"
            )
        }
    }

    suspend fun deleteLocalBackup(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (file.exists()) {
                file.delete()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
