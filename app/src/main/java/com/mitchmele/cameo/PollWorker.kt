package com.mitchmele.cameo

import android.app.PendingIntent
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.mitchmele.cameo.model.GalleryItem
import com.mitchmele.cameo.network.PhotoFetcher
import com.mitchmele.cameo.util.CameoConstants
import com.mitchmele.cameo.util.CameoConstants.NOTIFICATION_CHANNEL_ID
import com.mitchmele.cameo.util.CameoConstants.WORKER_TAG
import com.mitchmele.cameo.util.QueryPreferences

class PollWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {

        val query = QueryPreferences.getStoredQuery(context)
        val lastResultId = QueryPreferences.getLastResultId(context)
        val items: List<GalleryItem> = when {
            query.isEmpty() -> {
                PhotoFetcher().fetchPhotoRequest()
                    .execute()
                    .body()
                    ?.photos
                    ?.galleryItems
            }
            else -> {
                PhotoFetcher().searchPhotoRequest(query)
                    .execute()
                    .body()
                    ?.photos
                    ?.galleryItems
            }
        } ?: emptyList()

        if (items.isEmpty()) return Result.success()

        val resultId = items.first().id

        when {
            resultId == lastResultId -> {
                Log.i(WORKER_TAG, "Got an old result: $resultId")
            }
            else -> {
                Log.i(WORKER_TAG, "Got an new result: $resultId")
                QueryPreferences.setLastResultId(context, resultId)

                val intent = CameoActivity.newIntent(context)
                val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

                val resources = context.resources
                val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .run {
                        setTicker(resources.getString(R.string.new_pictures_title))
                        setSmallIcon(android.R.drawable.ic_menu_report_image)
                        setContentTitle(resources.getString(R.string.new_pictures_title))
                        setContentText(resources.getString(R.string.new_pictures_text))
                        setContentIntent(pendingIntent)
                        setAutoCancel(true)
                        build()
                    }
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.notify(0, notification)
            }
        }
        return Result.success()
    }
}