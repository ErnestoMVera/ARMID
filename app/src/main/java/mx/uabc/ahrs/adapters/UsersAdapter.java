package mx.uabc.ahrs.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mx.uabc.ahrs.R;
import mx.uabc.ahrs.entities.User;

public class UsersAdapter extends BaseAdapter {

    private List<User> userList;

    public UsersAdapter() {
        this.userList = new ArrayList<>();
    }

    public void addItems(List<User> users) {
        this.userList.clear();
        this.userList.addAll(users);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return userList.size();
    }

    @Override
    public User getItem(int i) {
        return userList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return getItem(i).uid;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        if (view == null) {
            view = LayoutInflater.from(viewGroup.getContext()).
                    inflate(R.layout.users_item, viewGroup, false);
        }

        User user = getItem(i);

        TextView name = view.findViewById(R.id.name);
        TextView details = view.findViewById(R.id.details);

        name.setText(user.name);
        details.setText(user.gender + ", " + user.age);

        return view;
    }
}
