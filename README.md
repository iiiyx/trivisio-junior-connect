Trivisio Junior Connect App Sample
===================================

This sample shows how to implement one-way text sending over Bluetooth from an Android device to the
Trivisio Junior device, using the fundamental Bluetooth API capabilities.

This app is built on the [Android BluetoothChat Sample][1]

Please follow the [Overview of Android support for Bluetooth][2] to understand how Bluetooth API works
on Android devices.

[1]: https://github.com/android/connectivity-samples/tree/master/BluetoothChat
[2]: https://developer.android.com/guide/topics/connectivity/bluetooth

> [!IMPORTANT]
> Your future app must establish a BT connection using SPP UUID - the one that is used by the Junior device to receive text data: `00001101-0000-1000-8000-00805f9b34fb`, see the [BluetoothConnectService][3] for details

[3]: https://github.com/iiiyx/trivisio-junior-connect/blob/master/app/src/main/java/com/trivisio/junior/connect/BluetoothConnectService.java

Sample App Functionality
------------

1. Pair your Android device with the Junior device using Bluetooth
2. Run this sample app
3. If the device is paired and there is only one paired Junior device then the app will connect to the Junior device automatically. If there is more than one Junior device paired then the app will ask you to select one. Once selected, it will connect automatically.
4. When the device is connected, you can enter some text in the text input and tap the `Send` button.
5. Connected Junior device will show you the last 120 symbols of the sent text.

> [!IMPORTANT]
> Junior device can show only 120 symbols on its screen, you need to send only the last 120 recognized symbols, so, a user will see a continuously shifting set of text (a ticker). See [BluetoothConnectFragment.onTextSend()][4] for the implementation details.

[4]: https://github.com/iiiyx/trivisio-junior-connect/blob/master/app/src/main/java/com/trivisio/junior/connect/BluetoothConnectFragment.java#L380
