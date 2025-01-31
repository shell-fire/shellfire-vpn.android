package de.shellfire.vpn.android;


import android.widget.HorizontalScrollView;

public class TabletServerListFragment extends ServerListFragment {

    private static final String TAG = "TbltServerSelectSecFra";
    private static final int sProgress = 100;
    private static String sSearchByUserText = "";
    private static boolean sIsVisibleFilter;
    private HorizontalScrollView continentLayout;


    public void updateServerSelect() {
        // TODO: Check if this is needed
        updateFilterUI();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (searchedText != null && searchedText.getText() != null) {
            sSearchByUserText = searchedText.getText().toString();
        }

    }


}
