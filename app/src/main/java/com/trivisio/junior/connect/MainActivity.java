package com.trivisio.junior.connect;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

/**
 * A simple launcher activity containing a summary sample description, sample log and a custom
 * {@link Fragment} which can display a view.
 * <p>
 * For devices with displays with a width of 720dp or greater, the sample log is always visible,
 * on other devices it's visibility is controlled by an item on the Action Bar.
 */
public class MainActivity extends FragmentActivity {

    public static final String TAG = "MainActivity";
    private static final int REQUEST_BT_PERMISSIONS = 222;

    private boolean doubleBackToExitPressedOnce;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private Bundle mSavedInstanceState;
    private BluetoothConnectFragment bluetoothConnectFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSavedInstanceState = savedInstanceState;
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(
                        MainActivity.this,
                        new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BT_PERMISSIONS);
                return;
            }
        }

        continueInit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // do not call super
        if (requestCode == REQUEST_BT_PERMISSIONS) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 1 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                continueInit();
            } else {
                Toast.makeText(MainActivity.this, "Bluetooth permissions are required. Cannot continue.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void continueInit() {
        if (mSavedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            bluetoothConnectFragment = new BluetoothConnectFragment();
            transaction.replace(R.id.sample_content_fragment, bluetoothConnectFragment);
            transaction.commit();
        }

        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    public void onBackPressed() {
        //Checking for fragment count on backstack
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else if (!doubleBackToExitPressedOnce) {
            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Press BACK again to exit.", Toast.LENGTH_SHORT).show();

            uiHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 2000);
        } else {
            if (bluetoothConnectFragment != null) {
                bluetoothConnectFragment.disconnectDevice();
            }
            super.onBackPressed();
        }
    }

}
