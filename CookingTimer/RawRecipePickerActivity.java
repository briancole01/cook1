package au.com.brian.timer.activity;

import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import au.com.brian.timer.R;
import au.com.brian.timer.R.layout;
import au.com.brian.timer.R.raw;
import au.com.brian.timer.domain.Recipe;
import au.com.brian.timer.util.RecipeParser;

public class RawRecipePickerActivity extends ListActivity {

	private ListView listView;
	private static final String TAG = "RecipePickerActivity";
	private static int[] recipeIds = new int[] {
		R.raw.roast_lamb, R.raw.test1, R.raw.test2, R.raw.test3, R.raw.roast_pork 
	};
	
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
        String[] recipeNames = new String[recipeIds.length];
        for (int i = 0; i < recipeIds.length; i++) {
        	try { 
        		// TODO - instead of parsing file, just get the name and description
        		Recipe r = RecipeParser.parse(getResources().openRawResource(recipeIds[i]));
        		recipeNames[i] = r.getName();
        	}
        	catch (Exception e) {
    			e.printStackTrace();
    			recipeNames[i] = "could not load this recipe";
        			
        	}
        }
		setListAdapter((new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, recipeNames)));
		listView.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Log.i(TAG,"got long click on view " + v);
				return false;
			}
		});
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Log.i(TAG, "Got click on " + position);
		        Intent intent = new Intent(RawRecipePickerActivity.this, RecipeIntroActivity.class);
		        intent.putExtra("recipeId",recipeIds[position]);
	            startActivityForResult(intent,0);
			}
		});
    }
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent result) {
		if (resultCode == RESULT_OK) {
			returnResult();
		}
	}
	
    void returnResult() {
    	Intent i = new Intent();
    	setResult(RESULT_OK, i);
    	finish();
    }
    
}
