package ch.mbuehler.eth.mgis.treasurego;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Adapter class to display Treasure objects.
 * Inspired by
 * https://www.journaldev.com/10416/android-listview-with-custom-adapter-example-tutorial
 */
public class TreasureAdapter extends ArrayAdapter<Treasure> {

    /**
     * View lookup cache
     */
    private static class ViewHolder {
        TextView nameText;  // Name of the Treasure
        TextView achievedRewardText;  // Achieved Reward for a given Treasure
        TextView maxRewardText;  // Maximum reward of the Treasure
        ImageView image;  // Image of the Treasure
    }

    /**
     * @param data           The treasures to be displayed
     * @param context        ApplicationContext
     */
    TreasureAdapter(ArrayList<Treasure> data, Context context) {
        super(context, R.layout.treasure_row, data);
    }

    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        // Get the data item for this position
        Treasure treasure = getItem(position);
        // Check if an existing arActivityView is being reused, otherwise inflate the arActivityView
        ViewHolder viewHolder; // arActivityView lookup cache stored in tag

        // Only create a new View if necessary
        if (convertView == null) {
            // we need to create a new View
            viewHolder = new ViewHolder();
            // Create the arActivityView with our RelativeLayout template
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.treasure_row, parent, false);
            // Set the required Text- and ImageViews
            viewHolder.nameText = convertView.findViewById(R.id.name);
            viewHolder.achievedRewardText = convertView.findViewById(R.id.achievedReward);
            viewHolder.maxRewardText = convertView.findViewById(R.id.MaxRewardValue);
            viewHolder.image = convertView.findViewById(R.id.item_info);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // Set the text values
        viewHolder.nameText.setText(treasure.getName());
        viewHolder.achievedRewardText.setText(getAchievedRewardText(treasure.getUuid()));
        viewHolder.maxRewardText.setText(Integer.valueOf(treasure.getReward()).toString());

        // Set the image
        // Choose Treasure picture depending on whether the Treasure has been found or not
        if (GameStatus.Instance().hasCompletedQuest(treasure.getUuid())) {
            // The user has already found the Treasure
            viewHolder.image.setImageResource(R.drawable.treasurego_launcher);
        } else {
            // The Treasure has yet to be found
            viewHolder.image.setImageResource(R.drawable.treasure_closed_small);
        }
        // Return the completed arActivityView
        return convertView;
    }

    private String getAchievedRewardText(String treasureUuid) {
        int achievedReward = 0;
        if (GameStatus.Instance().getTreasureQuests().keySet().contains(treasureUuid)) {
            achievedReward = GameStatus.Instance().getMaxReward(GameStatus.Instance().getTreasureQuests().get(treasureUuid));
        }

        String achievedRewardText = String.format("%d", achievedReward);
        return achievedRewardText;
    }
}
