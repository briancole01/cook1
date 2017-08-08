package au.com.brian.timer.util;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import au.com.brian.timer.R;
import au.com.brian.timer.domain.Task;

/** From the api sample List14.java */
public class ListAdapterView extends BaseAdapter {
    private LayoutInflater mInflater;
    private Bitmap icon1;
    private Bitmap icon2;
    private Bitmap icon_not_complete;
    private Bitmap icon_complete;
    private List<Task> list;
    public static final int EDIT_LAYOUT = R.layout.editable_list_item;

    public ListAdapterView(Context context, List<Task> list) {
        // Cache the LayoutInflate to avoid asking for a new one each time.
        mInflater = LayoutInflater.from(context);
        this.list = list;
        // Icons bound to the rows.
        icon1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon48x48_1);
        icon2 = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon48x48_2);
        icon_not_complete = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon48x48_1);
        icon_complete = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon48x48_2);
    }

    //@Override
    public int getCount() {
        return list.size();
    }

    //@Override
    public Task getItem(int position) {
        return list.get(position);
    }

    //@Override
    public long getItemId(int position) {
        return position;
    }

    //@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to children views to avoid unneccessary calls
        // to findViewById() on each row.
        ViewHolder holder;

        // When convertView is not null, we can reuse it directly, there is no need
        // to reinflate it. We only inflate a new View when the convertView supplied
        // by ListView is null.
        if (convertView == null) {
            convertView = mInflater.inflate(EDIT_LAYOUT, null);

            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            holder = new ViewHolder();
            holder.description = (TextView) convertView.findViewById(R.id.description);
           	//holder.editIcon = (ImageView) convertView.findViewById(R.id.edit_icon);
           	//holder.deleteIcon = (ImageView) convertView.findViewById(R.id.delete_icon);
            //holder.complete = (ImageView) convertView.findViewById(R.id.complete);
           //holder.icon = (ImageView) convertView.findViewById(R.id.icon);

            convertView.setTag(holder);
        } else {
            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolder) convertView.getTag();
        }

        // Bind the data efficiently with the holder.
        Task t = list.get(position);
        if (t.hasTasks()) {
        	// append details of the children to the description
        	String s = "";
        	for (Task t2: t.getTasks()) {
        	   s += t2.getTitle() + " <small>(" + t2.getMinutes() + " mins)</small>, ";
        	}
        	if (s.length() > 2) {
        		s = ": " + s.substring(0, s.length()-2);    		
        	}
        	 holder.description.setText(Html.fromHtml("<b>" + t.getTitle() + "</b>" + s));
        }
        else {
            holder.description.setText(t.getTitle());
        }
        	        	
	   // holder.editIcon.setImageBitmap(icon1);
	   // holder.deleteIcon.setImageBitmap(icon1);
        //holder.icon.setImageBitmap((position & 1) == 1 ? mIcon1 : mIcon2);
        //holder.complete.setImageBitmap(t.isComplete() ? icon_complete : icon_not_complete);
        return convertView;
    }

    static class ViewHolder {
        TextView description;
        //ImageView editIcon;
        //ImageView deleteIcon;
        //ImageView icon;
        //ImageView complete;
    }
}