package de.shellfire.vpn.android.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import de.shellfire.vpn.android.R;
import de.shellfire.vpn.android.Server;
import de.shellfire.vpn.android.ServerListFragment;
import de.shellfire.vpn.android.ServerType;
import de.shellfire.vpn.android.Vpn;
import de.shellfire.vpn.android.VpnRepository;
import de.shellfire.vpn.android.databinding.ServerListItemBinding;
import de.shellfire.vpn.android.utils.CountryUtils;

public class ServerListAdapter extends ArrayAdapter<Server> implements Filterable {
    private static final String TAG = "ServerListAdapter";
    private static LayoutInflater inflater;
    private final Context mContext;
    private final ServerListFragment serverListFragment;

    final HashMap<Server, Integer> mIdMap = new HashMap<Server, Integer>();
    private List<Server> mObjects;
    private List<Server> filteredData;
    private boolean isDisplayingLoadProgressBar;
    private int loadProgress = 100;
    private final List<Server> origin;
    private String localeLanguage;
    private boolean isAdvancedList;
    private final VpnRepository vpnRepository;
    private Observer<Server> selectedServerObserver;
    private LifecycleOwner mLifecycleOwner;


    public ServerListAdapter(Context context, int textViewResourceId, List<Server> serverList, boolean isAdvancedList, ServerListFragment serverListFragment, LifecycleOwner lifecycleOwner) {
        super(context, textViewResourceId, serverList);
        Log.d(TAG, "ServerListAdapter - constructor called");

        this.serverListFragment = serverListFragment;
        origin = serverList;
        mContext = context;
        mLifecycleOwner = lifecycleOwner;
        for (int i = 0; i < serverList.size(); ++i) {
            mIdMap.put(serverList.get(i), i);

            mObjects = serverList;
        }

        this.filteredData = new ArrayList<>(serverList);
        inflater = LayoutInflater.from(mContext);
        localeLanguage = Locale.getDefault().getDisplayLanguage();

        this.isAdvancedList = isAdvancedList;

        vpnRepository = VpnRepository.getInstance(context);
    }

    public String getLocale() {
        return localeLanguage;
    }

    public void setLocale(String localeLanguage) {
        this.localeLanguage = localeLanguage;

        notifyDataSetChanged();
    }

    public int getCount() {
        return filteredData == null ? 0 : filteredData.size();
    }

    public Server getItem(int position) {
        return filteredData.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        Server serverToDraw = filteredData.get(position);
        ViewHolder holder;
        ServerListItemBinding binding;

        if (convertView == null) {
            binding = ServerListItemBinding.inflate(inflater, parent, false);
            holder = new ViewHolder(binding);
            convertView = binding.getRoot();
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
            binding = holder.binding;
        }

        // Ensure the item is clickable as soon as it's displayed
        convertView.setOnClickListener(v -> {
            if (serverListFragment != null) {
                serverListFragment.handleServerChange(serverToDraw);
            }

        });

        View finalConvertView = convertView;
        selectedServerObserver = new Observer<>() {
            @Override
            public void onChanged(Server selectedServer) {
                ServerListAdapter.this.updateCountryFlag(binding, finalConvertView, holder, serverToDraw, selectedServer);
                ServerListAdapter.this.adjustTextColors(binding, finalConvertView, serverToDraw, selectedServer);
                vpnRepository.getSelectedServer().removeObserver(this);
            }
        };

        vpnRepository.getSelectedServer().observeForever(selectedServerObserver);
        updateTextFields(binding, serverToDraw);
        updateStarRatings(binding, serverToDraw);
        updateServerDetails(binding, convertView, serverToDraw);
        adjustTextSizeAfterLayout(binding);
        return convertView;
    }


    private void updateTextFields(ServerListItemBinding binding, Server server) {
        String vpnServerIdString = String.valueOf(server.getVpnServerId());

        binding.vpnCountry.setText(server.getCountryPrint());

        binding.vpnCity.setText(server.getCity());

        binding.vpnAccountType.setText(server.getServerType().toString());
        binding.serverNum.setText(mContext.getString(R.string.server) + " " + server.getVpnServerId());
    }


    private void updateCountryFlag(ServerListItemBinding binding, View convertView, ViewHolder holder, Server serverToDraw, Server selectedServer) {
        int countryImageResId = CountryUtils.getCountryFlagImageResId(serverToDraw.getCountryEnum());
        int borderResId = R.drawable.border;

        if (selectedServer != null && selectedServer.getVpnServerId() == serverToDraw.getVpnServerId()) {
            borderResId = R.drawable.border_selected;
        }

        Drawable borderDrawable = ContextCompat.getDrawable(mContext, borderResId);
        Drawable flagDrawable = getRoundedDrawable(countryImageResId);

        LayerDrawable layerDrawable;

        if (flagDrawable != null) {
            // Create a LayerDrawable with both border and flag
            layerDrawable = new LayerDrawable(new Drawable[]{borderDrawable, flagDrawable});
            int padding = (int) (2 * mContext.getResources().getDisplayMetrics().density);
            // Set inset for the second layer (flagDrawable)
            layerDrawable.setLayerInset(1, padding, padding, padding, padding);
        } else {
            // Create a LayerDrawable with only the border
            layerDrawable = new LayerDrawable(new Drawable[]{borderDrawable});
        }

        // Set the layer drawable as the background
        binding.country.setBackground(layerDrawable);
    }


    private void updateStarRatings(ServerListItemBinding binding, Server server) {

        switch (server.getServerType()) {
            case Free:
                binding.star1.setVisibility(View.VISIBLE);
                binding.star2.setVisibility(View.GONE);
                binding.star3.setVisibility(View.GONE);
                break;
            case Premium:
                binding.star1.setVisibility(View.VISIBLE);
                binding.star2.setVisibility(View.VISIBLE);
                binding.star3.setVisibility(View.GONE);
                break;
            case PremiumPlus:
                binding.star1.setVisibility(View.VISIBLE);
                binding.star2.setVisibility(View.VISIBLE);
                binding.star3.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void updateServerDetails(ServerListItemBinding binding, View convertView, Server server) {
        binding.speedTextView.setText(server.getServerSpeed().getResId());
        binding.securityTextView.setText(server.getSecurity().getResId());

        int load = server.getLoadPercentage();
        binding.loadBar.setProgress(load);
        binding.loadBar.getProgressDrawable().setAlpha(180);
        binding.progressText.setText(load + " %");

        if (!isAdvancedList) {
            hideViewsForCountryList(binding);
            changeListView(binding, server);
        } else {
            showViewsForServerList(binding);
            changeListView(binding, server);
        }
    }

    private void adjustTextColors(ServerListItemBinding binding, View convertView, Server serverToDraw, Server selectedServer) {
        if (serverToDraw != null && selectedServer != null && serverToDraw.getVpnServerId() == selectedServer.getVpnServerId()) {
            convertView.setBackgroundColor(ContextCompat.getColor(convertView.getContext(), R.color.base_blue_color));
            binding.vpnCountry.setTextColor(Color.WHITE);
            binding.vpnCity.setTextColor(Color.WHITE);
            binding.serverNum.setTextColor(ContextCompat.getColor(convertView.getContext(), R.color.white));
            binding.star1.setImageDrawable(ContextCompat.getDrawable(convertView.getContext(), R.drawable.ic_crown_blue));
            binding.star2.setImageDrawable(ContextCompat.getDrawable(convertView.getContext(), R.drawable.ic_crown_blue));
            binding.star3.setImageDrawable(ContextCompat.getDrawable(convertView.getContext(), R.drawable.ic_crown_blue));
            binding.starLayout.setBackground(ContextCompat.getDrawable(convertView.getContext(), R.drawable.rate_white_bg));
            binding.starLayout.setVisibility(View.INVISIBLE);
        }
        else {
            convertView.setBackgroundColor(ContextCompat.getColor(convertView.getContext(), R.color.white));
            binding.vpnCountry.setTextColor(ContextCompat.getColor(convertView.getContext(), R.color.gray_text_color));
            binding.vpnCity.setTextColor(ContextCompat.getColor(convertView.getContext(), R.color.gray_text_color));
            binding.serverNum.setTextColor(ContextCompat.getColor(convertView.getContext(), R.color.gray_text_color));
            binding.star1.setImageDrawable(ContextCompat.getDrawable(convertView.getContext(), R.drawable.ic_crown));
            binding.star2.setImageDrawable(ContextCompat.getDrawable(convertView.getContext(), R.drawable.ic_crown));
            binding.star3.setImageDrawable(ContextCompat.getDrawable(convertView.getContext(), R.drawable.ic_crown));
            binding.starLayout.setBackground(ContextCompat.getDrawable(convertView.getContext(), R.drawable.rate_blue_bg));
        }
    }

    private void adjustTextSizeAfterLayout(ServerListItemBinding binding) {
        if (isAdvancedList) {
            adjustCombinedTextSize(binding, binding.vpnCountry, binding.starLayout, binding.vpnCity);
        }
    }


    private Drawable getRoundedDrawable(int resourceId) {
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), resourceId);
        if (bitmap != null) {
            Bitmap roundedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
            Canvas canvas = new Canvas(roundedBitmap);
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setShader(new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
            float radius = 4 * mContext.getResources().getDisplayMetrics().density;
            canvas.drawRoundRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), radius, radius, paint);
            return new BitmapDrawable(mContext.getResources(), roundedBitmap);
        } else {
            return null;
        }

    }

    public float getFontSizeInPixels(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, mContext.getResources().getDisplayMetrics());
    }

    private void adjustCombinedTextSize(ServerListItemBinding binding, TextView vpnCountry, View starLayout, TextView vpnCity) {
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;

        int availableWidth = (int) (screenWidth * 0.4);

        float textSize = getFontSizeInPixels(16); // Math.min(vpnCity.getTextSize(), vpnCountry.getTextSize());

        textSize = adjustTextSizeToFitWidth(vpnCity, vpnCountry, availableWidth, textSize);
        vpnCity.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        vpnCountry.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    }

    private float adjustTextSizeToFitWidth(TextView vpnCity, TextView vpnCountry, int availableWidth, float initialTextSize) {
        Paint vpnCityPaint = new Paint();
        vpnCityPaint.set(vpnCity.getPaint());
        Paint vpnCountryPaint = new Paint();
        vpnCountryPaint.set(vpnCountry.getPaint());

        String cityText = vpnCity.getText().toString();
        String countryText = vpnCountry.getText().toString();

        float textSize = initialTextSize;

        // Measure the combined width of both texts
        float totalTextWidth = 10000000; // Initialize to a large value

        // Reduce text size if the combined width exceeds the available width
        while (totalTextWidth > availableWidth && textSize > 0) {
            textSize -= 1;
            vpnCityPaint.setTextSize(textSize);
            vpnCountryPaint.setTextSize(textSize);
            totalTextWidth = vpnCityPaint.measureText(cityText) + vpnCountryPaint.measureText(countryText);
        }

        return textSize;
    }

    private void changeListView(ServerListItemBinding binding, Server server) {
        LiveData<Vpn> selectedVpnLiveData = vpnRepository.getSelectedVpn();
        selectedVpnLiveData.observe(mLifecycleOwner, new Observer<Vpn>() {
            @Override
            public void onChanged(Vpn vpn) {
                if (vpn != null) {

                    ServerType myServerType = vpn.getAccountType();
                    if (myServerType == ServerType.PremiumPlus) {
                        setAlphaForCountryItem(binding, 0.7f, 1f);
                    } else if (myServerType == ServerType.Premium) {
                        if (server.getServerType() == ServerType.PremiumPlus) {
                            setAlphaForCountryItem(binding, 0.3f, 0.5f);
                        } else {
                            setAlphaForCountryItem(binding, 0.7f, 1f);
                        }
                    } else if (myServerType == ServerType.Free) {
                        if (server.getServerType() == ServerType.PremiumPlus || server.getServerType() == ServerType.Premium) {
                            setAlphaForCountryItem(binding, 0.3f, 0.5f);
                        } else {
                            setAlphaForCountryItem(binding, 0.7f, 1f);
                        }
                    }
                }

            }
        });

    }

    private void setAlphaForCountryItem(ServerListItemBinding binding, float alphaText, float alphaFlag) {
        binding.vpnCountry.setAlpha(alphaText);
        binding.serverNum.setAlpha(alphaText);
        binding.country.setAlpha(alphaFlag);
        if (isAdvancedList) {
            binding.vpnCity.setAlpha(alphaText);
        }
    }

    private void hideViewsForCountryList(ServerListItemBinding binding) {
        binding.star1.setVisibility(View.INVISIBLE);
        binding.star2.setVisibility(View.INVISIBLE);
        binding.star3.setVisibility(View.INVISIBLE);
        binding.starLayout.setVisibility(View.INVISIBLE);
        binding.vpnCity.setVisibility(View.GONE);
        binding.loadContainer.setVisibility(View.GONE);
    }

    private void showViewsForServerList(ServerListItemBinding binding) {
        binding.starLayout.setVisibility(View.VISIBLE);
        binding.vpnCity.setVisibility(View.VISIBLE);
        binding.serverNum.setVisibility(View.VISIBLE);
        if (isDisplayingLoadProgressBar) {
            binding.loadContainer.setVisibility(View.VISIBLE);
        } else {
            binding.loadContainer.setVisibility(View.GONE);
        }
    }

    public void setAdvancedList(boolean advancedList) {
        isAdvancedList = advancedList;
    }

    public void setFilterList(List<Server> servers) {
        Log.d(TAG, "setFilterList - start");

        if (servers != null) {
            Log.d(TAG, "setFilterList - servers is not null, size: " + servers.size());
            filteredData = servers;
            Log.d(TAG, "setFilterList - filteredData set");

            for (int i = 0; i < servers.size(); ++i) {
                mIdMap.put(servers.get(i), i);
                mObjects = servers;
            }

            Log.d(TAG, "setFilterList - mIdMap and mObjects updated");
            notifyDataSetChanged();
            Log.d(TAG, "setFilterList - notifyDataSetChanged called");
        } else {
            Log.d(TAG, "setFilterList - servers is null");
        }

        Log.d(TAG, "setFilterList - end");
    }


    public void setFilterListNotNotify(List<Server> servers) {
        Log.d(TAG, "setFilterListNotNotify - start");

        filteredData = servers;
        for (int i = 0; i < servers.size(); ++i) {
            mIdMap.put(servers.get(i), i);
            mObjects = servers;
        }
    }

    private void hideCrowns(View rowView, int id) {
        ImageView crownView = rowView.findViewById(id);
        crownView.setVisibility(View.GONE);
    }

    public void setDisplayLoadProgressBar(boolean displayLoadProgressBar) {
        this.isDisplayingLoadProgressBar = displayLoadProgressBar;
        notifyDataSetChanged();
    }

    public void setDisplayLoadProgressBarNoNotify(boolean displayLoadProgressBar) {
        this.isDisplayingLoadProgressBar = displayLoadProgressBar;
    }

    public void setLoadProgressNotNotify(int loadProgress) {
        this.loadProgress = loadProgress;
    }

    static class ViewHolder {
        final ServerListItemBinding binding;

        public ViewHolder(ServerListItemBinding binding) {
            this.binding = binding;
        }
    }

}
