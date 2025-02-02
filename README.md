# ShellfireVPN-Android

## 🚀 Overview

ShellfireVPN-Android is an open-source Android application that provides a secure and private VPN connection. This VPN client can only be used to connect to the **Shellfire VPN network**. This repository contains the latest clean version with separate build flavors:
- **googlePlay:** Includes proprietary extensions (e.g., Google Play Services, Firebase, etc.) for the Play Store.
- **fdroid:** A fully free, open-source build with proprietary dependencies removed or stubbed for F-Droid compatibility.

Repository Location: [GitHub Repository](https://github.com/shell-fire/shellfire-vpn.android/tree/main/app)

## 📌 Features

- Secure VPN connection with encryption
- Open-source and privacy-focused
- Can only be used to connect to the **Shellfire VPN network**
- Two build flavors:
  - **googlePlay:** For distribution on the Google Play Store.
  - **fdroid:** For inclusion in the F-Droid repository (proprietary components removed).
- Uses modern Gradle build flavors and stubs to separate proprietary dependencies

## 👥 Installation

You can install ShellfireVPN-Android in the following ways:

### **1. From Google Play**

- Download the latest version from the Google Play Store: [ShellfireVPN on Google Play](https://play.google.com/store/apps/details?id=de.shellfire.vpn.android)

### **2. From F-Droid**

- The F-Droid variant is fully free of proprietary dependencies.  
- It will be available on F-Droid soon. (Make sure to check the F-Droid repository once approved.)

### **3. Build from Source**

You can manually build the app using Android Studio. There are two available flavors:

#### **Prerequisites:**
- Android Studio (latest version)
- Java JDK 11+
- Android SDK (with required components)
- Gradle (the build system is free and open source)

#### **Build Instructions:**

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/shell-fire/shellfire-vpn.android.git
   cd shellfire-vpn.android
   ```

2. **Select the Build Flavor:**

   - **For F-Droid build (fully FLOSS):**
     - In Android Studio, open the **Build Variants** panel.
     - Under **Active Build Variant** for the app module, select **fdroidDebug** (or **fdroidRelease**).
     - Or build from the command line:
       ```bash
       ./gradlew assembleFdroidRelease
       ```
     - The resulting APK will have the package name `de.shellfire.vpn.android.fdroid`.

   - **For Google Play build (with proprietary components):**
     - In the Build Variants panel, select **googlePlayDebug** (or **googlePlayRelease**).
     - Or build via:
       ```bash
       ./gradlew assembleGooglePlayRelease
       ```
     - The resulting APK will have the package name `de.shellfire.vpn.android.gp`.

3. **Signing and Local Properties:**
   - Create a file named `local.properties` in the project root with your signing credentials:
     ```
     storeFile=C:\path\to\your\keystore.jks
     storePassword=YOUR_STORE_PASSWORD
     keyAlias=YOUR_KEY_ALIAS
     keyPassword=YOUR_KEY_PASSWORD
     ```

## 🔐 Secure Signing Setup

To ensure security, the signing keys are **not included** in this repository. You must configure your own signing credentials:

1. **Create `local.properties` file in the root directory:**
   ```
   storeFile=C:\Users\YOUR_USER\keystore.jks
   storePassword=YOUR_PASSWORD
   keyAlias=YOUR_ALIAS
   keyPassword=YOUR_KEY_PASSWORD
   ```
2. The app will automatically use these credentials when building **locally**.

## 🔒 Privacy & Security

We prioritize privacy. The F-Droid build (fdroid flavor):
- **Removes proprietary dependencies:** Google Play Services, Firebase Crashlytics, etc. are excluded or replaced with stubs.
- Uses only FLOSS libraries and build tools.
- Will be fully reproducible with FLOSS toolchains.

The Google Play build includes some proprietary extensions, but we maintain a separate FDroid branch that complies with F-Droid’s policies.

## 🐝 Dependencies & Acknowledgements

ShellfireVPN-Android builds on:
- **OpenVPN-ics** (GPL v2) by Arne Schwabe
- **Google Play Services** and other proprietary components (only in the googlePlay flavor)
- **Stubs** are provided for proprietary libraries in the FDroid build

Special thanks to Arne Schwabe for his contributions to the open-source VPN ecosystem.

## 🛠 Contributing

We welcome contributions! To contribute:
1. Fork the repository.
2. Create a new branch (`git checkout -b feature-branch`).
3. Make changes and commit (`git commit -m "Add new feature"`).
4. Push to your fork (`git push origin feature-branch`).
5. Open a Pull Request.

## 📝 License

This project is licensed under **GPL v2** due to its dependence on OpenVPN-ics. See the `LICENSE` file for details.

## 💌 Contact & Support

For questions or support:
- **GitHub Issues:** Report bugs or request features.
- **Email:** [support@shellfire.net](mailto:support@shellfire.net)

---

*⭐ If you like this project, please give it a star on GitHub! ⭐*

