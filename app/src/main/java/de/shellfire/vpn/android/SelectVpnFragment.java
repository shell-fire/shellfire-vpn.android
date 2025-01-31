package de.shellfire.vpn.android;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;

import de.shellfire.vpn.android.viewmodel.SharedViewModel;
import de.shellfire.vpn.android.viewmodel.SharedViewModelFactory;

public class SelectVpnFragment extends Fragment {

    private static final String TAG = "SelectVpnFragment";
    private SelectVpnViewModel selectVpnViewModel;
    private VpnRepository vpnRepository;
    private Observer<List<Vpn>> vpnListObserver;
    private SharedViewModel sharedViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        setRetainInstance(true);

        // Initialize SharedViewModel
        sharedViewModel = new ViewModelProvider(requireActivity(), new SharedViewModelFactory(
                getContext().getApplicationContext()
        )).get(SharedViewModel.class);


        selectVpnViewModel = new ViewModelProvider(this).get(SelectVpnViewModel.class);
        vpnRepository = VpnRepository.getInstance(requireContext());

    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        return inflater.inflate(R.layout.fragment_select_vpn, container, false);
    }

    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        observeVpnList();
    }


    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView");
        super.onDestroyView();
        unobserveVpnList();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.select_vpn, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void observeVpnList() {
        vpnListObserver = new Observer<List<Vpn>>() {
            @Override
            public void onChanged(List<Vpn> vpnList) {
                if (vpnList == null) {
                    Log.d(TAG, "observeVpnList - vpnList is null");
                } else {
                    Log.d(TAG, "observeVpnList - vpnList is not null, calling handleVpnList");
                    handleVpnList(vpnList);
                }
            }
        };

        selectVpnViewModel.getVpnListLiveData().observeForever(vpnListObserver);
    }

    private void unobserveVpnList() {
        selectVpnViewModel.getVpnListLiveData().removeObserver(vpnListObserver);
    }

    private void handleVpnList(List<Vpn> vpnList) {
        if (vpnRepository.getIsSettingServer().getValue()) {
            Log.d(TAG, "handleVpnList - isSettingServer is true, returning");
            return;
        }

        final ListView listview = requireView().findViewById(R.id.listViewVpnSelection);
        final VpnListAdapter adapter = new VpnListAdapter(requireContext(), R.layout.vpn_list_item, vpnList);
        listview.setAdapter(adapter);

        listview.setOnItemClickListener((parent, view, position, id) -> {
            final Vpn item = (Vpn) parent.getItemAtPosition(position);
            Log.d(TAG, "handleVpnList - onclick - selected vpn: " + item);
            sharedViewModel.setSelectedVpn(item.getVpnId());

            unobserveVpnList();
        });
    }


    private class VpnListAdapter extends ArrayAdapter<Vpn> {
        final HashMap<Vpn, Integer> mIdMap = new HashMap<>();
        private List<Vpn> mObjects;

        public VpnListAdapter(Context context, int textViewResourceId, List<Vpn> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
                mObjects = objects;
            }
        }

        @Override
        public long getItemId(int position) {
            Vpn item = getItem(position);
            if (mIdMap != null) {
                Object res = mIdMap.get(item);
                if (res != null && res instanceof Integer) {
                    return (int) res;
                }
            }
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Vpn vpn = mObjects.get(position);

            LayoutInflater inflater = (LayoutInflater) requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.vpn_list_item, parent, false);

            TextView nameView = rowView.findViewById(R.id.vpnName);
            nameView.setText(vpn.getName().trim());

            ServerType serverType = vpn.getAccountType();

            ImageView star1 = rowView.findViewById(R.id.star_1);
            ImageView star2 = rowView.findViewById(R.id.star_2);
            ImageView star3 = rowView.findViewById(R.id.star_3);

            if (serverType == ServerType.Free) {
                star1.setVisibility(View.VISIBLE);
                star2.setVisibility(View.GONE);
                star3.setVisibility(View.GONE);
            } else if (serverType == ServerType.Premium) {
                star1.setVisibility(View.VISIBLE);
                star2.setVisibility(View.VISIBLE);
                star3.setVisibility(View.GONE);
            } else {
                star1.setVisibility(View.VISIBLE);
                star2.setVisibility(View.VISIBLE);
                star3.setVisibility(View.VISIBLE);
            }

            return rowView;
        }
    }
}
