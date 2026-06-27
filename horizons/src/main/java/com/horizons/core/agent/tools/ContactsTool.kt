package com.horizons.core.agent.tools

import android.content.Context
import android.provider.ContactsContract
import com.horizons.core.agent.ToolResult
import org.json.JSONArray
import org.json.JSONObject

class ContactsTool(private val context: Context) {

    fun search(query: String): ToolResult {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
        )
        val q = "%${query.replace("'", "''")}%"
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"

        return try {
            val cursor = context.contentResolver.query(uri, projection, selection, arrayOf(q),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC")
                ?: return ToolResult("search_contacts", false, "ContentResolver returned null")

            val results = mutableMapOf<String, JSONObject>()
            cursor.use { c ->
                while (c.moveToNext()) {
                    val name  = c.getString(0) ?: continue
                    val phone = c.getString(1) ?: continue
                    val type  = c.getInt(2)
                    val label = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                        context.resources, type, ""
                    ).toString()
                    results.getOrPut(name) { JSONObject().apply { put("name", name); put("phones", JSONArray()) }  }
                        .getJSONArray("phones").put(JSONObject().apply { put("number", phone); put("type", label) })
                }
            }

            // Also search emails
            val emailUri = ContactsContract.CommonDataKinds.Email.CONTENT_URI
            val emailProj = arrayOf(
                ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Email.ADDRESS,
            )
            context.contentResolver.query(emailUri, emailProj, selection, arrayOf(q), null)?.use { c ->
                while (c.moveToNext()) {
                    val name  = c.getString(0) ?: continue
                    val email = c.getString(1) ?: continue
                    results.getOrPut(name) { JSONObject().apply { put("name", name); put("phones", JSONArray()) } }
                        .apply {
                            if (!has("email")) put("email", email)
                        }
                }
            }

            val arr = JSONArray(results.values.toList())
            if (arr.length() == 0)
                ToolResult("search_contacts", true, "[]")
            else
                ToolResult("search_contacts", true, arr.toString())
        } catch (e: SecurityException) {
            ToolResult("search_contacts", false, "Permission denied — grant READ_CONTACTS in Settings → App permissions")
        } catch (e: Exception) {
            ToolResult("search_contacts", false, e.message ?: "contacts query failed")
        }
    }
}
