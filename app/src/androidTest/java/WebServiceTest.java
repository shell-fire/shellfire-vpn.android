import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.test.core.app.ApplicationProvider;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import de.shellfire.vpn.android.DataRepository;
import de.shellfire.vpn.android.Server;
import de.shellfire.vpn.android.ShellfireApplication;
import de.shellfire.vpn.android.Vpn;
import de.shellfire.vpn.android.VpnRepository;
import de.shellfire.vpn.android.auth.AuthRepository;
import de.shellfire.vpn.android.auth.LoginStatus;
import de.shellfire.vpn.android.model.Alias;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WebServiceTest {

	@Mock
	private static Context mockContext;

	@BeforeClass
	public static void setUpOnce() throws Exception {
		System.setProperty("test.env", "true");
		System.out.println("starting up...");

		// Initialize MockContext if not already initialized
		mockContext =  ApplicationProvider.getApplicationContext();


		if (mockContext == null) {
			throw new Exception("MockContext is null");
		}


		AuthRepository authRepository = AuthRepository.getInstance(mockContext);
		authRepository.login("fgattung+1716236934@gmail.com", "the_passw0rT");
		LiveData<LoginStatus> resultLiveData = authRepository.getLoginStatus();
		CountDownLatch latch = new CountDownLatch(1);

		// Ensure observeForever runs on the main thread
		Handler handler = new Handler(Looper.getMainLooper());
		handler.post(() -> {
			resultLiveData.observeForever(new Observer<>() {
				@Override
				public void onChanged(LoginStatus result) {
					System.out.println("loggedIn: " + (result != null));
					if (result != null) {
						System.out.println("token: " + result);
					}
					latch.countDown();
				}
			});
		});

		// Wait for the login to complete or timeout after 10 seconds
		if (!latch.await(10, TimeUnit.SECONDS)) {
			throw new AssertionError("Login did not complete in the expected time");
		}

		ShellfireApplication.setIsTestMode(true);
	}

	@Test
	public void test01_GetServerList() throws Exception {
		System.out.println("testGetServerList");
		assertTrue(mockContext != null);
		DataRepository dataRepository = DataRepository.getInstance(mockContext);
		LiveData<List<Server>> resultLiveData = dataRepository.getServerList();
		CountDownLatch latch = new CountDownLatch(1);

		// Ensure observeForever runs on the main thread
		Handler handler = new Handler(Looper.getMainLooper());
		handler.post(() -> {
			resultLiveData.observeForever(result -> {
				assertTrue(result != null);
				assertTrue(result.size() > 10);
				System.out.println("Num servers: " + result.size());
				for (Server server : result) {
					System.out.println("Server: " + server.toString());
				}
				latch.countDown();
			});
		});

		// Wait for the getServerList to complete or timeout after 10 seconds
		if (!latch.await(10, TimeUnit.SECONDS)) {
			throw new AssertionError("GetServerList did not complete in the expected time");
		}



	}


	@Test
	public void test02_GetAllVpnDetails() throws Exception {
		System.out.println("testGetAllVpnDetails");
		VpnRepository vpnRepository = VpnRepository.getInstance(mockContext);
		LiveData<List<Vpn>> resultLiveData = vpnRepository.getVpnList();

		CountDownLatch latch = new CountDownLatch(1);

		// Ensure observeForever runs on the main thread
		Handler handler = new Handler(Looper.getMainLooper());
		handler.post(() -> {
			resultLiveData.observeForever(result -> {
				assertTrue(result != null);
				assertTrue(result.size() >= 1);
				System.out.println("Num VPNs: " + result.size());
				for (Vpn vpn : result) {
					System.out.println("VPN: " + vpn.getName());
				}
				vpnRepository.setSelectedVpn(result.get(0).getVpnId());
				latch.countDown();
			});
		});

		// Wait for the getServerList to complete or timeout after 10 seconds
		if (!latch.await(10, TimeUnit.SECONDS)) {
			throw new AssertionError("GetServerList did not complete in the expected time");
		}




	}



	@Test
	public void test16_GetWebServiceAlias() throws Exception {
		System.out.println("test16_GetWebServiceAlias");
		DataRepository dataRepository = DataRepository.getInstance(mockContext);
		LiveData<Alias[]> resultLiveData = dataRepository.getAliasList();

		CountDownLatch latch = new CountDownLatch(1);

		// Ensure observeForever runs on the main thread
		Handler handler = new Handler(Looper.getMainLooper());
		handler.post(() -> {
			resultLiveData.observeForever(result -> {
				assertTrue(result != null);
				assertTrue(result.length >= 1);
				System.out.println("Aliases: " + result);

				latch.countDown();
			});
		});

		// Wait for the getServerList to complete or timeout after 10 seconds
		if (!latch.await(10, TimeUnit.SECONDS)) {
			throw new AssertionError("GetServerList did not complete in the expected time");
		}




	}


	/*
	@Test
	public void test03_GetCertificatesForOpenVpn() throws Exception {
		System.out.println("testGetCertificatesForOpenVpn");
		if (service.getSelectedVpn() == null && service.getAllVpnDetails(false) != null && service.getAllVpnDetails(false).size() > 0) {
			// todo: maybe remove again or apply
			service.setSelectedVpn(service.getAllVpnDetails(false).get(0).getVpnId());
		}

		List<WsFile> result = service.getCertificates();
		assertTrue(result != null);
		assertTrue(result.size() > 1);
		System.out.println("Num files: " + result.size());
		for (WsFile file : result) {
			System.out.println("Name: " + file.getName());
			System.out.println("Content: " + file.getContent());
		}
	}

	@Test
	public void test04_GetParametersForOpenVpn() throws Exception {
		System.out.println("Starting test04_GetParametersForOpenVpn");
		if (service.getSelectedVpn() == null && service.getAllVpnDetails(false) != null && service.getAllVpnDetails(false).size() > 0) {
			// todo: maybe remove again or apply
			service.setSelectedVpn(service.getAllVpnDetails(false).get(0).getVpnId());
		}

		String parameters = service.getParametersForOpenVpn();

		assertTrue(parameters != null);
		System.out.println("Parameters: " + parameters);
	}

	@Test
	public void test05_SetServerTo() throws Exception {
		System.out.println("Starting test05_SetServerTo");
		if (service.getSelectedVpn() == null && service.getAllVpnDetails(false) != null && service.getAllVpnDetails(false).size() > 0) {
			service.setSelectedVpn(service.getAllVpnDetails(false).get(0).getVpnId());
		}

		boolean success = service.setServerTo(service.getServerById(30));
		assertTrue(success);
		System.out.println("Change server to 30  - Success: " + (success ? "true" : "false"));

		boolean success2 = service.setServerTo(service.getServerById(4));
		assertTrue(success2);
		System.out.println("Change server to 30  - Success: " + (success2 ? "true" : "false"));

		Server vpnServer = new Server();
		try {
			vpnServer.setVpnServerId(41111);
			boolean success3 = service.setServerTo(vpnServer);
			// above should always throw exception
			assertTrue(false);
		} catch (RuntimeException e) {
			assertTrue(true);
		}

	}

	@Test
	public void test06_SetProtocol() throws Exception {
		System.out.println("Starting test06_SetProtocol");
		if (service.getSelectedVpn() == null && service.getAllVpnDetails(false) != null && service.getAllVpnDetails(false).size() > 0) {
			// todo: maybe remove again or apply
			service.setSelectedVpn(service.getAllVpnDetails(false).get(0).getVpnId());
		}

		boolean success = service.setProtocol("TCP");
		assertTrue(success);
		System.out.println("Changeto protocol to TCP  - Success: " + (success ? "true" : "false"));

		boolean success2 = service.setProtocol("UDP");
		assertTrue(success2);
		System.out.println("Change protocol to UDP  - Success: " + (success2 ? "true" : "false"));

		Server vpnServer = new Server();
		try {
			boolean success3 = service.setProtocol("FOOBAR");
			// should never be reached
			assertTrue(false);

		} catch (RuntimeException e) {
			assertTrue(true);
		}

	}

	@Test
	public void test07_GetActivationStatus() throws Exception {
		System.out.println("Starting test07_GetActivationStatus");


		boolean active = service.getActivationStatusActive();
		assertTrue(active);

	}

	@Test
	public void test08_VerifyMarketInAppBillingPurchase() throws Exception {
		System.out.println("Starting test08_VerifyMarketInAppBillingPurchase");


		String signature = "foo";
		String signedData = "bar";

		try {
			boolean verified = service.verifyMarketInAppBillingPurchase(signedData, signature);
			// should never be reached
			assertTrue(false);

		} catch (RuntimeException e) {
			assertTrue(true);
		}

		String signedData2 = "{\"orderId\":\"GPA.3317-8517-2108-39269\",\"packageName\":\"de.shellfire.vpn.android\",\"productId\":\"item_day_premium_plus\",\"purchaseTime\":1716218932282,\"purchaseState\":0,\"developerPayload\":\"611613\",\"purchaseToken\":\"canmfgomjcdejmndhekpgdkk.AO-J1OwaMCT4q3g-7Vms58fLkQKE8FjCs7dpmUQqpuepbCDXIA2sWdJjGzjrgVSH5j7uzjYWJGAR1nL-KMlHvHlp3v3xVo4QkR9Xn5-GxPLZ1l4n7JAHPA0\"}";
		String signature2 = "L6LiaDEAvfIteoen02+f1bdXg5/m6S5lfTgku0FkMWi3BenfkLLceamsnyJR29Q3RE2XVUKeOOaZpYZWgnZ2q+DyvUrobGHtlcv+OUuuw+nzz74JUV7GMuzGZ6o+o7Ljb0VqmAQku2ySPtSW6IPJRLNLqnCU/HlOO6X9ELomTMSVW5MczcvOvQAKAuKvEl7MnhaiV97l2eQTPE8fSITwxB8LAq8XJhSnY+eAr2D6xhBJMT572pMWkZGMNawPjYOLzJOeun20tFEE0oLcwCxIjfRa5Ucdq3lpV9rLPIkBuXQFcXCTfz7fciZ8RxF+D6LOUSYhx23ydkoqfiZRZK16Jw==";

		boolean verified2 = service.verifyMarketInAppBillingPurchase(signedData2, signature2);
		// should never be reached
		assertTrue(verified2);
	}

	@Test
	public void test09_Register() throws Exception {
		System.out.println("Starting test09_Register");

		String email = "fgattung@gmail.com";
		String password = "the_passw0rT";
		int newsletter = 0;
		int resend = 0;

		try {
			RegisterResponse registerResponse = service.register(email, password, newsletter, resend);
			// should never be reached
			assertTrue(false);

		} catch (RuntimeException e) {
			// account already exists, expecting exception
			assertTrue(true);
		}

		String email2 = "fgattung+" + Instant.now().getEpochSecond() + "@gmail.com";

		String password2 = "the_passw0rT";
		int newsletter2 = 0;
		int resend2 = 0;

		RegisterResponse registerResponse2 = service.register(email2, password2, newsletter2, resend2);
		assertTrue(registerResponse2.isSuccess());
		assertTrue(registerResponse2.getToken() != null);
	}

	@Test
	public void test10_getComparisonTable() throws Exception {
		System.out.println("Starting test10_getComparisonTable");

		VpnAttributeList response = service.getComparisonTable();
		System.out.println(response);
		assertTrue(response != null);
	}

	@Test
	public void test11_getProductIdentifiersAndroid() throws Exception {
		System.out.println("Starting test11_getProductIdentifiersAndroid");

		List<ProductIdentifiersResponse.WsSku> response = service.getProductIdentifiersAndroid();
		System.out.println(response);
		assertTrue(response != null);
	}

	@Test
	public void test12_getProductIdentifiersAndroidPasses() throws Exception {
		System.out.println("Starting test12_getProductIdentifiersAndroidPasses");

		List<ProductIdentifiersResponse.WsSku> response = service.getProductIdentifiersAndroidPasses();
		System.out.println(response);
		assertTrue(response != null);
	}

	@Test
	public void test12_getDeveloperPayload() throws Exception {
		System.out.println("Starting test12_getDeveloperPayload");

		String response = service.getDeveloperPayload();
		System.out.println(response);
		assertTrue(response != null);
	}

	@Test
	public void test13_sendLog() throws Exception {
		System.out.println("Starting test13_sendLog");

		String logText1 = "this the text to send via sendLog :-)\r\nautomated test case 13 part 1...";
		String logText2 = "Here is additional text to send via sendLog :-)\r\nautomated test case 13 part 2...";

		// Create BufferedReaders from the log texts
		StringReader logReader1 = new StringReader(logText1);
		StringReader logReader2 = new StringReader(logText2);

		// Call the sendLogToShellfire method with the LogRequestBody
		boolean result = service.sendLog(logReader1); // , logReader2);

		// Assert the result (assuming sendLogToShellfire returns a boolean indicating success)
		assertTrue("sendLogToShellfire should return true", result);
	}

	@Test
	public void test14_getAbout() throws Exception {
		System.out.println("Starting test14_getAbout");


		List<WsHelpItem> about = service.getAbout();
		System.out.println(about);
	}

	@Test
	public void test15_upgradeVpn() throws Exception {
		System.out.println("Starting test15_upgradeVpn");

		if (service.getSelectedVpn() == null && service.getAllVpnDetails(false) != null && service.getAllVpnDetails(false).size() > 0) {
			service.setSelectedVpn(service.getAllVpnDetails(false).get(0).getVpnId());
		}

		String signedData = "FOO";
		String signature = "BAR";
		try {
			service.upgradeToPremiumAndroid(signedData, signature);
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(true);
		}

		//String signedData2 = "{\"orderId\":\"GPA.3325-6569-7464-49939\",\"packageName\":\"de.shellfire.vpn.android\",\"productId\":\"item_day_premium_plus\",\"purchaseTime\":1716325533104,\"purchaseState\":0,\"developerPayload\":\"611793\",\"purchaseToken\":\"blnclpjmngkigancegnhgdbe.AO-J1OwkJdnJNGABT6aA77eKJrjNlFREcpS5N4J23L5J2y7VN2rPxnhIbqy11SgcztG2FyMSlcbPZy3ie_R3GTGx1yMao7fnLDFp2A12d_NRIngBx3z0xew\"}";
		//String signature2 = "ml+NpyuODmKAnWQfpvhMbrPks3tdY8bwgIPFOuufUY4h4FpKl7/nAjyp1NBlQ21FAv43HhnJFWp1AAgD+gL4sed1NGv7cEA0dfkw9Q1wVqEyaVkF24SpKGh6w9is+X6YxuHYKm6k+4wf7T6owy6NhsJrJmKGg4QtemRPWNEklsy/MU47X59mfQq2sE3hIRD+K7QYplUeVWyy1xLX8ymRIGidgdeVlfYLBL9BFLjPIY40TBuyVzuYApJNZe3c0Mh9LwSlD8JjOQUHJCbvtfVqr6+EgtNJSeBR1QADALBVGJ0N0xvoEXCf7WrlHiAdfckC7TUCilSERgklkh4RflFEFw==";
		String signedData2 = "{\"orderId\":\"GPA.3317-8517-2108-39269\",\"packageName\":\"de.shellfire.vpn.android\",\"productId\":\"item_day_premium_plus\",\"purchaseTime\":1716218932282,\"purchaseState\":0,\"developerPayload\":\"611613\",\"purchaseToken\":\"canmfgomjcdejmndhekpgdkk.AO-J1OwaMCT4q3g-7Vms58fLkQKE8FjCs7dpmUQqpuepbCDXIA2sWdJjGzjrgVSH5j7uzjYWJGAR1nL-KMlHvHlp3v3xVo4QkR9Xn5-GxPLZ1l4n7JAHPA0\"}";
		String signature2 = "L6LiaDEAvfIteoen02+f1bdXg5/m6S5lfTgku0FkMWi3BenfkLLceamsnyJR29Q3RE2XVUKeOOaZpYZWgnZ2q+DyvUrobGHtlcv+OUuuw+nzz74JUV7GMuzGZ6o+o7Ljb0VqmAQku2ySPtSW6IPJRLNLqnCU/HlOO6X9ELomTMSVW5MczcvOvQAKAuKvEl7MnhaiV97l2eQTPE8fSITwxB8LAq8XJhSnY+eAr2D6xhBJMT572pMWkZGMNawPjYOLzJOeun20tFEE0oLcwCxIjfRa5Ucdq3lpV9rLPIkBuXQFcXCTfz7fciZ8RxF+D6LOUSYhx23ydkoqfiZRZK16Jw==";
		try {
			service.upgradeToPremiumAndroid(signedData2, signature2);
		} catch (Exception e) {
			assertTrue(true);
		}

	}

	 */
}
