package au.com.brian.timer.domain;

import java.text.SimpleDateFormat;
import java.util.Calendar;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import au.com.brian.timer.util.Util;

public class SavedRecipe {

	private static String PREF_NAME = "au.com.brian.timer.savedRecipe";
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MMM/yyyy HH:mm:ss");
	private static String TAG = "SavedRecipe";
	
	SavedRecipe() {
	}
	
	static void saveRecipe(Context context,Recipe r) {
		clearRecipe(context);
        Log.i(TAG, "Saving recipe " + r);
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_WORLD_WRITEABLE);
		Editor editor = prefs.edit();
		editor.putString("recipeFileName", r.getFileName());
		editor.putString("finishTime", dateFormat.format(r.getFinishTime().getTime()));
		for (Task t: r.getTasks()) {
			editor.putBoolean(String.valueOf(t.getId()), t.isComplete());
		}
		editor.commit();
	}
	
	public static void clearRecipe(Context context) {
        Log.i(TAG, "clearing recipe");
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_WORLD_WRITEABLE);
		Editor editor = prefs.edit();
		editor.clear();
		editor.commit();
	}
	
	public static Recipe getRecipe(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_WORLD_WRITEABLE);
		Recipe r = null;
		try {
			r = Util.getRecipe(prefs, context);
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		if (r == null) {
			// not an error - most of the time, we aren't in the middle of a recipe
			return null;
		}
		String finishTime = prefs.getString("finishTime", null);
        Log.i(TAG, "got finish time " + finishTime);
        if (finishTime == null) {
        	return null;
        }
		Calendar c = Calendar.getInstance();
		try {
			c.setTime(dateFormat.parse(finishTime));
		} catch (Exception e) {
			e.printStackTrace();
		}
		r.setFinishTime(c);
		r.addFinishTask();
		for (Task t: r.getTasks()) {
			t.setComplete(prefs.getBoolean(String.valueOf(t.getId() + "_complete"), false));
		}
		return r;
	}

	public static void setComplete(Context context, int id, boolean b) {
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_WORLD_WRITEABLE);
		Editor editor = prefs.edit();
		editor.putBoolean(String.valueOf(id), b);
		editor.commit();
	}
}
