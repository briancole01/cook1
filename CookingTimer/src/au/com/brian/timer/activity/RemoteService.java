/*
 */

package au.com.brian.timer.activity;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import au.com.brian.timer.R;
import au.com.brian.timer.domain.IRemoteService;
import au.com.brian.timer.domain.IRemoteServiceCallback;
import au.com.brian.timer.domain.Recipe;
import au.com.brian.timer.domain.SavedRecipe;
import au.com.brian.timer.domain.Task;
import au.com.brian.timer.util.IncomingMessageView;

/**
 * This is an example of implementing an application service that runs in a
 * different process than the application.  Because it can be in another
 * process, we must use IPC to interact with it.  The
 * {@link RemoteServiceController} and {@link RemoteServiceBinding} classes
 * show how to interact with the service.
 */
public class RemoteService extends Service {

    static NotificationManager n;
    static AlarmManager a;
    static Recipe r;
    private static final String TAG = "Service";
	public static int MINUTE = Calendar.MINUTE; // change to seconds for debugging
	//public static final int RECIPE_FILE = R.raw.roast_lamb;
	//public static final int RECIPE_FILE = R.raw.test2;
	private final static int APPLICATION_NOTIF_ID = Integer.MAX_VALUE-1;
	private static final int ALARM_MSG_ID = 1;
	private static final int CALLBACK_MSG_ID = 2;
    final RemoteCallbackList<IRemoteServiceCallback> mCallbacks
    = new RemoteCallbackList<IRemoteServiceCallback>();
	
    
    @Override
    public void onCreate() {
        n = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        a = (AlarmManager)getSystemService(ALARM_SERVICE);
        Log.e(TAG,"FIXME - ignoring saved recipe while we sort out task ids");
        //r = SavedRecipe.getRecipe(this);
        r = null;
        if (r != null) {
        	if (r.getFinishTime().before(Calendar.getInstance())) {
        		Log.i(TAG, "discarding saved recipe with old finish time - " + r.getFinishTime());
        		r = null;
        		SavedRecipe.clearRecipe(this);
        	}
        }
        new Thread() {
        	@Override
        	public void run() {
         	    callbackHandler.sendEmptyMessage(CALLBACK_MSG_ID); // we want to reload for our UI to see the completed task
        	}
        } ;
    }
 /*   
    void loadRecipe(int recipeId) {
        r = RecipeParser.parse(getResources().openRawResource(recipeId));
        r.setId(recipeId);
		Log.i(TAG, "got recipe = " + r);
		if (r.getFinishTime() == null) {
			r.calcFinishTime();
			r.addFinishTask();
		}
		SavedRecipe.saveRecipe(this, r);
		processAlarm(null);
		sendApplicationNotification();
    }
*/

    @Override
	public void onStart (Intent intent, int startId) {
    	//int recipeId = intent.getIntExtra("recipeId",-1);
    	Log.i(TAG, "onStart()");
    	//if (recipeId == -1) {
    	//	return;
    	//}
    	//loadRecipe(recipeId);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        //return START_STICKY; // return available for new Android 2.0 method
    }

    @Override
    public void onDestroy() {
     	Log.i(TAG,"onDestroy - start");
     	clearApplicationNotification();
     	
        // Unregister all callbacks.
        mCallbacks.kill();
        
        // Remove the next pending message to increment the counter, stopping
        // the increment loop.
        alarmHandler.removeMessages(ALARM_MSG_ID);
     	Log.i(TAG,"onDestroy - end");
   }

	@Override
	public IBinder onBind(Intent arg0) {
		return this.binder;
	}
    
	private final IRemoteService.Stub binder = new IRemoteService.Stub() {
		
	    public void registerCallback(IRemoteServiceCallback cb) {
	    	Log.i(TAG,"registering callback");
	    	mCallbacks.register(cb);
	    }
	    /**
	     * Remove a previously registered callback interface.
	     */
	    public void unregisterCallback(IRemoteServiceCallback cb){
	    	Log.i(TAG,"unregistering callback");
	    	mCallbacks.unregister(cb);
	    }
	    
	    public Recipe getRecipe() { 
	    	Log.i(TAG,"returning recipe " +  r);
	    	return r; 
	    }
	    
	    public void setRecipe(Recipe r) {
	    	Log.i(TAG,"setting new recipe " + r);
	    	RemoteService.r = r;
	        if (r != null) {
	    		processAlarm(null);
	    		sendApplicationNotification();
	        }
	    }
	    
	    //public void setRecipeId(int recipeId) {
	    //	Log.i(TAG,"setting new recipe id " + recipeId);
	    //	loadRecipe(recipeId);
	    //}
	};
	
	private void sendApplicationNotification() {
        Notification notif = new Notification(R.drawable.icon, "Cooking Timer",
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, IncomingMessageView.class), 0);
        // Set the info for the views that show in the notification panel.
        notif.setLatestEventInfo(this, "Cooking Timer", "You are currently cooking " + r.getName(), contentIntent);

        // after a 100ms delay, vibrate for 250ms, pause for 100 ms and
        // then vibrate for 500ms.
        //notif.vibrate = new long[] { 100, 250, 200, 500, 200, 500, 200, 500};
        //notif.defaults |= Notification.DEFAULT_SOUND;
        //notif.defaults |= Notification.DEFAULT_LIGHTS;
        //notif.defaults |= Notification.FLAG_INSISTENT; // does this work? do I need to play music instead of the default sound?
        notif.defaults |= Notification.FLAG_ONGOING_EVENT;
        notif.defaults |= Notification.FLAG_NO_CLEAR;
        
         n.notify(APPLICATION_NOTIF_ID, notif);
	}

	private void clearApplicationNotification() {
        n.cancel(APPLICATION_NOTIF_ID);
	}
	
    private Handler alarmHandler = new Handler() {
        @Override public void handleMessage(Message m) {
        	Integer id = m.getData().getInt("taskId");
        	Log.i(TAG, "id = " +  id);
       	    processAlarm(id);
        }
        
    };
    
    // we just got the alarm for the passed taskId
    void processAlarm(Integer id) {
        Log.i(TAG, "processing alarm for " + id);
    	if (id != null) {
	    	Task t = r.getTaskForId(id);
	    	if (t == null) {
	    		Log.e(TAG,"Got null task for id = " + id);
	    		Log.e(TAG,"Recipe is " + r.toDebug());
		        stopSelf(); // can't ever get past this - can't set the null task to complete
		        return;
	    	}
	    	makeNotification(t);
	   		t.setComplete(true);
	        callbackHandler.sendEmptyMessage(CALLBACK_MSG_ID); // we want to reload for our UI to see the completed task
	   		SavedRecipe.setComplete(this, id, true); // TODO - only fix up saved recipe if we are in danger of being killed
    	}
		Task next = r.getNextAlarm();
		if (next == null) {
	        Log.i(TAG, "no more tasks!");
	        stopSelf();
	        return;
		}
    	final Message m = alarmHandler.obtainMessage(ALARM_MSG_ID);
    	Bundle b = new Bundle();
    	b.putInt("taskId", next.getId());
    	m.setData(b);
        Log.i(TAG, "sending arg for next alarm. id = " +  m.getData().getInt("taskId"));
    	alarmHandler.sendMessageDelayed(m, next.getAlarmTime().getTimeInMillis()-System.currentTimeMillis());
    }
    
    private void makeNotification(final Task t) {
        // construct the Notification object.
    	Log.i(TAG,"Making notification for " + t);
        Notification notif = new Notification(R.drawable.icon, t.getTitle(),
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, IncomingMessageView.class), 0);
        // Set the info for the views that show in the notification panel.
        notif.setLatestEventInfo(this, "Cooking Timer - " + t.getTitle(), "more detail goes here", contentIntent);

        // after a 100ms delay, vibrate for 250ms, pause for 100 ms and
        // then vibrate for 500ms.
        notif.vibrate = new long[] { 100, 250, 200, 500, 200, 500, 200, 500};
        notif.defaults |= Notification.DEFAULT_SOUND;
        notif.defaults |= Notification.DEFAULT_LIGHTS;
        notif.defaults |= Notification.FLAG_INSISTENT; // does this work? do I need to play music instead of the default sound?
        
        n.notify(t.getNotificationId(), notif);
    }
    
    /**
     * Our Handler used to execute operations on the main thread.  This is used
     * to schedule increments of our value.
     */
    private final Handler callbackHandler = new Handler() {
        @Override public void handleMessage(Message msg) {

                    // Broadcast to all clients the new value.
                    final int N = mCallbacks.beginBroadcast();
                    Log.i(TAG, "sending reload recipe for " +  N);
                    for (int i=0; i<N; i++) {
                        try {
                            mCallbacks.getBroadcastItem(i).reloadRecipe();
                        } catch (RemoteException e) {
                            // The RemoteCallbackList will take care of removing
                            // the dead object for us.
                        }
                    }
                    mCallbacks.finishBroadcast();
                    
        }
    };
    
    /**
     * Show a notification while this service is running.
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.remote_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.stat_sample, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, LocalServiceController.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.remote_service_label),
                       text, contentIntent);

        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        mNM.notify(R.string.remote_service_started, notification);
    }
     */

}
