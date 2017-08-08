package au.com.brian.timer.activity;

import java.util.Calendar;
import java.util.List;

import roboguice.activity.RoboListActivity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import au.com.brian.timer.R;
import au.com.brian.timer.domain.IRemoteService;
import au.com.brian.timer.domain.Recipe;
import au.com.brian.timer.domain.Task;
import au.com.brian.timer.util.ListAdapterView;
import au.com.brian.timer.util.Util;

public class RecipeIntroActivity extends RoboListActivity {

	private static final String TAG = "RecipeIntroActivity";
	//private String recipeFileName;
	private int hour;
	private int minute;
	static final int TIME_DIALOG_ID = 0;
	private final static int EDIT_TASK_DIALOG_ID = 1;
	TextView finishTime;
	protected IRemoteService service;
	Recipe r;
	Task clickedTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		r = Util.getRecipe(getIntent(), this);
		Log.i(TAG,"Got recipe " + r.toDebug());
			
		setContentView(R.layout.recipe_intro);
		// Watch for button clicks.
		Button button = (Button) findViewById(R.id.startButton);
		if (button != null)
			button.setOnClickListener(new OnClickListener() {
				//@Override
				public void onClick(View v) {
					Log.i(TAG, "Got click ");
					startRecipe();
				}
			});
		Button changeTimeButton = (Button) findViewById(R.id.changeTimeButton);
		if (changeTimeButton != null)
			changeTimeButton.setOnClickListener(new OnClickListener() {
				///@Override
				public void onClick(View v) {
					Log.i(TAG, "Got change time click ");
					showDialog(TIME_DIALOG_ID);
				}
			});
		TextView introText = (TextView) findViewById(R.id.introText);
		introText.setText(r.getIntro());
		if (!r.isOk()) {
			return;
		}
		TextView totalTime = (TextView) findViewById(R.id.totalTime);
		totalTime.setText("Total time: " + r.getTotalTime() );
		finishTime = (TextView) findViewById(R.id.finishTime);
		r.calcFinishTime();
		Calendar c = r.getFinishTime();
		hour = c.get(Calendar.HOUR_OF_DAY);
		minute = c.get(Calendar.MINUTE);
		updateFinishTime();
		final List<Task> taskList = r.getViewTaskListForDisplay();
		setListAdapter(new ListAdapterView(this,taskList));
        this.bindService(new Intent(this, RemoteService.class),
            	connection, BIND_AUTO_CREATE);   
        ListView listView = getListView();
		listView.setOnItemClickListener(new OnItemClickListener() {
			//@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				clickedTask = taskList.get(position);
				Log.i(TAG, "Got click on " + position + ", clickedTask = " + clickedTask);
				showDialog(EDIT_TASK_DIALOG_ID);
			}
		});
		
		listView.setOnLongClickListener(new OnLongClickListener() {
			//@Override
			public boolean onLongClick(View v) {
				Log.i(TAG, "Got long click on " + v);
				return true;
			}
		});
		
        registerForContextMenu(getListView());
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		Log.i(TAG, "Got context menu on " + v);
		// TODO - see developer.android notepad-ex2.html
	}
	
	void startRecipe() {
		try {
			r.setFinish(Util.twoZero(hour) + ":" + Util.twoZero(minute));
			r.calcFinishTime();
			r.addFinishTask();
			service.setRecipe(r);
		} catch (RemoteException e) {
			// TODO pop up an error dialog and ask user to try again
			e.printStackTrace();
		}
        Intent intent = new Intent(RecipeIntroActivity.this, CookingTimerActivity.class);
        //intent.putExtra("recipeFileName",recipeListInfo.get(position).file);
        startActivity(intent);
	}

	void updateFinishTime() {
		finishTime.setText(Util.twoZero(hour) + ":" + Util.twoZero(minute));
	}

	// the callback received when the user "sets" the time in the dialog
	private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int h, int m) {
			hour = h;
			minute = m;
			updateFinishTime();
		}
	};
	private Task currentlyEditingTask;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case TIME_DIALOG_ID:
			return new TimePickerDialog(this, mTimeSetListener, hour, minute,
					false);
		case EDIT_TASK_DIALOG_ID:
			return editTaskDialog();
		}
		
		return null;
	}
	
	@Override
	protected void onPrepareDialog(final int id, final Dialog d) {
		switch (id) {
		case EDIT_TASK_DIALOG_ID:
			// TODO - icon for the task ((AlertDialog)d).setIcon(killer.drawable.get(0));
			if (clickedTask != null) {
			  setTask((AlertDialog)d, clickedTask);
			}
			break;
		}
	}
	
	private void setTask(AlertDialog taskDialog, Task t) {
		Log.i(TAG,"Setting dialog task = " + t);
		currentlyEditingTask = t;
		EditText titleText = (EditText)taskDialog.findViewById(R.id.titleText);
		titleText.setText(t.getTitle());
		EditText descriptionText = (EditText)taskDialog.findViewById(R.id.descriptionText);
		descriptionText.setText(t.getDescription());
		EditText minutesText = (EditText)taskDialog.findViewById(R.id.minutesText);
		minutesText.setText(String.valueOf(t.getMinutes()));
		TextView titlePrefixText = (TextView)taskDialog.findViewById(R.id.titlePrefixText);
		titlePrefixText.setText(String.valueOf(t.getTitlePrefix()));
	}
	
	private void updateTask(final View editTaskDialogView) {
		EditText descriptionText = (EditText)editTaskDialogView.findViewById(R.id.descriptionText);
		currentlyEditingTask.setDescription(descriptionText.getText().toString());
	}
	
	private AlertDialog editTaskDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
		final View editTaskDialogView = inflater.inflate(R.layout.dialog_edit_task,(ViewGroup)findViewById(R.id.layout_root));
		builder.setView(editTaskDialogView);
		builder.setPositiveButton(R.string.alert_dialog_save, new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int whichButton) {
				// save my changes (only in the database, so won't survive restart while we are loading the files fresh every time)
				updateTask(editTaskDialogView);
			}
		});
		builder.setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.cancel();
			}
		});		
		builder.setTitle(R.string.app_name);
		AlertDialog dialog = builder.create();
		return dialog;
	}
	
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder s) {
            service = IRemoteService.Stub.asInterface(s);
            Log.i(TAG,"connected to service");
        }
        public void onServiceDisconnected(ComponentName className) {
             service = null;
            Log.i(TAG,"disconnected from service");
        }
    };
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
     	try { this.unbindService(connection); }
     	catch (Exception e) { }
   }
    

}
