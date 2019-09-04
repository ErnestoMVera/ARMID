package mx.uabc.ahrs.adapters;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mx.uabc.ahrs.R;
import mx.uabc.ahrs.models.DataPoint;

public class TrainingAdapter extends BaseAdapter {

    private List<List<DataPoint>> pointsList;
    private int selectedIndex;
    private int spots;

    public TrainingAdapter(int spots) {

        this.pointsList = new ArrayList<>();
        this.spots = spots;

        for (int i = 0; i < spots; i++) {
            pointsList.add(new ArrayList<>());
        }

    }

    public List<List<DataPoint>> getPointsList() {
        return this.pointsList;
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
        notifyDataSetChanged();
    }

    public void addItem(int position, DataPoint dataPoint) {
        this.pointsList.get(position).add(dataPoint);
        notifyDataSetChanged();
    }

    public void clearItem(int position) {
        this.pointsList.get(position).clear();
        notifyDataSetChanged();
    }

    public void clearItems() {

        for (int i = 0; i < this.spots; i++) {
            pointsList.get(i).clear();
        }

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return pointsList.size();
    }

    @Override
    public List<DataPoint> getItem(int i) {
        return pointsList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        if (view == null) {
            view = LayoutInflater.from(viewGroup.getContext()).
                    inflate(R.layout.training_item, viewGroup, false);
        }

        List<DataPoint> dataPoints = getItem(i);

        TextView spot = view.findViewById(R.id.spot);
        TextView samples = view.findViewById(R.id.samples);

        spot.setText(DataPoint.getSpotName(i));
        String text = dataPoints.size() + " muestras";
        samples.setText(text);

        int textColor = i == selectedIndex ? viewGroup.getContext().getResources().getColor(R.color.colorAccent)
                : viewGroup.getContext().getResources().getColor(R.color.textColorPrimary);

        int typeface = i == selectedIndex ? Typeface.BOLD : Typeface.NORMAL;

        spot.setTypeface(null, typeface);
        spot.setTextColor(textColor);
        samples.setTypeface(null, typeface);
        samples.setTextColor(textColor);

        return view;
    }
}
