package au.com.brian.timer.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import roboguice.util.Ln;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import au.com.brian.timer.domain.Recipe;
import au.com.brian.timer.domain.Task;
import au.com.brian.timer.domain.TaskContainer;

//   /Applications/android-sdk-mac_86/tools/sqlite3

public class RecipeDBHelper {

	private static final String TAG = "RecipeDBHelper";
	public static final String DB_NAME = "cooking5";
	public static final String RECIPE_TABLE = "recipe1";
	public static final String JOIN_TABLE = "join1";
	public static final String TASK_TABLE = "task1";
	public static final int DB_VERSION = 1;

	private static final String CLASSNAME = RecipeDBHelper.class.getSimpleName();
	private static final String[] RECIPE_COLS = new String[] { "_id", "filename", "name", "intro", "taskId" };
	private static final String[] JOIN_COLS = new String[] { "_id", "taskId", "parentTaskId" };
	private static final String[] TASK_COLS = new String[] { "_id", "title", "minutes", "description" };
	
	protected Integer id;
	protected String title;
	protected Integer minutes;
	protected String description;
	protected Calendar alarmTime;
	protected boolean isComplete = false;

	private SQLiteDatabase db = null;
	private DBOpenHelper dbOpenHelper = null;
	
	public RecipeDBHelper(Context context) { 
		this.dbOpenHelper = new DBOpenHelper(context, DB_NAME, DB_VERSION); 
		this.db = this.dbOpenHelper.getWritableDatabase();
	}	

	public void close() { 
		if (this.db != null) {
			this.db.close(); 
			this.db = null;	
		}
	}
	
	public static Recipe get(Context context, long recipeId) {
        RecipeDBHelper db = null;
        Recipe r = null;
		try {
			db = new RecipeDBHelper(context);
			r = db.get(recipeId);
		}
		catch (Exception e) {
			Log.e(TAG,"Exception getting recipe " + recipeId + " from database : " + e);
			e.printStackTrace();
		}
		finally { if (db != null) db.close(); }
		return r;
	}
	
	public boolean exists(String recipeName) {
		return (getRecipeIdForName(recipeName) >= 0);
	}
		
		public void insert(Recipe r) { 
			Log.i(TAG,"inserting recipe " + r);
			Task t = new Task("Placeholder task for recipe " + r.getName(),0,"");
			insertTask(t);
			r.setTaskId(t.getId());
			ContentValues recipeValues = new ContentValues(); 
			recipeValues.put("filename", r.getFileName()); 
			recipeValues.put("name", r.getName()); 
			recipeValues.put("intro", r.getIntro()); 
			recipeValues.put("taskId", t.getId()); // recipe has a "dummy" task, to be the parent of the tasks in it 
			int recipeId = (int)this.db.insert(RecipeDBHelper.RECIPE_TABLE, null, recipeValues); // why is _id an int, and this returns a long?
			r.setId(recipeId);
			insertChildren(r);
		}
		
		private void insertChildren(TaskContainer c) {
			for (Task t: c.getTasks()) {
				if (t.getId() == null || t.getId() < 0) {
					// add the task to the database
					insertTask(t);
				}
				// link the task to it's container
				ContentValues joinValues = new ContentValues();
				joinValues.put("taskId", t.getId());
				if (c instanceof Recipe) {
					// obviously can't use the recipe id - it's a different table. so recipes have an entry in task as well
					joinValues.put("parentTaskId", ((Recipe)c).getTaskId()); 
				}
				else {
					joinValues.put("parentTaskId", c.getId());
				}
				this.db.insert(RecipeDBHelper.JOIN_TABLE, null, joinValues);
				Ln.i("linked task %s (%s) to parent %s (%s,%s)", t.getTitle(),t.getId(),c.getTitle(),c.getId(), c instanceof Recipe ? ((Recipe)c).getTaskId() : "-");
				if (t.hasTasks()) {
					insertChildren(t);
				}
			}
		}
				
		/** insert into the database. also sets the id into the task */
		private void insertTask(Task t) {
			Log.i(TAG,"inserting task " + t);
			ContentValues taskValues = new ContentValues();
			taskValues.put("title", t.getTitle());
			taskValues.put("minutes", t.getMinutes());
			taskValues.put("description", t.getDescription());
			int taskId = (int)this.db.insert(RecipeDBHelper.TASK_TABLE, null, taskValues);				
			t.setId(taskId); 
		}
		/** remove existing links, and add the new ones, without touching the actual tasks */
		private void relinkChildren(TaskContainer r) {
			this.db.delete(RecipeDBHelper.JOIN_TABLE, "parentTaskId=" + r.getId(), null);
			insertChildren(r);
		}
		
		public void update(Recipe r) { 
			ContentValues values = new ContentValues(); 
			values.put("filename", r.getFileName()); 
			values.put("name", r.getName()); 
			values.put("intro", r.getIntro()); 
			values.put("taskId", r.getTaskId()); // will be a problem if the task changes ...
			this.db.update(RecipeDBHelper.RECIPE_TABLE, values, "_id=" + r.getId(), null);
			relinkChildren(r);
		}
		
		public void update(Task t) { 
			ContentValues taskValues = new ContentValues();
			taskValues.put("title", t.getTitle());
			taskValues.put("minutes", t.getMinutes());
			taskValues.put("description", t.getDescription());
			this.db.update(RecipeDBHelper.TASK_TABLE, taskValues, "_id=" + t.getId(), null);
			if (t.hasTasks()) {
				relinkChildren(t);
			}
		}
		
		public void deleteRecipe(long id) {
			this.db.delete(RecipeDBHelper.RECIPE_TABLE, "_id=" + id, null);
			//this.db.delete(RecipeDBHelper.JOIN_TABLE, "parentTaskId=" + id, null); // FIXME - need to delete the dummy task, and it's children
		} 
		
		public void deleteRecipe(String name) {
			long recipeId = getRecipeIdForName(name);
			if (recipeId == -1) {
				Log.e(TAG,"No recipe found with name=" + name);
			}
			else {
				deleteRecipe(recipeId);
			}
		}
		
		public void clearDatabase() {
			Log.e(TAG,"Clearing the whole database!");
			DBOpenHelper.dropTables(db);
			DBOpenHelper.createTables(db);
		}
		
		private long getRecipeIdForName(String name) {
			Cursor c = null; 
			try {
				c = this.db.query(true, RecipeDBHelper.RECIPE_TABLE, RecipeDBHelper.RECIPE_COLS, "name = '" + name + "'", null, null, null, null, null); 
				if (c.getCount() > 0) {
					c.moveToFirst(); 
					return c.getLong(0); 
				} 
			} catch (SQLException e) {		
				Log.i(TAG, "Exception getting Recipe for name = " + name + ":" + e); 
			} finally {		
				if (c != null && !c.isClosed()) { 
					c.close();
				}
			}
			return -1;
		}
		
		public Recipe get(long recipeId) {
			Cursor c = null; 
			Recipe r = null; 
			try {
				c = this.db.query(true, RecipeDBHelper.RECIPE_TABLE, RecipeDBHelper.RECIPE_COLS, "_id = '" + recipeId + "'", null, null, null, null, null); 
				if (c.getCount() > 0) {
					c.moveToFirst(); 
					r = populateRecipeFromCursor(c);
					addTasks(r);
				} 
				else {
					r = new Recipe("No recipe found for id=" + recipeId);
				}
			} catch (SQLException e) {		
				Log.i(TAG, "Exception getting Recipe for recipeId = " + recipeId + ":" + e); 
			} finally {		
				if (c != null && !c.isClosed()) { 
					c.close();
				}
			}
			Log.d(TAG,"get(" + recipeId + ") returning recipe " + r.toDebug());
			return r;
		}
		
		public Recipe get(String name) { 
			Cursor c = null; 
			Recipe r = null; 
			try {
				c = this.db.query(true, RecipeDBHelper.RECIPE_TABLE, RecipeDBHelper.RECIPE_COLS, "name = '" + name + "'", null, null, null, null, null); 
				if (c.getCount() > 0) {
					c.moveToFirst(); 
					r = populateRecipeFromCursor(c);
					addTasks(r);
				} 
			} catch (SQLException e) {		
				Log.i(TAG, "Exception getting Recipe for name = " + name + ":" + e); 
			} finally {		
				if (c != null && !c.isClosed()) { 
					c.close();
				}
			}
			return r;
		}
		
		private Recipe populateRecipeFromCursor(Cursor c) {
			Recipe r = new Recipe();
			r.setId(c.getInt(0)); 
			r.setFileName(c.getString(1));
			r.setName(c.getString(2));
			r.setIntro(c.getString(3));
			r.setTaskId(c.getInt(4));
			Log.d(TAG,"populateRecipeFromCursor got recipe " + r.toDebug());
			return r;
		}
		
		private Task populateTaskFromCursor(Cursor c) {
			Task t = new Task();
			t.setId(c.getInt(0)); 
			t.setTitle(c.getString(1));
			t.setMinutes(c.getInt(2));
			t.setDescription(c.getString(3));
			return t;
		}
		
		/** gets recipes - doesn't get the tasks */
		public List<Recipe> getAll() {
			ArrayList<Recipe> ret = new ArrayList<Recipe>(); 
			Cursor c = null; 
			try {
				c = this.db.query(RecipeDBHelper.RECIPE_TABLE, RecipeDBHelper.RECIPE_COLS, null, null, null, null, null);
				int numRows = c.getCount(); 
				c.moveToFirst(); 
				for (int i=0;i<numRows;++i) {
					Recipe r = populateRecipeFromCursor(c);
					ret.add(r); 
					c.moveToNext();
				} 
			} catch (SQLException e) {
				Log.e(TAG, "SQLException in getAll() :" + e); 
			} finally {
				if (c != null && !c.isClosed()) { 
					c.close();
				}
			} 
			return ret;
		}		
		
		/* Add the tasks to the passed recipe */
		private void addTasks(TaskContainer r) {
			Cursor c = null; 
			String[] col = { "taskId" };
			try {
				c = this.db.query(RecipeDBHelper.JOIN_TABLE, col, "parentTaskId = " + r.getId(), null, null, null, null);
				int numRows = c.getCount(); 
				Ln.d("Got %s sub-tasks from the database for %s", numRows, r);
				if (numRows == 0) {
					return; // a simple task with no children
				}
				c.moveToFirst(); 
				for (int i=0;i<numRows;++i) {
					long taskId = c.getLong(0);
					Task t = getTask(taskId); 
					Ln.d("   got sub-task = %s", t);
					c.moveToNext();		
				}
				c.moveToFirst(); 
				for (int i=0;i<numRows;++i) {
					// get each child task for the passed container
					long taskId = c.getLong(0);
					Task t = getTask(taskId); 
					if (t == null) {
						Log.w(TAG,"Got no task for taskId " + taskId);
						c.moveToNext();					
						continue;
					}
					Ln.d("   got sub-task = %s", t);
					// now get any subtasks
					addTasks(t);
					r.addTask(t);
					c.moveToNext();		
				} 
			} catch (SQLException e) {
				Log.e(TAG, "SQLException in getAll() :" + e); 
			} finally {
				if (c != null && !c.isClosed()) { 
					c.close();
				}
			} 
		}
		
		private Task getTask(long taskId) {
			Cursor c = null;
			Task t = null;
			try {		 
				c = this.db.query(RecipeDBHelper.TASK_TABLE, RecipeDBHelper.TASK_COLS, "_id = " + taskId, null, null, null, null);
				c.moveToFirst();
				t = populateTaskFromCursor(c);
			} catch (SQLException e) {
				Log.e(TAG, "SQLException in getTask(" + taskId + ") :" + e); 
			} finally {
				if (c != null && !c.isClosed()) { 
					c.close();
				}
			} 
			return t;
		}
		
//////////
		
	private static class DBOpenHelper extends SQLiteOpenHelper {

		//private static final String[] RECIPE_COLS = new String[] { "_id", "filename", "name", "intro", "taskId" };
		//private static final String[] RECIPE_TASK_COLS = new String[] { "_id", "taskId", "parentTaskId" };
		//private static final String[] TASK_COLS = new String[] { "_id", "title", "minutes", "description" };

		private static final String TAG = "DBOpenHelper";
		private static final String DB_CREATE_RECIPE = "CREATE TABLE "
				+ RecipeDBHelper.RECIPE_TABLE
				+ " (_id INTEGER PRIMARY KEY AUTOINCREMENT, filename TEXT, name TEXT UNIQUE NOT NULL, intro TEXT, taskId INTEGER);";
		private static final String DB_CREATE_JOIN = "CREATE TABLE "
			+ RecipeDBHelper.JOIN_TABLE
			+ " (_id INTEGER PRIMARY KEY AUTOINCREMENT, taskId LONG NOT NULL, parentTaskId LONG NOT NULL);";
		private static final String DB_CREATE_TASK = "CREATE TABLE "
			+ RecipeDBHelper.TASK_TABLE
			+ " (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, minutes INT, description TEXT);";

		public DBOpenHelper(Context context, String dbName, int version) {
			super(context, dbName, null,
					version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			createTables(db);
		}

		@Override
		public void onOpen(SQLiteDatabase db) {
			super.onOpen(db);
		}
		
		public static void createTables(SQLiteDatabase db) {
			try {
				db.execSQL(DBOpenHelper.DB_CREATE_RECIPE);
				db.execSQL(DBOpenHelper.DB_CREATE_JOIN);
				db.execSQL(DBOpenHelper.DB_CREATE_TASK);
			} catch (SQLException e) {
				Log.e(TAG, "Exception in createTables(): " + e);
			}
		}

		public static void dropTables(SQLiteDatabase db) {
			try {
				db.execSQL("DROP TABLE IF EXISTS " + RECIPE_TABLE);
				db.execSQL("DROP TABLE IF EXISTS " + TASK_TABLE);
				db.execSQL("DROP TABLE IF EXISTS " + JOIN_TABLE);
			} catch (SQLException e) {
				Log.e(TAG, "Exception in createTables(): " + e);
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.e(TAG, "onUpgrade not implemented");
			// db.execSQL("DROP TABLE IF EXISTS " + DBHelper.RECIPE_TABLE);
			// this.onCreate(db);
		}
	}
}
