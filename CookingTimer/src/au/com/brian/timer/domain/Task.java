package au.com.brian.timer.domain;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import au.com.brian.timer.activity.RemoteService;
import au.com.brian.timer.util.Util;

public class Task implements Cloneable, Comparable<Task>, Parcelable, TaskContainer {
	
	protected Integer id; // unique id from database
	protected int notificationId; // unique id (in this running recipe) for notifications
	protected String title;
	protected String titlePrefix;
	protected Integer minutes;
	protected String description;
	protected Calendar alarmTime;
	protected boolean isComplete = false;
	public final static Integer FINISH_TASK_ID = Integer.MAX_VALUE;
    private static final String TAG = "Task";
	private ArrayList<String> errors = new ArrayList<String>(2);
	private ArrayList<Task> tasks = new ArrayList<Task>();

	public Task() { }
	
	public Task(String title, Integer minutes, String description) {
		this.title = title;
		this.minutes = minutes;
		this.description = description;
	}

	public Task(Integer id, String title, Integer minutes, String description, int notificationId, ArrayList<Task> tasks) {
		this.id = id;
		this.title = title;
		this.minutes = minutes;
		this.description = description;
		this.notificationId = notificationId;
		this.tasks = tasks;
	}

	public Task(Integer id, String title, Integer minutes, String description, Calendar alarmTime, int notificationId, ArrayList<Task> tasks) {
		this(id,title,minutes,description, notificationId,tasks);
		this.alarmTime = alarmTime;
	}

	public Task(Parcel source) {
		readFromParcel(source);
	}

	public void clear() {
		id = null;
		title = "";
		minutes = null;
		description = "";
		tasks = new ArrayList<Task>();
	}
	
	public boolean isOk() {
		if (minutes == null && !hasTasks()) return false; // either have a time, or children 
		return true;
	}
	
	public final void setAlarmTime(Calendar finishTime) {
		alarmTime = (Calendar)finishTime.clone();
		alarmTime.add(RemoteService.MINUTE, minutes * -1);
	}
	
	public final void setAlarmTime(Calendar finishTime,int extraMinutes) {
		setAlarmTime(finishTime);
		// adjust the alarm time backwards by the passed number of minutes
		alarmTime.add(RemoteService.MINUTE, extraMinutes * -1);
	}
	
	public final Calendar getAlarmTime() {
		return alarmTime;
	}
	
	public final String getTitle() {
		return title;
	}
	public final void setTitle(String title) {
		this.title = title;
	}
	public final int getMinutes() {
		if (minutes == null) return 0;
		else return minutes;
	}
	public final void setMinutes(String minutes) {
		this.minutes = Integer.parseInt(minutes);
	}
	public final void setMinutes(int minutes) {
		this.minutes = minutes;
	}
	public final String getDescription() {
		return description;
	}
	public final void setDescription(String description) {
		this.description = description;
	}
	public final Integer getId() {
		return id;
	}
	public final void setId(int taskId) {
		this.id = taskId;
	}
	public final int getNotificationId() {
		return notificationId;
	}
	public final void setNotificationId(int notificationId) {
		this.notificationId = notificationId;
	}
	@Override
	public String toString() {
		return "Task[id=" + id  + ",title=" + title +",minutes=" + minutes +",desc=" + description + ",alarmTime=" + 
		        Util.format(alarmTime) + ",subtasks=" + getNumTasks() + "]";

	}
	public final boolean isComplete() {
		return isComplete;
	}

	public final void setComplete(boolean isComplete) {
		this.isComplete = isComplete;
	}

	public void addTask(Task st) {
		tasks.add(st);
	}
	
	public ArrayList<Task> getTasks() {
		return tasks;
	}
	
	/** nb - returns null if tasks aren't set yet */
	public Integer getNumTasks() {
		return (tasks == null ? null : tasks.size());
	}
	
	public boolean hasTasks() { 
		Integer i = getNumTasks();
		return i == null ? false : i > 0;  // null means "not set yet", so really neither true nor false ... but gets called by toDebug().
	}

	@Override
	public Object clone() {
		return new Task(id,title,minutes,description,alarmTime,notificationId,tasks);

	}

	//@Override
	public int compareTo(Task t) {
		// TODO Auto-generated method stub
		if (t == null) return -1;
		if (t.getAlarmTime() == null) return -1;
		if (alarmTime == null) return 1;
		return -(t.getAlarmTime().compareTo(alarmTime));
	}

	//@Override
	public int describeContents() {
		return hashCode();
	}

	//@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(String.valueOf(id));
		dest.writeString(title);
		dest.writeString(titlePrefix);
		dest.writeString(String.valueOf(minutes));
		dest.writeString(description);
		dest.writeString(String.valueOf(notificationId));
		if (alarmTime == null) {
			dest.writeLong(0);
		}
		else {
			dest.writeLong(alarmTime.getTimeInMillis());
		}
		dest.writeString(String.valueOf(isComplete));
		dest.writeParcelableArray(tasks.toArray(new Task[tasks.size()]), 0);
	}
	protected void readFromParcel(Parcel source) {
		//Log.d(TAG,"readFromParcel");
		try { this.id = Integer.parseInt(source.readString()); }
		catch (Exception e) { this.id = null; }
		this.title = source.readString();
		this.titlePrefix = source.readString();
		try { this.minutes = Integer.parseInt(source.readString()); }
		catch (Exception e) { this.minutes = null; }
		this.description = source.readString();
		try { this.notificationId = Integer.parseInt(source.readString()); }
		catch (Exception e) { this.notificationId = 0; }
		this.alarmTime = Calendar.getInstance();
		alarmTime.setTimeInMillis(source.readLong());
		isComplete = Boolean.valueOf(source.readString());
		Object[] taskArray = source.readParcelableArray(this.getClass().getClassLoader());
		for (Object o: taskArray) {
			if (o instanceof Task) {
				tasks.add((Task)o);
			}
		}
	}
	
	public static final Parcelable.Creator<Task> CREATOR = new Parcelable.Creator<Task>() {
	    public Task createFromParcel(Parcel source) {
	          return new Task(source);
	    }
	    public Task[] newArray(int size) {
	          return new Task[size];
	    }
	};

	public final String getTitlePrefix() {
		return titlePrefix;
	}

	public final void setTitlePrefix(String titlePrefix) {
		this.titlePrefix = titlePrefix;
	}

	//@Override
	public final void addError(String s) {
		errors.add(s);
	}
	public final List<String> getErrors() {
		return errors;
	}
}
