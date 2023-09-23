/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.trivisio.junior.connect;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.nio.charset.StandardCharsets;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothConnectFragment extends Fragment {

    private static final String TAG = "BluetoothConnectFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 200;
    private static final int REQUEST_ENABLE_BT = 300;

    private Button mConnectButton;
    private Button mDisconnectButton;
    private Button mSendButton;
    private EditText mTextInput;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the connect services
     */
    private BluetoothConnectService mConnectService = null;

    private String oldValue;
    private final static int CAPACITY = 120;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        FragmentActivity activity = getActivity();
        if (mBluetoothAdapter == null && activity != null) {
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mBluetoothAdapter == null) {
            return;
        }
        // If BT is not on, request that it be enabled.
        // setupConnectService() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            if (ContextCompat.checkSelfPermission(BluetoothConnectFragment.this.requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(BluetoothConnectFragment.this.requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the connection
        } else if (mConnectService == null) {
            setupConnectService();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectDevice();
    }

    public void disconnectDevice() {
        if (mConnectService != null) {
            sendMessage("Waiting for message", true);
            mConnectService.stop();
            oldValue = "";
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mConnectService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mConnectService.getState() == BluetoothConnectService.STATE_NONE) {
                // Start the Bluetooth connect service
                mConnectService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_connect, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mTextInput = view.findViewById(R.id.sendTextInput);
        mTextInput.setVisibility(View.GONE);

        mSendButton = view.findViewById(R.id.sendBtn);
        mSendButton.setVisibility(View.GONE);
        mSendButton.setOnClickListener(view1 -> {
            onTextSend(mTextInput.getText().toString());
        });

        mConnectButton = view.findViewById(R.id.connectBtn);
        mConnectButton.setOnClickListener(view1 -> {
            mConnectButton.setEnabled(false);
            Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
        });

        mDisconnectButton = view.findViewById(R.id.disconnectBtn);
        mDisconnectButton.setVisibility(View.GONE);
        mDisconnectButton.setOnClickListener(view1 -> {
            disconnectDevice();
        });
    }

    /**
     * Set up the UI and background operations for connection.
     */
    private void setupConnectService() {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() &&
                ContextCompat.checkSelfPermission(BluetoothConnectFragment.this.requireContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(BluetoothConnectFragment.this.requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            mConnectButton.performClick();
        }
        // Initialize the array adapter for the conversation thread
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        // Initialize the BluetoothConnectService to perform bluetooth connections
        mConnectService = new BluetoothConnectService(activity, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer();
    }

    private void sendMessage(String message) {
        sendMessage(message, false);
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     * @param silent Send text w/o toasts in any case.
     */
    private void sendMessage(String message, boolean silent) {
        // Check that we're actually connected before trying anything
        if (mConnectService.getState() != BluetoothConnectService.STATE_CONNECTED) {
            if (!silent) {
                Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        Log.d(TAG, String.format("sending %s", message));
        // Get the message bytes and tell the BluetoothConnectService to write
        byte[] send = message.getBytes(StandardCharsets.UTF_8);
        mConnectService.write(send);

        // Reset out string buffer to zero and clear the edit text field
        mOutStringBuffer.setLength(0);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothConnectService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothConnectService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            break;
                        case BluetoothConnectService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothConnectService.STATE_LISTEN:
                        case BluetoothConnectService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            mConnectButton.setEnabled(true);
                            mConnectButton.setVisibility(View.VISIBLE);
                            mTextInput.setVisibility(View.GONE);
                            mSendButton.setVisibility(View.GONE);
                            mDisconnectButton.setVisibility(View.GONE);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    // can be used to show sent message on UI
                    // byte[] writeBuf = (byte[]) msg.obj;
                    // // construct a string from the buffer
                    // String writeMessage = new String(writeBuf);
                    break;
                case Constants.MESSAGE_READ:
                    // can be used to retrieve message from the connected device
                    // byte[] readBuf = (byte[]) msg.obj;
                    // // construct a string from the valid bytes in the buffer
                    // String readMessage = new String(readBuf, 0, msg.arg1);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.CONNECTED:
                    mConnectButton.setVisibility(View.GONE);
                    mSendButton.setVisibility(View.VISIBLE);
                    mTextInput.setVisibility(View.VISIBLE);
                    mDisconnectButton.setVisibility(View.VISIBLE);
                    break;
                case Constants.DISCONNECTED:
                    mSendButton.setVisibility(View.GONE);
                    mTextInput.setVisibility(View.GONE);
                    mDisconnectButton.setVisibility(View.GONE);
                    mConnectButton.setVisibility(View.VISIBLE);
                    mConnectButton.setEnabled(true);
                    oldValue = "";
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK && data != null) {
                    connectDevice(data);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a connection
                    setupConnectService();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    FragmentActivity activity = getActivity();
                    if (activity != null) {
                        Toast.makeText(activity, R.string.bt_not_enabled_leaving,
                                Toast.LENGTH_SHORT).show();
                        activity.finish();
                    }
                }
                break;
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     */
    private void connectDevice(Intent data) {
        // Get the device MAC address
        Bundle extras = data.getExtras();
        if (extras == null) {
            return;
        }
        String address = extras.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (ActivityCompat.checkSelfPermission(BluetoothConnectFragment.this.requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            Log.d("Device UUIDs: ", String.valueOf(device.fetchUuidsWithSdp()));
        }
        // Attempt to connect to the device
        mConnectService.connect(device);
    }

    /****************************************** UI Widget Callbacks *******************************/

    private void onTextSend(String text) {
        String value = text.replaceAll("\n", " ").trim();
        if (value.equals(oldValue)) {
            return;
        }
        oldValue = value;
        if (value.length() == 0) {
            sendMessage("");
            return;
        }

        int c = value.length() - 1;
        StringBuilder sb = new StringBuilder(CAPACITY);
        for (int i = 0; i < CAPACITY && c >= 0; i++) {
            sb.append(value.charAt(c));
            c--;
        }

        value = sb.reverse().toString();
        sendMessage(value);
    }
}
