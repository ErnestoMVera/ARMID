package mx.uabc.ahrs.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mx.uabc.ahrs.R;
import mx.uabc.ahrs.models.Setting;

public class SettingsAdapter extends BaseAdapter {

    private List<Setting> settingList;

    public SettingsAdapter() {
        this.settingList = new ArrayList<>();
    }

    public void addItems(List<Setting> settings) {
        this.settingList.clear();
        this.settingList.addAll(settings);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return settingList.size();
    }

    @Override
    public Setting getItem(int i) {
        return settingList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
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
