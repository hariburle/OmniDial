package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.buffer
import okio.source

class ContactPhotoFetcher(
    private val context: Context,
    private val uri: Uri
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val cr = context.contentResolver
        val inputStream = try {
            ContactsContract.Contacts.openContactPhotoInputStream(cr, uri, true)
                ?: ContactsContract.Contacts.openContactPhotoInputStream(cr, uri, false)
                ?: cr.openInputStream(uri)
        } catch (_: Exception) {
            try {
                cr.openInputStream(uri)
            } catch (_: Exception) {
                null
            }
        } ?: return null

        return SourceResult(
            source = ImageSource(inputStream.source().buffer(), context),
            mimeType = "image/jpeg",
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val authority = data.authority ?: return null
            val isContactUri = authority == ContactsContract.AUTHORITY ||
                    authority == "com.android.contacts" ||
                    authority == "contacts" ||
                    data.toString().contains("contacts", ignoreCase = true)
            if (!isContactUri) return null
            return ContactPhotoFetcher(context, data)
        }
    }
}
