# ShellfireVPN-Android

## 🚀 Overview
ShellfireVPN-Android is an open-source Android application that provides a secure and private VPN connection. This repository contains the latest **clean** version, though it still includes proprietary extensions. The changes for F-Droid compatibility will be implemented later.

Repository Location: [GitHub Repository](https://github.com/shell-fire/shellfire-vpn.android/tree/main/app)

## 📌 Features
- Secure VPN connection with encryption
- Open-source and privacy-focused
- Currently includes proprietary extensions
- F-Droid compatibility planned for the future
- Optimized for Android devices

## 📥 Installation
You can install ShellfireVPN-Android in the following ways:

### **1. From Google Play**
- Download the latest version from the Google Play Store: [ShellfireVPN on Google Play](https://play.google.com/store/apps/details?id=de.shellfire.vpn.android)

### **2. From F-Droid (Coming Soon)**
- The F-Droid version is free from proprietary dependencies.
- Link to F-Droid will be added soon.

### **3. Build from Source**
You can manually build the app using Android Studio:

#### **Prerequisites:**
- Android Studio (latest version)
- Java JDK 11+
- Android SDK with necessary dependencies

#### **Build Instructions:**
```bash
# Clone the repository
git clone https://github.com/shell-fire/shellfire-vpn.android.git
cd shellfire-vpn.android

# Open in Android Studio and build
```

## 🔑 Secure Signing Setup
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
We prioritize privacy. The app:
- Currently includes proprietary extensions.
- Will later be modified to exclude proprietary dependencies for F-Droid.
- Does **not** use proprietary analytics (Firebase, Google Analytics, etc.).
- Is fully open-source and auditable.

## 🛠 Contributing
We welcome contributions! To contribute:

1. Fork the repository
2. Create a new branch (`git checkout -b feature-branch`)
3. Make changes and commit (`git commit -m "Add new feature"`)
4. Push to your fork (`git push origin feature-branch`)
5. Open a Pull Request

## 📝 License
This project is licensed under the **Apache 2.0 License**. See the `LICENSE` file for details.

## 📬 Contact & Support
For questions, suggestions, or support, reach out via:
- **GitHub Issues** (Bug reports, feature requests)
- **Email:** support@shellfirevpn.com

---

_⭐ If you like this project, please consider giving it a star on GitHub! ⭐_

