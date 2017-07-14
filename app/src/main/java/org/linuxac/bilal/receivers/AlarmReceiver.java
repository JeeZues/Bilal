/*
 *  Copyright © 2015 Djalel Chefrour
 * 
 *  This file is part of Bilal.
 *
 *  Bilal is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Bilal is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Bilal.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.linuxac.bilal.receivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import org.linuxac.bilal.AlarmScheduler;
import org.linuxac.bilal.AthanService;
import org.linuxac.bilal.R;
import org.linuxac.bilal.activities.MainActivity;
import org.linuxac.bilal.activities.StopAthanActivity;
import org.linuxac.bilal.helpers.PrayerTimes;
import org.linuxac.bilal.helpers.UserSettings;

import static android.content.Context.VIBRATOR_SERVICE;

public class AlarmReceiver extends BroadcastReceiver
{
    protected static final String TAG = "AlarmReceiver";
    public static final String EXTRA_PRAYER_INDEX = "org.linuxac.bilal";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String alarmTxt = intent.getStringExtra(EXTRA_PRAYER_INDEX);
        Log.i(TAG, "Athan alarm is ON: " + alarmTxt);

        int index = Integer.parseInt(alarmTxt.substring(0,1));          // 1st char = prayer index

        if (UserSettings.isVibrateEnabled(context)) {
            // this is independent of notification setVibrate
            Vibrator vibrator = (Vibrator)context.getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(new long[] {1000, 1000, 1000, 1000, 1000, 1000, 1000}, -1);
        }

        if (UserSettings.isAthanEnabled(context)) {
            // don't use notification setSound as system can stop and we can't!
            Intent audioIntent = new Intent(context, AthanService.class);
            audioIntent.putExtra(EXTRA_PRAYER_INDEX, index);
            context.startService(audioIntent);
        }

        if (UserSettings.isNotificationEnabled(context)) {
            showNotification(context, index);
        }

        // Broadcast to MainActivity so it updates its screen if on
        Intent updateIntent = new Intent(MainActivity.UPDATE_VIEWS);
        context.sendBroadcast(updateIntent);

        // Re-arm alarm.
        AlarmScheduler.updatePrayerTimes(context, false);
    }

    private void showNotification(Context context, int index)
    {
        int notificationId = 0;

        // Use one intent to show MainActivity and another intent to stop athan (by notification
        // button or swipe left or volume button press)
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent activity = PendingIntent.getActivity(context, 0, intent, 0);
        PendingIntent stopAudioIntent = StopAthanActivity.getIntent(notificationId, context);

        Bitmap largeIconBmp = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_notif_large);
        /*Keep this in case we need it
        Resources res = context.getResources();
        int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
        int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
        largeIconBmp = Bitmap.createScaledBitmap(largeIconBmp, width, height, false);*/


        String contentTitle = String.format(context.getString(R.string.time_for),
                PrayerTimes.getName(context, index));
        String contentTxt = String.format(context.getString(R.string.time_in),
                AlarmScheduler.sCityName); // TODO UserSetting for city name
        String actionTxt = context.getString(R.string.stop_athan);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setSmallIcon(R.drawable.ic_notif)
                        .setContentTitle(contentTitle)
                        .setContentText(contentTxt)
                        .setContentIntent(activity)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setDeleteIntent(stopAudioIntent)
                        .setLargeIcon(largeIconBmp)
                        .addAction(R.drawable.ic_stop_athan, actionTxt, stopAudioIntent);

        // TODO: add a timeout (till Iqama) with android O

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Build the notification and issue it
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}
