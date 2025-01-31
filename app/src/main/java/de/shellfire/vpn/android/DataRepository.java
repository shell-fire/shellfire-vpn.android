package de.shellfire.vpn.android;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import java.util.Arrays;
import java.util.List;

import de.shellfire.vpn.android.auth.AuthRepository;
import de.shellfire.vpn.android.auth.LoginStatus;
import de.shellfire.vpn.android.model.Alias;
import de.shellfire.vpn.android.webservice.JsonWebService;
import de.shellfire.vpn.android.webservice.ShellfireWebService;
import de.shellfire.vpn.android.webservice.WebService;
import de.shellfire.vpn.android.webservice.model.AliasListContainer;
import de.shellfire.vpn.android.webservice.model.BaseResponse;
import de.shellfire.vpn.android.webservice.model.VpnAttributeList;
import retrofit2.Call;

public class DataRepository {

    private static final String TAG = "DtaRep";
    private static DataRepository instance;
    private final ShellfireWebService webService;
    private final JsonWebService jsonWebService;

    // DAO instances
    private final VpnAttributeListDao vpnAttributeListDao;
    private final SkuDao skuDao;
    private final HelpItemDao helpItemDao;
    private final ServerDao serverDao;
    private final AliasDao aliasDao;

    // LiveData objects
    private final MediatorLiveData<VpnAttributeList> vpnAttributeListLiveData;
    private final MediatorLiveData<List<Sku>> skuAndroidListLiveData;
    private final MediatorLiveData<List<Sku>> skuAndroidPassesListLiveData;
    private final MediatorLiveData<List<HelpItem>> helpItemListLiveData;
    private final MediatorLiveData<List<Server>> serverListLiveData;
    private final MutableLiveData<Boolean> isServerListLoading;
    private final MediatorLiveData<Alias[]> aliasListLiveData;
    private final MutableLiveData<Boolean> isVpnAttributeListLoading;
    private final MutableLiveData<Boolean> isSkuAndroidListLoading;
    private final MutableLiveData<Boolean> isSkuAndroidPassesListLoading;
    private final MutableLiveData<Boolean> isHelpItemListLoading;
    private final MutableLiveData<Boolean> isAliasListLoading;
    private final AuthRepository authRepository;

    private DataRepository(Context context) {
        Log.d(TAG + ".cons", "DataRepository() - start");

        webService = ShellfireWebService.getInstance(context);
        jsonWebService = WebService.getInstance(context).getJsonWebService();

        // Initialize DAO instances
        vpnAttributeListDao = AppDatabase.getDatabase(context).vpnAttributeListDao();
        skuDao = AppDatabase.getDatabase(context).skuDao();
        serverDao = AppDatabase.getDatabase(context).serverDao();
        aliasDao = AppDatabase.getDatabase(context).aliasDao();

        // Initialize LiveData objects
        vpnAttributeListLiveData = new MediatorLiveData<>();
        skuAndroidListLiveData = new MediatorLiveData<>();
        skuAndroidPassesListLiveData = new MediatorLiveData<>();
        helpItemDao = AppDatabase.getDatabase(context).helpItemDao();
        helpItemListLiveData = new MediatorLiveData<>();
        serverListLiveData = new MediatorLiveData<>();
        isServerListLoading = new MutableLiveData<>(false);
        aliasListLiveData = new MediatorLiveData<>();
        isVpnAttributeListLoading = new MutableLiveData<>(false);
        isSkuAndroidListLoading = new MutableLiveData<>(false);
        isSkuAndroidPassesListLoading = new MutableLiveData<>(false);
        isHelpItemListLoading = new MutableLiveData<>(false);
        isAliasListLoading = new MutableLiveData<>(false);

        // Load data from local database on startup for VPN attribute list
        Log.d(TAG + ".cons", "Loading data from local database on startup for VPN attribute list");
        LiveData<VpnAttributeList> localVpnAttributeListData = Transformations.map(
                vpnAttributeListDao.getVpnAttributeList(),
                entity -> {
                    Log.d(TAG + ".cons", "Transforming VpnAttributeListEntity to VpnAttributeList");
                    if (entity != null && entity.getDataJson() != null) {
                        return VpnAttributeListConverter.toVpnAttributeList(entity.getDataJson());
                    } else {
                        return null;
                    }
                });


        vpnAttributeListLiveData.addSource(localVpnAttributeListData, vpnAttributeList -> {
            Log.d(TAG + ".cons", "VpnAttributeList LiveData changed");
            if (vpnAttributeList != null) {
                Log.d(TAG + ".cons", "VpnAttributeList is not null, setting LiveData value");
                vpnAttributeListLiveData.setValue(vpnAttributeList);
            }
        });

        // Load data from local database on startup for Android SKUs
        Log.d(TAG + ".cons", "Loading data from local database on startup for Android SKUs");
        LiveData<List<Sku>> localSkuAndroidData = skuDao.getSkusByType("sub");
        skuAndroidListLiveData.addSource(localSkuAndroidData, skus -> {
            Log.d(TAG + ".cons", "SkuAndroidList LiveData changed");
            if (skus != null && !skus.isEmpty()) {
                Log.d(TAG + ".cons", "SkuAndroidList is not null and not empty, setting LiveData value");
                skuAndroidListLiveData.setValue(skus);
            }
        });


        // Load data from local database on startup for Android Passes SKUs
        Log.d(TAG + ".cons", "Loading data from local database on startup for Android Passes SKUs");
        LiveData<List<Sku>> localSkuAndroidPassesData = skuDao.getSkusByType("item");
        skuAndroidPassesListLiveData.addSource(localSkuAndroidPassesData, skus -> {
            Log.d(TAG + ".cons", "SkuAndroidPassesList LiveData changed");
            if (skus != null && !skus.isEmpty()) {
                Log.d(TAG + ".cons", "SkuAndroidPassesList is not null and not empty, setting LiveData value");
                skuAndroidPassesListLiveData.setValue(skus);
            }
        });


        // Load data from local database on startup for help items
        Log.d(TAG + ".cons", "Loading data from local database on startup for help items");
        LiveData<List<HelpItem>> localHelpItemData = helpItemDao.getAllHelpItems();
        helpItemListLiveData.addSource(localHelpItemData, helpItems -> {
            Log.d(TAG + ".cons", "HelpItemList LiveData changed");
            if (helpItems != null && !helpItems.isEmpty()) {
                Log.d(TAG + ".cons", "HelpItemList is not null and not empty, setting LiveData value");
                helpItemListLiveData.setValue(helpItems);
            }
        });


        // Load data from local database on startup for servers
        Log.d(TAG + ".cons", "Loading data from local database on startup for servers");
        LiveData<List<Server>> localServerData = serverDao.getAllServers();
        serverListLiveData.addSource(localServerData, servers -> {
            Log.d(TAG + ".cons", "ServerList LiveData changed");
            if (servers != null && !servers.isEmpty()) {
                Log.d(TAG + ".cons", "ServerList is not null and not empty, setting LiveData value");
                serverListLiveData.setValue(servers);
            }
        });

        // Load data from local database on startup for aliases
        Log.d(TAG + ".cons", "Loading data from local database on startup for aliases");
        LiveData<Alias[]> localAliasData = aliasDao.getAllAliases();
        aliasListLiveData.addSource(localAliasData, aliasEntities -> {
            Log.d(TAG + ".cons", "AliasList LiveData changed");
            if (aliasEntities != null && aliasEntities.length > 0) {
                Log.d(TAG + ".cons", "AliasList is not null and not empty, setting LiveData value");
                aliasListLiveData.setValue(aliasEntities);
            }
        });

        fetchAllData();

        authRepository = AuthRepository.getInstance(context);
        observeLoginStatus();

        Log.d(TAG + ".cons", "DataRepository() - end");
    }

    private void observeLoginStatus() {
        // When the user has logged in, we need to reload the data as the language may have changed
        authRepository.getLoginStatus().observeForever(loginStatus -> {
            if (loginStatus == LoginStatus.LoggedIn) {
                fetchAllData();
            }
        });
    }

    private void fetchAllData() {
        new Handler(Looper.getMainLooper()).post(() -> {
            fetchVpnAttributeList();
            fetchSkuAndroidList();
            fetchSkuAndroidPassesList();
            fetchHelpItemList();
            fetchServerList();
            fetchAliasList();
        });
    }

    public static synchronized DataRepository getInstance(Context context) {
        Log.d(TAG + ".getIns", "getInstance() - start");
        if (instance == null) {
            Log.d(TAG + ".getIns", "Instance is null, creating new DataRepository");
            instance = new DataRepository(context);
        }
        Log.d(TAG + ".getIns", "getInstance() - end");
        return instance;
    }

    public LiveData<VpnAttributeList> getVpnAttributeList() {
        return vpnAttributeListLiveData;
    }

    private void fetchVpnAttributeList() {
        Log.d(TAG + ".fetchVpnAttrList", "fetchVpnAttributeList() - start");
        if (Boolean.TRUE.equals(isVpnAttributeListLoading.getValue())) {
            Log.d(TAG + ".fetchVpnAttrList", "VPN attribute list is already loading, returning");
            return;
        }

        isVpnAttributeListLoading.setValue(true);
        Log.d(TAG + ".fetchVpnAttrList", "Set isVpnAttributeListLoading to true");

        Call<BaseResponse<VpnAttributeList>> call = jsonWebService.getComparisonTable();
        webService.makeAsyncCall(call, "Failed to get comparison table data", new ShellfireWebService.BaseCallback<VpnAttributeList>() {
            @Override
            public void onSuccess(VpnAttributeList vpnAttributeList) {
                Log.d(TAG + ".fetchVpnAttrList", "VPN attribute list retrieved successfully");
                VpnAttributeList oldVpnAttributeList = vpnAttributeListLiveData.getValue();
                if (oldVpnAttributeList == null || !oldVpnAttributeList.equals(vpnAttributeList)) {
                    Log.d(TAG + ".fetchVpnAttrList", "New VPN attribute list is different from old one, updating LiveData");
                    vpnAttributeListLiveData.postValue(vpnAttributeList);
                    new Thread(() -> {
                        Log.d(TAG + ".fetchVpnAttrList", "Starting thread to update local database with new VPN attribute list");
                        VpnAttributeListEntity vpnAttributeListEntity = new VpnAttributeListEntity(1, VpnAttributeListConverter.fromVpnAttributeList(vpnAttributeList));
                        vpnAttributeListDao.clearAll();
                        vpnAttributeListDao.insertVpnAttributeList(vpnAttributeListEntity);
                        Log.d(TAG + ".fetchVpnAttrList", "Thread finished updating local database with new VPN attribute list");
                    }).start();
                }
                isVpnAttributeListLoading.setValue(false);
                Log.d(TAG + ".fetchVpnAttrList", "Set isVpnAttributeListLoading to false");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG + ".fetchVpnAttrList", "Failed to retrieve VPN attribute list", e);
                isVpnAttributeListLoading.setValue(false);
                Log.d(TAG + ".fetchVpnAttrList", "Set isVpnAttributeListLoading to false");
            }
        });
        Log.d(TAG + ".fetchVpnAttrList", "fetchVpnAttributeList() - end");
    }

    public LiveData<List<Sku>> getSkuAndroidList() {
        return skuAndroidListLiveData;
    }

    private void fetchSkuAndroidList() {
        Log.d(TAG + ".fetchSkuAndrList", "fetchSkuAndroidList() - start");
        if (Boolean.TRUE.equals(isSkuAndroidListLoading.getValue())) {
            Log.d(TAG + ".fetchSkuAndrList", "SKU Android list is already loading, returning");
            return;
        }

        isSkuAndroidListLoading.setValue(true);
        Log.d(TAG + ".fetchSkuAndrList", "Set isSkuAndroidListLoading to true");

        Call<BaseResponse<List<Sku>>> call = jsonWebService.getProductIdentifiersAndroid();
        webService.makeAsyncCall(call, "Failed to get product identifiers", new ShellfireWebService.BaseCallback<List<Sku>>() {
            @Override
            public void onSuccess(List<Sku> skus) {
                Log.d(TAG + ".fetchSkuAndrList", "SKU Android list retrieved successfully: " +skus);

                List<Sku> oldSkus = skuAndroidListLiveData.getValue();
                if (oldSkus == null || !oldSkus.equals(skus)) {
                    Log.d(TAG + ".fetchSkuAndrList", "New SKU Android list is different from old one, updating LiveData to: " + skus);
                    skuAndroidListLiveData.postValue(skus);
                    new Thread(() -> {
                        Log.d(TAG + ".fetchSkuAndrList", "Starting thread to update local database with new SKU Android list");
                        skuDao.clearSkusByType("sub");
                        skuDao.insertSkus(skus);
                        Log.d(TAG + ".fetchSkuAndrList", "Thread finished updating local database with new SKU Android list");
                    }).start();
                }
                isSkuAndroidListLoading.setValue(false);
                Log.d(TAG + ".fetchSkuAndrList", "Set isSkuAndroidListLoading to false");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG + ".fetchSkuAndrList", "Failed to retrieve SKU Android list", e);
                isSkuAndroidListLoading.setValue(false);
                Log.d(TAG + ".fetchSkuAndrList", "Set isSkuAndroidListLoading to false");
            }
        });
        Log.d(TAG + ".fetchSkuAndrList", "fetchSkuAndroidList() - end");
    }

    public LiveData<List<Sku>> getSkuAndroidPassesList() {
        return skuAndroidPassesListLiveData;
    }

    private void fetchSkuAndroidPassesList() {
        Log.d(TAG + ".fetchSkuAndrList", "fetchSkuAndroidPassesList() - start");
        if (Boolean.TRUE.equals(isSkuAndroidPassesListLoading.getValue())) {
            Log.d(TAG + ".fetchSkuAndrList", "SKU Android Passes list is already loading, returning");
            return;
        }

        isSkuAndroidPassesListLoading.setValue(true);
        Log.d(TAG + ".fetchSkuAndrList", "Set isSkuAndroidPassesListLoading to true");

        Call<BaseResponse<List<Sku>>> call = jsonWebService.getProductIdentifiersAndroidPasses();
        webService.makeAsyncCall(call, "Failed to get product identifiers", new ShellfireWebService.BaseCallback<List<Sku>>() {
            @Override
            public void onSuccess(List<Sku> skus) {
                Log.d(TAG + ".fetchSkuAndrList", "SKU Android Passes list retrieved successfully");

                List<Sku> oldSkus = skuAndroidPassesListLiveData.getValue();
                if (oldSkus == null || !oldSkus.equals(skus)) {
                    Log.d(TAG + ".fetchSkuAndrList", "New SKU Android Passes list is different from old one, updating LiveData to: " + skus);
                    skuAndroidPassesListLiveData.postValue(skus);
                    new Thread(() -> {
                        Log.d(TAG + ".fetchSkuAndrList", "Starting thread to update local database with new SKU subscription list");
                        skuDao.clearSkusByType("item");
                        skuDao.insertSkus(skus);
                        Log.d(TAG + ".fetchSkuAndrList", "Thread finished updating local database with new SKU subscription list");
                    }).start();
                }
                isSkuAndroidPassesListLoading.setValue(false);
                Log.d(TAG + ".fetchSkuAndrList", "Set isSkuAndroidPassesListLoading to false");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG + ".fetchSkuAndrList", "Failed to retrieve SKU Android Passes list", e);
                isSkuAndroidPassesListLoading.setValue(false);
                Log.d(TAG + ".fetchSkuAndrList", "Set isSkuAndroidPassesListLoading to false");
            }
        });
        Log.d(TAG + ".fetchSkuAndrList", "fetchSkuAndroidPassesList() - end");
    }

    public LiveData<List<HelpItem>> getHelpItemList() {
        return helpItemListLiveData;
    }

    private void fetchHelpItemList() {
        Log.d(TAG + ".fetchHelpList", "fetchHelpItemList() - start");
        if (Boolean.TRUE.equals(isHelpItemListLoading.getValue())) {
            Log.d(TAG + ".fetchHelpList", "Help item list is already loading, returning");
            return;
        }

        isHelpItemListLoading.setValue(true);
        Log.d(TAG + ".fetchHelpList", "Set isHelpItemListLoading to true");

        Call<BaseResponse<List<HelpItem>>> call = jsonWebService.getAbout();
        webService.makeAsyncCall(call, "Failed to get help items", new ShellfireWebService.BaseCallback<List<HelpItem>>() {
            @Override
            public void onSuccess(List<HelpItem> helpItems) {
                Log.d(TAG + ".fetchHelpList", "Help item list retrieved successfully");
                List<HelpItem> oldHelpItems = helpItemListLiveData.getValue();
                if (oldHelpItems == null || !oldHelpItems.equals(helpItems)) {
                    Log.d(TAG + ".fetchHelpList", "New help item list is different from old one, updating LiveData");
                    helpItemListLiveData.postValue(helpItems);
                    new Thread(() -> {
                        Log.d(TAG + ".fetchHelpList", "Starting thread to update local database with new help item list");
                        helpItemDao.clearAllHelpItems();
                        helpItemDao.insertHelpItems(helpItems);
                        Log.d(TAG + ".fetchHelpList", "Thread finished updating local database with new help item list");
                    }).start();
                }
                isHelpItemListLoading.setValue(false);
                Log.d(TAG + ".fetchHelpList", "Set isHelpItemListLoading to false");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG + ".fetchHelpList", "Failed to retrieve help item list", e);
                isHelpItemListLoading.setValue(false);
                Log.d(TAG + ".fetchHelpList", "Set isHelpItemListLoading to false");
            }
        });
        Log.d(TAG + ".fetchHelpList", "fetchHelpItemList() - end");
    }

    public LiveData<List<Server>> getServerList() {
        return serverListLiveData;
    }

    private void fetchServerList() {
        Log.d(TAG + ".fetchServerList", "fetchServerList() - start");
        if (Boolean.TRUE.equals(isServerListLoading.getValue())) {
            Log.d(TAG + ".fetchServerList", "Server list is already loading, returning");
            return;
        }

        isServerListLoading.setValue(true);
        Log.d(TAG + ".fetchServerList", "Set isServerListLoading to true");

        Call<BaseResponse<List<Server>>> call = jsonWebService.getServerList();
        webService.makeAsyncCall(call, "Failed to get server list", new ShellfireWebService.BaseCallback<List<Server>>() {
            @Override
            public void onSuccess(List<Server> servers) {
                Log.d(TAG + ".fetchServerList", "Server list retrieved successfully: " + servers);
                List<Server> oldServers = serverListLiveData.getValue();
                if (oldServers == null || !oldServers.equals(servers)) {
                    Log.d(TAG + ".fetchServerList", "New server list is different from old one, updating LiveData");
                    serverListLiveData.postValue(servers);
                    new Thread(() -> {
                        Log.d(TAG + ".fetchServerList", "Starting thread to update local database with new server list");
                        serverDao.clearAll();
                        serverDao.insertServers(servers);
                        Log.d(TAG + ".fetchServerList", "Thread finished updating local database with new server list");
                    }).start();
                }
                isServerListLoading.setValue(false);
                Log.d(TAG + ".fetchServerList", "Set isServerListLoading to false");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG + ".fetchServerList", "Failed to retrieve server list", e);
                isServerListLoading.setValue(false);
                Log.d(TAG + ".fetchServerList", "Set isServerListLoading to false");
            }
        });
        Log.d(TAG + ".fetchServerList", "fetchServerList() - end");
    }

    public LiveData<Boolean> isServerListLoading() {
        return isServerListLoading;
    }

    public LiveData<Alias[]> getAliasList() {
        return aliasListLiveData;
    }

    private void fetchAliasList() {
        Log.d(TAG + ".fetchAliasList", "fetchAliasList() - start");
        if (Boolean.TRUE.equals(isAliasListLoading.getValue())) {
            Log.d(TAG + ".fetchAliasList", "Alias list is already loading, returning");
            return;
        }

        isAliasListLoading.setValue(true);
        Log.d(TAG + ".fetchAliasList", "Set isAliasListLoading to true");

        Call<BaseResponse<AliasListContainer>> call = jsonWebService.getWebServiceAliasList();
        webService.makeAsyncCall(call, "Failed to get alias list", new ShellfireWebService.BaseCallback<AliasListContainer>() {
            @Override
            public void onSuccess(AliasListContainer aliases) {
                Log.d(TAG + ".fetchAliasList", "Alias list retrieved successfully: " + aliases);
                Alias[] oldAliases = aliasListLiveData.getValue();
                if (oldAliases == null || !Arrays.equals(oldAliases, aliases.getAliasList())) {
                    Log.d(TAG + ".fetchAliasList", "New alias list is different from old one, updating LiveData");
                    aliasListLiveData.postValue(aliases.getAliasList());
                    new Thread(() -> {
                        Log.d(TAG + ".fetchAliasList", "Starting thread to update local database with new alias list");
                        aliasDao.clearAll();
                        aliasDao.insertAliases(aliases.getAliasList());
                        Log.d(TAG + ".fetchAliasList", "Thread finished updating local database with new alias list");
                    }).start();
                }
                isAliasListLoading.setValue(false);
                Log.d(TAG + ".fetchAliasList", "Set isAliasListLoading to false");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG + ".fetchAliasList", "Failed to retrieve alias list", e);
                isAliasListLoading.setValue(false);
                Log.d(TAG + ".fetchAliasList", "Set isAliasListLoading to false");
            }
        });
        Log.d(TAG + ".fetchAliasList", "fetchAliasList() - end");
    }

    public LiveData<Boolean> isAliasListLoading() {
        return isAliasListLoading;
    }

    public void invalidateCache() {
        Log.d(TAG + ".invalidateCache", "invalidateCache() - start");
        new Thread(() -> {
            Log.d(TAG + ".invalidateCache", "Starting thread to clear local database cache");
            vpnAttributeListDao.clearAll();
            skuDao.clearSkusByType("sub");
            skuDao.clearSkusByType("item");
            helpItemDao.clearAllHelpItems();
            Log.d(TAG + ".invalidateCache", "Thread finished clearing local database cache");
        }).start();
        Log.d(TAG + ".invalidateCache", "invalidateCache() - end");
    }

    public LiveData<Sku> getSku(boolean isSubscription, ServerType accountType, int iBillingPeriod) {
        Log.d(TAG + ".getSku", "getSku(isSubscription=" + isSubscription + ", accountType=" + accountType + ", iBillingPeriod=" + iBillingPeriod + ") - start");
        MediatorLiveData<Sku> result = new MediatorLiveData<>();

        LiveData<List<Sku>> skuAndroidListLiveData = getSkuAndroidList();
        LiveData<List<Sku>> skuAndroidPassesListLiveData = getSkuAndroidPassesList();

        Observer<List<Sku>> observer = skuList -> {
            Log.d(TAG + ".getSku", "SkuList LiveData changed");
            if (skuList != null && !skuList.isEmpty()) {
                Log.d(TAG + ".getSku", "SkuList is not null and not empty, iterating through SKU list. Size of sku list: " + skuList.size());
                for (Sku sku : skuList) {
                    if (sku.isSubscription() == isSubscription && sku.getServerTypeString().equals(accountType.name()) && sku.getBillingPeriod() == iBillingPeriod) {
                        Log.d(TAG + ".getSku", "Matching SKU found, setting LiveData value and removing sources");
                        result.setValue(sku);
                        result.removeSource(skuAndroidListLiveData);
                        result.removeSource(skuAndroidPassesListLiveData);
                        return;
                    }
                }
            }
            Log.d(TAG + ".getSku", "No matching SKU found, setting LiveData value to null");
            result.setValue(null);
        };

        if (isSubscription) {
            result.addSource(skuAndroidListLiveData, observer);
        } else {
            result.addSource(skuAndroidPassesListLiveData, observer);
        }



        Log.d(TAG + ".getSku", "getSku() - end");
        return result;
    }
}
