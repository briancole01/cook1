/*
 * Copyright (C) 2012 by Me
 */
package au.com.brian.timer.activity;
/*
 *  /Applications/android-sdk-mac_86/tools/adb -d install /Users/briancole/Documents/workspace/CookingTimer/bin/CookingTimer.apk
 */
import java.util.List;

import roboguice.activity.RoboListActivity;
import roboguice.util.Ln;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.ListView;
import au.com.brian.timer.R;
import au.com.brian.timer.domain.IRemoteService;
import au.com.brian.timer.domain.IRemoteServiceCallback;
import au.com.brian.timer.domain.Recipe;
import au.com.brian.timer.domain.Task;
import au.com.brian.timer.util.ListAdapterTimer;
import au.com.brian.timer.util.Util;

import com.google.inject.Module;


/* TODO
// browse for recipe files - www.anddev.org/viewtopic.php?t=67 (adding icons to a list - t=97. 
//    adding icons to the file browser - www.anddev.org/android_file__v20-t101.html)
 
// task icons (based on type? eg. cutting / washing / oven / pan / pot).
// Error handling - numbers.
// UI needs some sort of restart/stop/pause. 
// alarms update the list in real-time by forcing reload of the data in the adapter
// clicking on notification shows step details, and link back to list.
// persistent notification brings up the list. 
// if no current recipe, go straight to picker
// try application object for recipe
 
*/
/** this class will just show the recipe it gets from the remote service */
public class CookingTimerActivity extends RoboListActivity implements OnLongClickListener {
    
	//private static final long FRAME_RATE = 100;
	private final static int TASK_DESCRIPTION_DIALOG_ID = 0;
    //private Recipe r = null; // get this from the service
	private ListView listView;
	private static final String TAG = "Activity";
	protected IRemoteService service;
	private int clickedPosition = -1;
	private boolean showingRecipe = false;
    
	protected void addApplicationModules(List<Module> modules) {
        modules.add(new MyModule());
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.loadRecipesIntoDatabase(this);

 /*
        // Watch for button clicks.
        Button button = (Button)findViewById(R.id.startButton);
        if (button != null) button.setOnClickListener(startListener);
 */       
        this.bindService(new Intent(this, RemoteService.class),
            	connection, BIND_AUTO_CREATE);   
        this.startService(new Intent(this, RemoteService.class));
        setContentView(R.layout.main);
        listView = getListView();
    }
    
    private void setUpTasksAndUI(Recipe r) {
        Ln.i(TAG, "setUpTasksAndUI for " + r);
        showingRecipe = true;
        setListAdapter(new ListAdapterTimer(this,r.getTimerTaskListForDisplay()));
        listView.setTextFilterEnabled(true);
        // FIXME
//		listView.setOnItemClickListener(new OnItemClickListener() {
//			@Override
//			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//				Log.i(TAG, "Got click on " + position);
//				clickedPosition = position;
//				showDialog(TASK_DESCRIPTION_DIALOG_ID);
//			}
//		});
    }
        
	@Override
	protected Dialog onCreateDialog(final int id) {
		switch (id) {
		case TASK_DESCRIPTION_DIALOG_ID: return taskDescriptionDialog();
		}
		return null;
	}

	@Override
	protected void onPrepareDialog(final int id, final Dialog d) {
		switch (id) {
		case TASK_DESCRIPTION_DIALOG_ID:
			// TODO - icon for the task ((AlertDialog)d).setIcon(killer.drawable.get(0));
			Object o = listView.getAdapter().getItem(clickedPosition);
			String message = "";
			if (o != null && o instanceof Task) {
			  message = ((Task)o).getDescription();
			}
			((AlertDialog)d).setMessage(message);
			break;
		}
	}
	
	private AlertDialog taskDescriptionDialog() {
		return new AlertDialog.Builder(this).setMessage("stuff about the task goes here")
		//.setIcon(R.drawable.ic_launcher_mm)
		.setTitle(R.string.app_name)
		.setPositiveButton(R.string.alert_dialog_continue, new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int whichButton) {
			}
		})
		.create();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		if (service == null) {
			Log.w(TAG,"No service at onStart() - doing bind and start");
	        this.bindService(new Intent(this, RemoteService.class),
	            	connection, BIND_AUTO_CREATE);   
	        this.startService(new Intent(this, RemoteService.class));
		}
		// recipe will load in our callback from the service
	}
/*
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent result) {
		if (resultCode == RESULT_OK) {
			if (service == null) {
				Log.i(TAG,"No service - starting with picked recipe");
		        this.bindService(new Intent(this, RemoteService.class),
		            	connection, BIND_AUTO_CREATE);   
		        this.startService(new Intent(this, RemoteService.class));
			}
        	recipeReloadHander.sendMessage(new Message());
		}
	}
*/	
	
	//@Override
	public boolean onLongClick(View arg0) {
		showDialog(TASK_DESCRIPTION_DIALOG_ID);
		return false;
	}

	// menu stuff - called the first time menu is pressed
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		super.onCreateOptionsMenu(menu);
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}
	// menu stuff
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		super.onOptionsItemSelected(item);
		final int id = item.getItemId();
		switch (id) {
		case R.id.menu_help:
			LayoutInflater inflater = LayoutInflater.from(this);
			View bView = inflater.inflate(R.layout.dialog_about, null);
			new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.about)
			.setView(bView)
			//.setPositiveButton(R.string.alert_dialog_continue, new DialogInterface.OnClickListener() {
			//	public void onClick(final DialogInterface dialog, final int whichButton) {
			//		//Log.i("MapMonsterActivity.menu_about", "currentState=" + currentState);
			//		usualUnPause();
			//	}
			//})
			.show();
			break;
		case R.id.menu_stop:
			stop();
			break;
		case R.id.menu_pause:
			pause();
			break;
		case R.id.menu_pick_recipe:
			goToRecipePicker();
			break;
		}
		return true;
	}

	private void stop() {
		Log.i(TAG,"Stop not implemented");
	}
	
	private void pause() {
		Log.i(TAG,"pause not implemented");
	}
	
	private void goToRecipePicker() {
        Intent intent = new Intent(CookingTimerActivity.this, RecipePickerActivity.class);
        startActivity(intent);		
	}
	

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder s) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            service = IRemoteService.Stub.asInterface(s);

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                service.registerCallback(reloadRecipeCallback);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
            Log.i(TAG,"connected to service");
            recipeReloadHander.sendMessage(new Message());
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            service = null;
            Log.i(TAG,"disconnected from service");
        }
    };

    /**
     * This implementation is used to receive callbacks from the remote
     * service.
     */
    private IRemoteServiceCallback reloadRecipeCallback = new IRemoteServiceCallback.Stub() {
        /**
         * This is called by the remote service regularly to tell us about
         * new values.  Note that IPC calls are dispatched through a thread
         * pool running in each process, so the code executing here will
         * NOT be running in our main thread like most other things -- so,
         * to update the UI, we need to use a Handler to hop over there.
         */
        public void reloadRecipe() {
            Log.i(TAG, "got reload recipe callback");
        	recipeReloadHander.sendMessage(new Message());
        }
    };
    
    private Handler recipeReloadHander = new Handler() {
        @Override public void handleMessage(Message msg) {
        	// reload the recipe and redisplay the UI
            Log.i(TAG, "reloading recipe now");
            Recipe r = null;
			try {
				r = service.getRecipe();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
    		if (r == null) {
    			Log.i(TAG,"Got null recipe from the service. showingRecipe=" + showingRecipe);
    			if (!showingRecipe) {
    				// no recipe on the screen or in the service - better pick one
    				goToRecipePicker();
    			}
    		}
    		else {
    			setUpTasksAndUI(r);
    		}
        }
        
    };
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
     	this.unbindService(connection);
   }

}
/*
	// animation loop
	private final Handler timer = new Handler();
	private final MyRunnable updateThread = new MyRunnable();
	private class MyRunnable implements Runnable {
		public void run() {
			timer.removeCallbacks(updateThread);
			try {
				listView.invalidate();
			} catch (final Exception e) {
				// don't let this stop us from trying again later
				//Log.e("MyRunnable.run", "exception invalidating mapView: " + e);
				e.printStackTrace();
			}
			timer.postDelayed(updateThread, FRAME_RATE);
		}
	}
    private OnClickListener mStopRepeatingListener = new OnClickListener() {
        public void onClick(View v) {
            // Create the same intent, and thus a matching IntentSender, for
            // the one that was scheduled.
            Intent intent = new Intent(CookingTimerActivity.this, RepeatingAlarm.class);
            PendingIntent sender = PendingIntent.getBroadcast(CookingTimerActivity.this,
                    0, intent, 0);
            
            // And cancel the alarm.
            AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
            am.cancel(sender);

            // Tell the user about what we did.
            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(CookingTimerActivity.this, R.string.repeating_unscheduled,
                    Toast.LENGTH_LONG);
            mToast.show();
        }
    };
*/


