package au.com.brian.timer.util;

import java.io.InputStream;
import java.util.Stack;

import org.xml.sax.Attributes;

import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;
import au.com.brian.timer.domain.Recipe;
import au.com.brian.timer.domain.Task;
import au.com.brian.timer.domain.TaskContainer;

public class RecipeParser {

	//private static int id;
	private static final String TAG = "RecipeParser";
	private static Recipe r;
	private static Stack<TaskContainer> taskStack;
	
	public static synchronized Recipe parse(InputStream is) {
		
		r = new Recipe();
		taskStack = new Stack<TaskContainer>();
		RootElement root = new RootElement("recipe");
		Element name = root.getChild("name");
		name.setEndTextElementListener(new EndTextElementListener() {
			public void end(String body) {
				Log.i(TAG,"got " + body);
				r.setName(body);
			}
		});
		Element finish = root.getChild("finish");
		finish.setEndTextElementListener(new EndTextElementListener() {
			public void end(String body) {
				r.setFinish(body);
			}
		});
		Element intro = root.getChild("intro");
		intro.setEndTextElementListener(new EndTextElementListener() {
			public void end(String body) {
				r.setIntro(body);
			}
		});
		Element taskElement = setUpTask(root);
		setUpTask(taskElement);
		taskStack.push(r);
		try {
			Xml.parse(is, Xml.Encoding.UTF_8, root.getContentHandler());
		}
		catch (Exception e) {
			Log.e(TAG, "Exception: " + e);
			e.printStackTrace();
		}
		return r;
		
	}
	
	private static Element setUpTask(final Element root) {
		
		final Task t = new Task();
		Element task = root.getChild("task");
		task.setStartElementListener(new StartElementListener(){
			public void start(Attributes a) {
				// clear data from the previous task
				Log.d(TAG, "clearing task " + t);
				t.clear();
				// I am now the "currentTask" - any tasks between now and my end are added as my children
				taskStack.push(t);
			}
		});
		task.setEndElementListener(new EndElementListener(){
			public void end() {
				// the task should now be built - add it to its parent
				if (t.isOk()) {
					//t.setId(id++);
					// add me to my parent (I am the currentTask) 
					taskStack.pop(); // get me off the stack
					// add to the current top of the stack, which is my parent 
					// (keeping it there, since there might be more tasks to added to this parent)
					Log.i(TAG, "Adding task " + t + " to " + taskStack.peek());
					taskStack.peek().addTask((Task)t.clone()); // need to clone??
				}
				else {
					Log.e(TAG,"Invalid Task - " + t);
					r.addError("Invalid task - " + t + " could not be added to the recipe");
				}
			}
		});
		Element title = task.getChild("title");
		title.setEndTextElementListener(new EndTextElementListener() {
			public void end(String body) {
				t.setTitle(body);
			}
		});
		Element minutes = task.getChild("minutes");
		minutes.setEndTextElementListener(new EndTextElementListener() {
			public void end(String body) {
				t.setMinutes(body);
			}
		});
		
		Element description = task.getChild("description");
		description.setEndTextElementListener(new EndTextElementListener() {
			public void end(String body) {
				t.setDescription(body);
			}
		});
		return task;
		
	}
	
}
