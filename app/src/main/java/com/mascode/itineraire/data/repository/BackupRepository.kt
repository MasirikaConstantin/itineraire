package com.mascode.itineraire.data.repository

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import androidx.room.withTransaction
import com.mascode.itineraire.data.local.ItineraireDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

data class BackupSummary(
    val places: Int,
    val days: Int,
    val events: Int,
    val journeys: Int,
)

class BackupRepository(
    private val database: ItineraireDatabase,
    private val contentResolver: ContentResolver,
) {
    suspend fun exportTo(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val root = JSONObject()
            .put("format", BACKUP_FORMAT)
            .put("version", BACKUP_VERSION)
            .put("createdAt", Instant.now().toString())
        val data = JSONObject()
        val counts = mutableMapOf<String, Int>()

        database.withTransaction {
            val sqlite = database.openHelper.writableDatabase
            TABLES.forEach { table ->
                val rows = JSONArray()
                sqlite.query("SELECT * FROM $table").use { cursor ->
                    while (cursor.moveToNext()) rows.put(cursor.toJsonObject())
                }
                data.put(table, rows)
                counts[table] = rows.length()
            }
        }
        root.put("data", data)

        contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
            writer.write(root.toString())
        } ?: error("Impossible d'ouvrir le fichier de sauvegarde.")

        counts.toSummary()
    }

    suspend fun restoreFrom(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Impossible d'ouvrir la sauvegarde.")
        val root = runCatching { JSONObject(text) }
            .getOrElse { error("Le fichier sélectionné n'est pas une sauvegarde Itinéraire valide.") }
        require(root.optString("format") == BACKUP_FORMAT) {
            "Ce fichier ne provient pas de l'application Itinéraire."
        }
        require(root.optInt("version", -1) == BACKUP_VERSION) {
            "Cette version de sauvegarde n'est pas prise en charge."
        }
        val data = root.optJSONObject("data") ?: error("La sauvegarde ne contient aucune donnée.")
        val rowsByTable = TABLES.associateWith { table ->
            data.optJSONArray(table) ?: error("La table $table manque dans la sauvegarde.")
        }
        validateColumns(rowsByTable)

        database.withTransaction {
            val sqlite = database.openHelper.writableDatabase
            DELETE_ORDER.forEach { table -> sqlite.execSQL("DELETE FROM $table") }
            TABLES.forEach { table ->
                val rows = rowsByTable.getValue(table)
                for (index in 0 until rows.length()) {
                    val row = rows.getJSONObject(index)
                    val columns = row.keys().asSequence().toList()
                    val placeholders = List(columns.size) { "?" }.joinToString(",")
                    val sql = "INSERT INTO $table (${columns.joinToString(",")}) VALUES ($placeholders)"
                    sqlite.execSQL(sql, columns.map { column -> row.sqliteValue(column) }.toTypedArray())
                }
            }
        }

        rowsByTable.mapValues { it.value.length() }.toSummary()
    }

    private fun validateColumns(rowsByTable: Map<String, JSONArray>) {
        val sqlite = database.openHelper.readableDatabase
        rowsByTable.forEach { (table, rows) ->
            val allowedColumns = mutableSetOf<String>()
            sqlite.query("PRAGMA table_info($table)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) allowedColumns += cursor.getString(nameIndex)
            }
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index)
                    ?: error("Une ligne de la table $table est invalide.")
                val columns = row.keys().asSequence().toSet()
                require(columns.isNotEmpty() && columns.all { it in allowedColumns }) {
                    "La structure de la table $table est invalide."
                }
            }
        }
    }

    private fun Cursor.toJsonObject(): JSONObject = JSONObject().also { row ->
        columnNames.forEachIndexed { index, name ->
            val value: Any = when (getType(index)) {
                Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                Cursor.FIELD_TYPE_INTEGER -> getLong(index)
                Cursor.FIELD_TYPE_FLOAT -> getDouble(index)
                Cursor.FIELD_TYPE_STRING -> getString(index)
                else -> error("Le type de la colonne $name ne peut pas être sauvegardé.")
            }
            row.put(name, value)
        }
    }

    private fun JSONObject.sqliteValue(column: String): Any? = when (val value = get(column)) {
        JSONObject.NULL -> null
        is Number, is String -> value
        else -> error("La valeur de $column est invalide.")
    }

    private fun Map<String, Int>.toSummary() = BackupSummary(
        places = getValue("places"),
        days = getValue("day_logs"),
        events = getValue("day_events"),
        journeys = getValue("journeys"),
    )

    private companion object {
        const val BACKUP_FORMAT = "com.mascode.itineraire.backup"
        const val BACKUP_VERSION = 1

        val TABLES = listOf(
            "day_logs",
            "places",
            "local_account",
            "app_security",
            "day_events",
            "journeys",
            "journey_legs",
            "journey_observations",
            "quick_actions",
        )
        val DELETE_ORDER = TABLES.asReversed()
    }
}
