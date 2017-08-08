package au.com.brian.timer.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import au.com.brian.timer.R;
import au.com.brian.timer.domain.Recipe;

public class Util {
	
	private static final String TAG = "Util";
	private static final boolean loadFromFiles = false;
	
	public static String format(Date date) {
		if (date == null) return "null";
		return toGoodHour(date.getHours()) + ":" +
	   	 twoZero(date.getMinutes()) + ":" +
		 twoZero(date.getSeconds());
	}
	
    public static String format(Calendar c) {
    	if (c == null) return "null";
    	else return toGoodHour(c.get(Calendar.HOUR)) + ":" +
    	 twoZero(c.get(Calendar.MINUTE)) + ":" +
    	 twoZero(c.get(Calendar.SECOND)) + " " +
    	 (c.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM") ;
    }
    
    public static String toGoodHour(int i) {
    	if (i == 0) return "12";
    	else return String.valueOf(i);
    }
    public static String twoZero(int i) {
    	if (i < 10) {
    		return  "0" + i;
    	}
    	return i + "";
    }
    
    public static Recipe getRecipe(Intent i, Context context) {
		long recipeId = i.getLongExtra("recipeId",-1);
		Recipe r = null;
		if (recipeId == -1) {
			Log.w(TAG, "No recipeId in intent");
			r = new Recipe("No recipe passed to this screen");
			r.setOk(false);
			return r;
		}
		try {
			r = RecipeDBHelper.get(context,recipeId);
		} catch (Exception e) {
			e.printStackTrace();
			r = new Recipe("Error loading recipeId " + recipeId + " : " + e);
			r.setOk(false);
		}
		return r;
    }
    
    public static Recipe getRecipe(SharedPreferences prefs, Context context) {
		String recipeFileName = prefs.getString("recipeFileName", null);
	    Log.i(TAG, "got recipeFileName " + recipeFileName);
		Recipe r;
		if (recipeFileName == null) {
			return null;
		}
		try {
			r = RecipeParser.parse(context.openFileInput(recipeFileName));
			r.setFileName(recipeFileName);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			r = new Recipe("Error loading recipe; " + e);
			r.setOk(false);
		}
		return r;
    }
    
    public static void loadRecipesIntoDatabase(final Context context) {
        //new Thread() {
        //	@Override
        //	public void run() {
        		try {
        			RecipeDBHelper db = new RecipeDBHelper(context);
        			db.clearDatabase();
        			List<Recipe> list = null;
        			if (loadFromFiles) { list = loadFromFiles(context); }
        			else { list = loadFromRaw(context); }
        			for (Recipe r: list) {
						try {
							if (!db.exists(r.getName())) db.insert(r);
						}
						catch (Exception e) {
							Log.e(TAG,"Exception saving recipe to database : " + e);
							Log.e(TAG,"Recipe = " + r);
							e.printStackTrace();
						}
        			}
        			db.close();    		
        		}
        		catch (Exception e) {
        			Log.e(TAG,"Exception saving to database : " + e);
        			e.printStackTrace();
        		}

        	//}
        //}.start() ;
    
    }

    private static List<Recipe> loadFromFiles(final Context context) {
        List<Recipe> list = new ArrayList<Recipe>(0);
		Log.i(TAG,"Files are using dir " + context.getFilesDir());
		String[] files = context.fileList();
		for (int i = 0; i < files.length; i++) {
			String file = files[i];
			Log.i(TAG,"Found file " + file);
			if (file.startsWith("recipe_") && file.endsWith(".xml")) {
				try {
					Recipe r = RecipeParser.parse(context.openFileInput(file));
				}
				catch (Exception e) {
					Log.e(TAG,"Exception " + e + " on file " + file);
					e.printStackTrace();
				}
			}
		}
		Log.i(TAG,"Got " + list.size() + " recipes from files");
		return list;
    }
    
    private static List<Recipe> loadFromRaw(final Context context) {
    	//int[] recipeIds = new int[] {
    	//		 R.raw.roast_pork, R.raw.lunch_with_jesus_and_helen, R.raw.recipe_roast_turkey };
    	List<Integer> recipeIdList = new ArrayList<Integer>();
    	Field[] fields = R.raw.class.getFields();
    	for (Field f: fields) {
    		try {
    			recipeIdList.add(f.getInt(null));
    		}
    		catch (Exception e) {
    			Log.w(TAG,"Got exception getting a recipe id: " + e);
    		}
    	}
    	List<Recipe> list = new ArrayList<Recipe>(0);
        for (int i = 0; i < recipeIdList.size(); i++) {
        	try { 
        		// TODO - instead of parsing file, just get the name and description
        		Recipe r = RecipeParser.parse(context.getResources().openRawResource(recipeIdList.get(i)));
        		r.setId(i);
        		r.setFileName("file" + i);
        		list.add(r);
        	}
        	catch (Exception e) {
    			e.printStackTrace();
    			list.add(new Recipe("could not load this recipe"));       			
        	}
        }
		Log.i(TAG,"Got " + list.size() + " recipes from raw files");
		return list;
    }

}
