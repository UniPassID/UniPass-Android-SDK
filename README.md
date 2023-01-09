UniPassWallet SDK for Android

This library allows you to integrate UniPass Wallet into your Android app.

| Version | Last updated   | UniPass Wallet Entry URL        | UPgrade Instruction                                                                    |
| -------- | ---------- | ---------------------------------- | -------------------------------------------------------------------------------------- |
| v0.0.5   | 2023.01.09 | https://testnet.wallet.unipass.id/ | Add UniPassActivity , remove requirements for singleTop launchMode . UniPassSDKOptions adds parameter: activity |
| v0.0.3   | 2023.01.04 | https://testnet.wallet.unipass.id/ | Add Connect / Transfer / Sign Message / Logout support                                 |

# Quick Start

You can have a look at the [examples](https://github.com/UniPassID/UniPass-Android-SDK/tree/main/app) made on top of this SDK and try it out yourself.

### Android demo

You can download Android demo apk from Google drive:

- [unipass_android_demo_v0.0.3.apk](https://drive.google.com/file/d/1U1AnEpxHjZmroz-03veZ8Q0-h7aqaR87/view?usp=sharing)

:::tip
The UniPass Wallet domain used in Demo project is: [https://testnet.wallet.unipass.id/](https://testnet.wallet.unipass.id/login)
:::

## Requirements

Android `API version 21` or newer

## Installation

1. In your project-level `build.gradle` or `settings.gradle` file, add JitPack repository:

```
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }  // Add this line
  }
}
```

2. In your app-level `build.gradle` dependencies section, add the following:

```
dependencies {
    // Replace {Version} with the version you what. For example v0.0.5
    implementation 'com.github.UniPassID:UniPass-Android-SDK:{Version}'
}
```

3. Add UniPassActivity / Configure Deep Link

Open your app's AndroidManifest.xml file and add UniPassActivity activity in your application, add deep link intent filter to UniPassActivity activity: 
You can refer to the [android developer documents](https://developer.android.com/training/app-links/deep-linking)

```xml
<activity
    android:name="com.unipass.core.UniPassActivity"
    android:exported="true"
    android:launchMode="singleTop">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="unipassapp"
            android:host="com.unipass.wallet"
            android:pathPattern="/*"
            android:pathPrefix="/redirect" />
    </intent-filter>
</activity>
```

If you are using v0.0.3 or below, you can add just deep link intent to your activity, but you should make sure your activity launchMode is set to singleTop

```xml
<intent-filter>
  <action android:name="android.intent.action.VIEW" />

  <category android:name="android.intent.category.DEFAULT" />
  <category android:name="android.intent.category.BROWSABLE" />

  <!-- Accept URIs: {YOUR_APP_PACKAGE_NAME}://* -->
  <data android:scheme="{YOUR_APP_PACKAGE_NAME}" />
  <!-- For reference only -->
  <data android:scheme="unipassapp"
        android:host="com.unipass.wallet"
        android:pathPattern="/*"
        android:pathPrefix="/redirect" />
</intent-filter>
```

Make sure your sign-in activity launchMode is set to singleTop in your AndroidManifest.xml if you are using v0.0.3 or below

```xml
<activity
  android:launchMode="singleTop"
  android:name=".YourActivity">
  // ...
</activity>
```

Open your app's AndroidManifest.xml file and add the following permission: 
`<uses-permission android:name="android.permission.INTERNET" />`

4. Then you can use UniPassSDK in your project:

```Kotlin
import com.unipass.core.UniPassSDK
```

# Initialization

## Creating a UniPassSDK instance

The UniPassSDK Constructor takes an object with UniPassSDKOptions as input

```java
unipassInstance = UniPassSDK(UniPassSDKOptions)
```

### Arguments

UniPassSDKOptions

| Parameter   | Type                               | Mandatory | Description                                                              |
| ----------- | ---------------------------------- | -------- | ------------------------------------------------------------------------- |
| context     | android.content.Context            | Yes      | Android context to launch UniPass Wallet, usually is the current activity |
| activity    | AppCompatActivity                  | Yes      | Android activity to use UniPassSDK                                        |
| env         | com.unipass.core.types.Environment | Yes      | SDK Environment                                                           |
| redirectUrl | Uri                                | No       | URL that UniPassSDK will redirect API responses                           |
| walletUrl   | String                             | No       | UniPass Wallet Url，Default is https://testnet.wallet.unipass.id          |
| appSettings | com.unipass.core.types.AppSettings | No       | configuration optional object to use custom app settings. Refer [AppSettings](#appsettings) for more info  |

### AppSettings

```java
// appSettings
data class AppSettings(
    val chain: ChainType = ChainType.polygon,
    val appName: String? = null,
    val appIcon: String? = null,
    val theme: UniPassTheme = UniPassTheme.dark
)

// ChainType
enum class ChainType(val value: String) {
    eth("eth"),
    polygon("polygon"),
    bsc("bsc"),
    rangers("rangers"),
    scroll("scroll")
}

//（UniPassTheme）
enum class UniPassTheme(val value: String){
    dark("dark"),
    light("light")
}
```

### Sample Code

```java
unipassInstance = UniPassSDK(
    UniPassSDKOptions(
        context = this,
        activity = this,
        redirectUrl = Uri.parse("unipassapp://com.unipass.wallet/redirect"),
        env = Environment.TESTNET
    )
)
```

If you are using v0.0.3 or below, you also need to call the setResultUrl API in onNewIntent

## Set Result URL

```java
override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    unipassInstance.setResultUrl(intent?.data)
}
```

# Connect UniPass Wallet

## Type definitions:

```java
interface UnipassCallBack <T>{
    fun success(output: T?)
    fun failure(exception: Exception)
}

class LoginOutput: BaseOutput(OutputType.Login) {
    val userInfo: UserInfo? = null
}

data class UserInfo (
    var email: String?,
    var address: String,
    var newborn: Boolean = false
)

enum class OutputType {
    @SerializedName("UP_LOGIN")
    Login,

    @SerializedName("UP_LOGOUT")
    Logout,

    @SerializedName("UP_SIGN_MESSAGE")
    SignMessage,

    @SerializedName("UP_TRANSACTION")
    SendTransaction
}

```

## Sample Code

```java
unipassInstance.login(object : UnipassCallBack<LoginOutput> {
    override fun success(output: LoginOutput?) {
        Log.d("Unipass Login", "success")
    }

    override fun failure(error: Exception) {
        Log.d("Unipass Login", error.message ?: "Something went wrong")
    }
})
```

`newborn` can be used to track new registration count.

# Send Transaction

## Type definitions:

```java
data class SendTransactionInput (
  val from: String,
  val to: String,
  val value: String? = "0x",
  val data: String? = "0x",
)

class SendTransactionOutput: BaseOutput(OutputType.SendTransaction) {
    val transactionHash: String? = null
}
```

## Sample Code

```java
// Ensure that the user is authorized to log in
if (unipassInstance.isLogin()) {
    var transactionInput = SendTransactionInput(unipassInstance.getAddress(),
        "0x7b5Bd7c9E3A0D0Ef50A9b3aCF5d1AcD58C3590d1",
        "0x" + toWei("0.001", Convert.Unit.ETHER).toBigIntegerExact().toString(16)
    )
    unipassInstance.sendTransaction(transactionInput, object : UnipassCallBack<SendTransactionOutput> {
        override fun success(output: SendTransactionOutput?) {
            Log.d("Unipass SendTransaction", "success")
        }
        override fun failure(error: Exception) {
            Log.d("Unipass SendTransaction", error.message ?: "Something went wrong")
        }
    })
}
```

# Sign Message

## Type definitions:

```java
enum class SignType(val value: String) {
    @SerializedName("PersonalSign")
    PersonalSign("PersonalSign"),

    @SerializedName("SignTypedData")
    SignTypedData("SignTypedData"),
}

data class SignInput (
    val from: String,
    val type: SignType,
    val msg: String
)

class SignOutput: BaseOutput(OutputType.SignMessage) {
    val signature: String? = null
}
```

## Sample Code

```java
// Ensure that the user is authorized to log in
if (unipassInstance.isLogin()) {
    var signInput = SignInput(unipassInstance.getAddress(), SignType.PersonalSign, "message to be signed")
    unipassInstance.signMessage(signInput, object : UnipassCallBack<SignOutput> {
        override fun success(output: SignOutput?) {
            Log.d("Unipass Sign Message", "success")
        }
        override fun failure(error: Exception) {
            Log.d("Unipass Sign Message", error.message ?: "Something went wrong")
        }
    })
}
```

# Logout Account

## Sample Code

```java
// logout will clear local upAccount data
unipassInstance.logout(object : UnipassCallBack<Void> {
    override fun success(output: Void?) {
        Log.d("Unipass Logout", "success")
    }
    override fun failure(error: Exception) {
        Log.d("Unipass Logout", error.message ?: "Something went wrong")
    }
})
```