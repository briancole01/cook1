package au.com.brian.timer.domain;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import au.com.brian.timer.activity.RemoteService;

public class Recipe implements TaskContainer,  Parcelable {
	
	private Integer id;
	private Integer taskId; // the associated task, which gives us an id we can use in the task join table to get our children tasks
	private String filename;
	private String name;
	private String intro;
	private boolean ok = true;
	// our tasks. may be simple tasks, or ParentTasks which contain Tasks
	private ArrayList<Task> tasks = new ArrayList<Task>(10);
	private ArrayList<String> errors = new ArrayList<String>(2);
	private Calendar finishTime = null;
	private String finish = null;
    private static final String TAG = "Recipe";
    private List<Task> taskListForDisplay = new ArrayList<Task>(10);
	
	public Recipe(Parcel data) {
		readFromParcel(data);
	}
	
	public Recipe() {
	}

	public Recipe(String intro) {
		this.intro = intro;
	}

	public Recipe(Integer id, String filename, String name, String intro) {
		this.id = id;
		this.filename = filename;
		this.name = name;
		this.intro = intro;
	}

	public Task getNextAlarm() {
		Task earliestTask = null;
		for (Task t: tasks) {
			if (t.hasTasks()) {
				for (Task t2: t.getTasks()) {
					earliestTask = getEarlierTask(t2,earliestTask);
				}
			}
			else {
				earliestTask = getEarlierTask(t,earliestTask);
			}
		}
		return earliestTask;
	}
	private Task getEarlierTask(Task t, Task earliestTask) {
		if (t.isComplete()) return earliestTask;
		if (t.getAlarmTime() == null) return earliestTask; // not a real task; prob. a parent
		if (earliestTask == null) {
			return t;
		}
		if (earliestTask.getAlarmTime().after(t.getAlarmTime())) {
			return t;
		}
		else {
			return earliestTask;
		}
	}
	public final Integer getId() {
		return id;
	}
	public final void setId(int id) {
		this.id = id;
	}
	public final String getFileName() {
		return filename;
	}
	public final void setFileName(String filename) {
		this.filename = filename;
	}
	public final void addTask(Task t) {
		tasks.add(t);
	}
	public final List<Task> getTasks() {
		return tasks;
	}
	
	public final void addError(String s) {
		errors.add(s);
	}
	public final List<String> getErrors() {
		return errors;
	}

	public final String getName() {
		return name;
	}
	public final void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name + " - " + tasks.size() + " tasks, file = '" + id + "', ok=" + ok;
	}
	
	public String toDebug() {
		String s = toString() + "\n";
		for (Task t: getTasks()) {
			s += "   "  + t + "\n";
			if (t.hasTasks()) {
				for (Task t2: t.getTasks()) {
					s += "     "  + t2 + "\n";
				}
			}
		}
		return s;
	}
	
	public final Calendar getFinishTime() {
		return finishTime;
	}
	
	public final void setFinishTime(Calendar finishTime) {
		Log.i("Recipe","Setting finish time to " + new Date(finishTime.getTimeInMillis()));
		this.finishTime = finishTime;
		for (Task t: tasks) {
			if (t.hasTasks()) {
				// set alarms for the subtasks
				// we don't set a time on the parent tasks - they don't get an alarm
				int timeSoFar = 0;
				//for (int i = t.getNumSubTasks()-1; i < 0; i--) {
				int i = t.getNumTasks()-1;
				while (i >= 0) {
					// for each subtask, push the alarm time back, to fit in subsequent subtasks
					Task subTask = t.getTasks().get(i);
					subTask.setAlarmTime(finishTime,timeSoFar);
					timeSoFar += subTask.getMinutes();
					i--;
				}
			}
			else {
				// we're a simple task
				t.setAlarmTime(finishTime);
			}
		}
	}
	public final String getFinish() {
		return finish;
	}
	public final void setFinish(String finish) {
		this.finish = finish;
	}
	
	public int getTotalTime() {
    	// tasks are done in parallel - find the longest, and so work out our finish time
        int maxTime = 0;
        for (Task t: getTasks()) {
        	if (t.hasTasks()) {
         		int totalTime = 0;
        		for (Task st: t.getTasks()) {
        			totalTime += st.getMinutes();
        		}
        		if (totalTime > maxTime) {
        			maxTime = totalTime;
        		}
        	}
        	else {
        		if (t.getMinutes() > maxTime) {
        			maxTime = t.getMinutes();
        		}
        	}
        }
        return maxTime;
	}
	
	public void calcFinishTime() {
 	    if (getFinish() != null) {
	    	Calendar c = Calendar.getInstance();
	        c.setTimeInMillis(System.currentTimeMillis());
	    	try {
				Date d = new SimpleDateFormat("hh:mm").parse(getFinish());
				c.set(Calendar.HOUR_OF_DAY, d.getHours());
				c.set(Calendar.MINUTE, d.getMinutes());
				c.set(Calendar.SECOND, 0);
			} catch (ParseException e) {
				e.printStackTrace();
			}
	    	setFinishTime(c);
	    }
	    else {
	        Calendar finishTime = Calendar.getInstance();
	        finishTime.setTimeInMillis(System.currentTimeMillis());
	        finishTime.add(RemoteService.MINUTE, getTotalTime());
	        setFinishTime(finishTime);
	    }
	}
	
	public synchronized void addFinishTask() {
        // add the special finish task, so we know when we can eat ...
		for (Task t: getTasks()) {
			if (t.getId() == Task.FINISH_TASK_ID) {
				Log.i(TAG,"removing old finishTask " + t);
				tasks.remove(t);
				break;
			}
		}
        Task finishTask = new Task(0,"Finished cooking - serve and enjoy!",0,"",getFinishTime(),Task.FINISH_TASK_ID, new ArrayList<Task>(0));
		Log.i(TAG,"adding finishTask " + finishTask);
        addTask(finishTask);
	}
	
	public Task getTaskForId(int id) {
		for (Task t: tasks) {
			if (t.getId() == null) continue;
			if (t.getId() == id) return t;
			if (t.hasTasks()) {
				for (Task t2: t.getTasks()) {
					if (t2.getId() == null) continue;
					if (t2.getId() == id) return t2;
				}
			}
		}
		return null;
	}
	
	//@Override
	public int describeContents() {
		return hashCode();
	}
	//@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeString(filename);
		dest.writeString(name);
		//dest.writeStringList(errors);
		dest.writeLong(finishTime.getTimeInMillis());
		dest.writeString(finish);
		dest.writeString(String.valueOf(ok));
		// writeTypedList / readTypedList don't work (probably because some objects are ParentTask, and some are Task)
		dest.writeParcelableArray(tasks.toArray(new Task[tasks.size()]), 0);
	}
	
	private final void readFromParcel(Parcel source) {
		id = source.readInt();
		filename = source.readString();
		name = source.readString();
		Log.d(TAG, "got recipe from parcel. name = " + name + ",id=" + id);		
		errors = new ArrayList<String>(); // empty for now //source.readStringList(errors);		
		finishTime = Calendar.getInstance();
		finishTime.setTimeInMillis(source.readLong());
		finish = source.readString();
		ok = source.readString().equals("true");
		Object[] taskArray = source.readParcelableArray(Task.class.getClassLoader());
		Log.d(TAG, "got task array from parcel. size = " + taskArray.length);		
		if (taskArray != null) {
			for (Object o: taskArray) {
				Log.d(TAG, "task object = " + o);		
				if (o instanceof Task) {
					tasks.add((Task)o);
				}
			}
		}
}
	public static final Parcelable.Creator<Recipe> CREATOR = new Parcelable.Creator<Recipe>() {
	    public Recipe createFromParcel(Parcel source) {
	          return new Recipe(source);
	    }
	    public Recipe[] newArray(int size) {
	          return new Recipe[size];
	    }
	};

    public List<Task> getTimerTaskListForDisplay() {
    	// FIXME - stupid that we are doing this again everytime, but we want the new "complete" flag for tasks
    	// Keep the flag in a separate list?
    	taskListForDisplay = new ArrayList<Task>(10);
        for (Task t: getTasks()) {
        	if (t.hasTasks()) {
         		for (Task st: t.getTasks()) {
        			st.setTitlePrefix(t.getTitle());
        			taskListForDisplay.add(st);
        		}
        	}
        	else {
        		taskListForDisplay.add(t);
        	}
        }
        Collections.sort(taskListForDisplay);
        return taskListForDisplay;
    }

    public List<Task> getViewTaskListForDisplay() {
    	// FIXME - stupid that we are doing this again everytime?
    	taskListForDisplay = new ArrayList<Task>(10);
        for (Task t: getTasks()) {
        	if (t.getId() == Task.FINISH_TASK_ID) {
        		continue;
        	}
    		taskListForDisplay.add(t);
        }
        return taskListForDisplay;
    }

	public final String getIntro() {
		return intro;
	}

	public final void setIntro(String intro) {
		this.intro = intro;
	}

	public final boolean isOk() {
		return ok;
	}

	public final void setOk(boolean ok) {
		this.ok = ok;
	}

	public void setTaskId(Integer taskId) {
		this.taskId = taskId;
	}

	public Integer getTaskId() {
		return taskId;
	}

	//@Override
	public String getTitle() {
		return name;
	}

}
