package mx.uabc.ahrs.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mx.uabc.ahrs.R;
import mx.uabc.ahrs.models.DataPoint;
import mx.uabc.ahrs.models.RecollectionData;
import mx.uabc.ahrs.models.Setting;

public class RecollectionAdapter extends BaseAdapter {

    private List<Setting> dataList;

    public RecollectionAdapter() {
        this.dataList = new ArrayList<>();
        this.dataList.add(new Setting("Spot", "N/A"));
        this.dataList.add(new Setting("Velocidad", "N/A"));
        this.dataList.add(new Setting("GPS", "N/A"));
    }

    public void addItem(RecollectionData recollectionData) {

        dataList.clear();
        dataList.add(new Setting("Spot", DataPoint.getSpotName(recollectionData.getSpot())));
        dataList.add(new Setting("Velocidad", (recollectionData.getSpeed() * 3.6) + " km/h"));
        dataList.add(new Setting("GPS", recollectionData.getLat() + ", " + recollectionData.getLng()));
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public Setting getItem(int i) {
        return dataList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        if (view == null) {
            view = LayoutInflater.from(viewGroup.getContext()).
                    inflate(R.layout.settings_item, viewGroup, false);
        }

        Setting setting = getItem(i);

        TextView title = view.findViewById(R.id.title);
        TextView value = view.findViewById(R.id.value);

        title.setText(setting.getTitle());
        value.setText(setting.getValue());

        return view;
    }
}
