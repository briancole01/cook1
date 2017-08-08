package au.com.brian.timer.activity;

import java.util.ArrayList;
import java.util.List;

import roboguice.activity.RoboListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import au.com.brian.timer.R;
import au.com.brian.timer.domain.Recipe;
import au.com.brian.timer.util.RecipeDBHelper;

public class RecipePickerActivity extends RoboListActivity {

	private ListView listView;
	private static final String TAG = "RecipePickerActivity";
	List<RecipeListInfo> recipeListInfo = new ArrayList<RecipeListInfo>();
	
	class RecipeListInfo {
		public long id;
		public String name;
		public String intro;
		public String file;
		public RecipeListInfo(long id,String name, String intro, String file) {
			this.id = id;
			this.name = name;
			this.intro = intro;
			this.file = file;
		}
	}
	
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 /*
        // Watch for button clicks.
        Button button = (Button)findViewById(R.id.startButton);
        if (button != null) button.setOnClickListener(startListener);
 */       
        setContentView(R.layout.recipe_picker);
        listView = getListView();
        List<Recipe> list = loadFromDB();
		for (Recipe r: list) {
			Log.d(TAG,r.toDebug());
			recipeListInfo.add(new RecipeListInfo(r.getId(), r.getName(), r.getIntro(), r.getFileName()));
		}
		setListAdapter((new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, getRecipeNames())));
		listView.setOnLongClickListener(new OnLongClickListener() {
			//@Override
			public boolean onLongClick(View v) {
				Log.i(TAG,"got long click on view " + v);
				return false;
			}
		});
		listView.setOnItemClickListener(new OnItemClickListener() {
			//@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Log.i(TAG, "Got click on " + position + ", recipeId = " + recipeListInfo.get(position).id);
		        Intent intent = new Intent(RecipePickerActivity.this, RecipeIntroActivity.class);
		        intent.putExtra("recipeId",recipeListInfo.get(position).id);
	            startActivity(intent);
			}
		});
    }
    
    private List<Recipe> loadFromDB() {
        List<Recipe> list = new ArrayList<Recipe>(0);
        RecipeDBHelper db = null;
		try {
			db = new RecipeDBHelper(getApplication().getBaseContext());
			list = db.getAll();
		}
		catch (Exception e) {
			Log.e(TAG,"Exception reading from database : " + e);
			e.printStackTrace();
		}
		finally { if (db != null) db.close(); }
		Log.i(TAG,"Got " + list.size() + " recipes from the database");
		return list;
    }
        
   private String[] getRecipeNames() {
    	String[] array = new String[recipeListInfo.size()];
    	for (int i = 0; i < recipeListInfo.size(); i++) {
    		array[i] = recipeListInfo.get(i).name;
    		//Log.d(TAG,"recipe "  + i + ": id=" + recipeListInfo.get(i).id + ",name=" + recipeListInfo.get(i).name);
     	}
    	return array;
    }
  /*  
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent result) {
		if (resultCode == RESULT_OK) {
			returnResult();
		}
	}
	
    void returnResult() {
    	Intent i = new Intent();
    	setResult(RESULT_OK, i);
		Log.i(TAG,"returning result");
    	finish();
    }
    */
    
}
