package de.shellfire.vpn.android;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class ServerFilterHelper {
    private final VpnRepository vpnRepository;
    private final String TAG = "ServerFilterHelper";

    public ServerFilterHelper(VpnRepository vpnRepository) {
        this.vpnRepository = vpnRepository;
    }

    public LiveData<List<Server>> getFilteredServers(LiveData<List<Server>> serversLiveData, boolean isPressedOne, boolean isPressedTwo,
                                                     boolean isPressedThree, int load, boolean isAdvancedList, String searchText) {

        Log.d(TAG, "getFilteredServers: isPressedOne: " + isPressedOne + " isPressedTwo: " + isPressedTwo + " isPressedThree: " + isPressedThree + " load: " + load + " isAdvancedList: " + isAdvancedList + " searchText: " + searchText);
        MediatorLiveData<List<Server>> resultLiveData = new MediatorLiveData<>();

        Observer<List<Server>> serverObserver = new Observer<>() {
            @Override
            public void onChanged(List<Server> servers) {
                Vpn vpn = vpnRepository.getSelectedVpn().getValue();
                if (vpn == null || servers == null || servers.isEmpty()) {
                    resultLiveData.setValue(new ArrayList<>());
                    return;
                }

                // Step 1: Filter out multiple servers per country based on VPN level, only when in standard mode
                List<Server> bestServers = servers;
                if (!isAdvancedList) {
                    bestServers = ServerFilterHelper.this.getBestServers(servers, vpn.getAccountType());
                }

                // Step 2: Filter by rating and load, only if in advanced mode
                List<Server> filteredByRatingAndLoad = bestServers;
                ;
                if (isAdvancedList) {
                    filteredByRatingAndLoad = ServerFilterHelper.this.filterByRatingAndLoad(bestServers, isPressedOne, isPressedTwo, isPressedThree, load, isAdvancedList);
                }

                // Step 3: Filter by search text
                List<Server> finalFilteredList = ServerFilterHelper.this.filterBySearchText(filteredByRatingAndLoad, searchText);

                // Step 4: Sort list by Country PrintName
                Collections.sort(finalFilteredList, new Comparator<Server>() {
                    @Override
                    public int compare(Server server1, Server server2) {
                        if (server1.getCountryPrint() != null && server2.getCountryPrint() != null) {
                            return server1.getCountryPrint().compareTo(server2.getCountryPrint());
                        } else {
                            return 0;
                        }
                    }
                });

                serversLiveData.removeObserver(this);
                resultLiveData.setValue(finalFilteredList);
            }
        };

        serversLiveData.observeForever(serverObserver);

        return resultLiveData;
    }

    private List<Server> getBestServers(List<Server> servers, ServerType vpnLevel) {
        HashMap<Country, Server> bestServerByCountry = new HashMap<>();

        for (Server server : servers) {
            // If no server for this country used yet, use this server,
            // or
            //      if a server already exists in the hashmap
            //          and the existing servers level is above the vpn level,
            //          and the servers level is lower or equal to the vpns level
            //          then use the server instead
            Server existingServer = bestServerByCountry.get(server.getCountryEnum());
            if (existingServer == null) {
                bestServerByCountry.put(server.getCountryEnum(), server);
            } else {
                // We already have a server for this country

                if (server.getServerType().ordinal() <= vpnLevel.ordinal()) {
                    // New server can be used by VPN - check if we should use it.

                    // scenario 1: its level is higher than the existing ones (e.g. we have a Free server and can use a Premium server)
                    if (existingServer.getServerType().ordinal() < server.getServerType().ordinal()) {
                        bestServerByCountry.put(server.getCountryEnum(), server);
                    }

                    // scenario 2: the existing server's level is higher than the vpn's, e.g. the vpn is not eligible to use the existing server
                    if (existingServer.getServerType().ordinal() > vpnLevel.ordinal()) {
                        bestServerByCountry.put(server.getCountryEnum(), server);
                    }
                }
            }
        }

        List<Server> bestServers = new ArrayList<>();
        bestServers.addAll(bestServerByCountry.values());

        return bestServers;
    }

    private List<Server> filterByRatingAndLoad(List<Server> servers, boolean isPressedOne, boolean isPressedTwo, boolean isPressedThree, int load, boolean isAdvancedList) {
        if (!isAdvancedList) {
            return servers;
        }

        List<Server> filteredList = new ArrayList<>();
        for (Server server : servers) {
            if ((isPressedOne && server.getServerType() == ServerType.Free) ||
                    (isPressedTwo && server.getServerType() == ServerType.Premium) ||
                    (isPressedThree && server.getServerType() == ServerType.PremiumPlus) ||
                    (!isPressedOne && !isPressedTwo && !isPressedThree)) {
                if (server.getLoadPercentage() <= load) {
                    filteredList.add(server);
                }
            }
        }

        return filteredList;
    }

    private List<Server> filterBySearchText(List<Server> servers, String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return servers;
        }

        List<Server> filteredList = new ArrayList<>();
        String lowerCaseSearchText = searchText.toLowerCase();

        for (Server server : servers) {
            if (server.getCountryPrint().toLowerCase().contains(lowerCaseSearchText) ||
                    (server.getCity() != null && server.getCity().toLowerCase().contains(lowerCaseSearchText))) {
                filteredList.add(server);
            }
        }

        return filteredList;
    }
}
