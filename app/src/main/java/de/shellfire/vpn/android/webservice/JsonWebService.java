package de.shellfire.vpn.android.webservice;

import java.util.List;
import java.util.Map;

import de.shellfire.vpn.android.HelpItem;
import de.shellfire.vpn.android.Server;
import de.shellfire.vpn.android.Sku;
import de.shellfire.vpn.android.Vpn;
import de.shellfire.vpn.android.webservice.model.AliasListContainer;
import de.shellfire.vpn.android.webservice.model.BaseResponse;
import de.shellfire.vpn.android.webservice.model.GetActivationStatusResponse;
import de.shellfire.vpn.android.webservice.model.LoginRequest;
import de.shellfire.vpn.android.webservice.model.LoginResponse;
import de.shellfire.vpn.android.webservice.model.ProductIdRequest;
import de.shellfire.vpn.android.webservice.model.RegisterRequest;
import de.shellfire.vpn.android.webservice.model.RegisterWithGoogleRequest;
import de.shellfire.vpn.android.webservice.model.SetProtocolRequest;
import de.shellfire.vpn.android.webservice.model.SetServerToRequest;
import de.shellfire.vpn.android.webservice.model.StringResponse;
import de.shellfire.vpn.android.webservice.model.UpgradeVpnRequest;
import de.shellfire.vpn.android.webservice.model.UpgradeVpnResponse;
import de.shellfire.vpn.android.webservice.model.VerifyMarketInAppBillingPurchaseRequest;
import de.shellfire.vpn.android.webservice.model.VpnAttributeList;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface JsonWebService {

    // Static Data Loaders
    @POST("json.php?action=getComparisonTable")
    Call<BaseResponse<VpnAttributeList>> getComparisonTable();

    @POST("json.php?action=getProductIdentifiersAndroid")
    Call<BaseResponse<List<Sku>>> getProductIdentifiersAndroid();

    @POST("json.php?action=getProductIdentifiersAndroidPasses")
    Call<BaseResponse<List<Sku>>> getProductIdentifiersAndroidPasses();

    @POST("json.php?action=getAbout")
    Call<BaseResponse<List<HelpItem>>> getAbout();

    @POST("json.php?action=getWebServiceAliasList")
    Call<BaseResponse<AliasListContainer>> getWebServiceAliasList();


    // Account Management
    @POST("json.php?action=login")
    Call<BaseResponse<LoginResponse>> login(@Body LoginRequest request);

    // Account Management
    @POST("json.php?action=logout")
    Call<BaseResponse<Void>> logout();

    @POST("json.php?action=register")
    Call<BaseResponse<LoginResponse>> register(@Body RegisterRequest request);

    @POST("json.php?action=registerWithGoogleToken")
    Call<BaseResponse<LoginResponse>> registerWithGoogleToken(@Body RegisterWithGoogleRequest request);

    @POST("json.php?action=getActivationStatus")
    Call<BaseResponse<GetActivationStatusResponse>> getActivationStatus();

    // VPN Logic
    @POST("json.php?action=getServerList")
    Call<BaseResponse<List<Server>>> getServerList();

    @POST("json.php?action=getAllVpnDetails")
    Call<BaseResponse<List<Vpn>>> getAllVpnDetails();

    @POST("json.php?action=getOpenVpnParams")
    Call<BaseResponse<Map<String, String>>> getOpenVpnParams(@Body ProductIdRequest request);

    @POST("json.php?action=getCertificates")
    Call<BaseResponse<Map<String, String>>> getCertificates(@Body ProductIdRequest request);

    @POST("json.php?action=setServerTo")
    Call<BaseResponse<Void>> setServerTo(@Body SetServerToRequest request);

    @POST("json.php?action=setProtocol")
    Call<BaseResponse<Void>> setProtocol(@Body SetProtocolRequest request);

    @POST("json.php?action=verifyMarketInAppBillingPurchase")
    Call<BaseResponse<Void>> verifyMarketInAppBillingPurchase(@Body VerifyMarketInAppBillingPurchaseRequest request);

    @POST("json.php?action=sendLog")
    Call<BaseResponse<Void>> sendLog(@Body LogRequestBody request);

    @POST("json.php?action=upgradeToPremiumAndroid")
    Call<BaseResponse<UpgradeVpnResponse>> upgradeToPremiumAndroid(@Body UpgradeVpnRequest request);

    @POST("json.php?action=getObfuscatedAccountId")
    Call<BaseResponse<StringResponse>> getObfuscatedAccountId();

}
