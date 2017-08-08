package au.com.brian.timer.domain;

import java.util.List;

public interface TaskContainer {

	public void addTask(Task t);
	public void addError(String s);
	public List<Task> getTasks();
	public Integer getId();
	public String getTitle();
}
