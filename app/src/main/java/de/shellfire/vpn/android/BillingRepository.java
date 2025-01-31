package de.shellfire.vpn.android;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import de.shellfire.vpn.android.webservice.JsonWebService;
import de.shellfire.vpn.android.webservice.ShellfireWebService;
import de.shellfire.vpn.android.webservice.WebService;
import de.shellfire.vpn.android.webservice.model.BaseResponse;
import de.shellfire.vpn.android.webservice.model.UpgradeVpnRequest;
import de.shellfire.vpn.android.webservice.model.UpgradeVpnResponse;
import retrofit2.Call;

public class BillingRepository {
    private static BillingRepository instance;
    private final ShellfireWebService webService;
    private final JsonWebService jsonWebService;
    private VpnRepository vpnRepository;

    private BillingRepository(Context context) {
        webService = ShellfireWebService.getInstance(context);
        jsonWebService = WebService.getInstance(context).getJsonWebService();
        vpnRepository = VpnRepository.getInstance(context);
    }

    public static synchronized BillingRepository getInstance(Context context) {
        if (instance == null) {
            instance = new BillingRepository(context.getApplicationContext());
        }
        return instance;
    }

    public LiveData<UpgradeVpnResponse> upgradeToPremiumAndroid(int vpnId, String signedData, String signature) {
        MutableLiveData<UpgradeVpnResponse> upgradeResultLiveData = new MutableLiveData<>();

        UpgradeVpnRequest request = new UpgradeVpnRequest();
        request.setProductId(vpnId);
        request.setSignedData(signedData);
        request.setSignature(signature);

        Call<BaseResponse<UpgradeVpnResponse>> call = jsonWebService.upgradeToPremiumAndroid(request);
        webService.makeAsyncCall(call, "Failed to upgrade VPN to premium with Google Play purchase", new ShellfireWebService.BaseCallback<UpgradeVpnResponse>() {
            @Override
            public void onSuccess(UpgradeVpnResponse response) {
                upgradeResultLiveData.setValue(response);
            }

            @Override
            public void onFailure(Exception e) {
                // Handle error
                upgradeResultLiveData.postValue(null);
            }
        });

        return upgradeResultLiveData;
    }


}
