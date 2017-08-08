/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package au.com.brian.timer.util;

import java.util.Date;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import au.com.brian.timer.R;
import au.com.brian.timer.activity.CookingTimerActivity;
import au.com.brian.timer.domain.Recipe;
import au.com.brian.timer.domain.SavedRecipe;
import au.com.brian.timer.domain.Task;

/**
 * This is an example of implement an {@link BroadcastReceiver} for an alarm that
 * should occur once.
 * <p>
 * When the alarm goes off, we show a <i>Toast</i>, a quick message.
 */
public class OneShotAlarm2 extends BroadcastReceiver
{
	
	private static final String TAG = "OneShotAlarm2";
	private Context context;
	private Intent intent;
	private static final int MSG_ID = 0;
	private static int count = 0;
	
    @Override
    public void onReceive(Context context, Intent intent)
    {
    	count++;
    	if (count > 3) return;
    	this.context = context;
    	this.intent = intent;
    	// FIXME - we are still getting the same message here everytime. can we clear it somehow in this method?
       	Log.i(TAG, "Alarm at " + (new Date()) + ", message = " + intent.getIntExtra("message", -1));
        Recipe r = SavedRecipe.getRecipe(context);
    	Task t = r.getTaskForId(intent.getIntExtra("message", -1));
    	Log.i(TAG, "task = " + t);
    	if (t == null) return;
    	makeNotification(t);
        t.setComplete(true);
        SavedRecipe.setComplete(context,t.getId(),true);
        Task next = r.getNextAlarm();
    	Log.i(TAG, "next task = " + next);
   	    setAlarm(next.getId(),next.getAlarmTime().getTimeInMillis());
    }
    
    private void makeNotification(final Task t) {
        // construct the Notification object.
        Notification notif = new Notification(R.drawable.icon, t.getTitle(),
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, IncomingMessageView.class), 0);
        // Set the info for the views that show in the notification panel.
        notif.setLatestEventInfo(context, "Cooking Timer - " + t.getTitle(), "more detail goes here", contentIntent);

        // after a 100ms delay, vibrate for 250ms, pause for 100 ms and
        // then vibrate for 500ms.
        notif.vibrate = new long[] { 100, 250, 200, 500, 200, 500, 200, 500};
        notif.defaults |= Notification.DEFAULT_SOUND;
        notif.defaults |= Notification.DEFAULT_LIGHTS;
        notif.defaults |= Notification.FLAG_INSISTENT; // does this work? do I need to play music instead of the default sound?
        
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(t.getId(), notif);
    }
    
    private void setAlarm(int id, long finishTime) {
    	// TODO - if we are the finish task, also call stop (but how?)
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent i = getPendingIntent(id);
        am.set(AlarmManager.RTC_WAKEUP, finishTime, i);
        Log.i(TAG, "Set task \"" + id + "\" alarm for " + Util.format(new Date(finishTime)));
    }
    
    private PendingIntent getPendingIntent(int id) {
        Intent intent = new Intent(context, OneShotAlarm.class);
        //Intent intent = new Intent("au.com.brian.timer." + t.getId());
        intent.putExtra("message", id);
    	Log.i(TAG, "intent message = " +  intent.getIntExtra("message",-1));
        PendingIntent sender = PendingIntent.getBroadcast(context,
                0, intent, 0);
        return sender;
    }
}

