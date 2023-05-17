package com.imu.reader.imureader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.imu.reader.imureader.databinding.ActivityFindDeviceBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FindDeviceActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, View.OnClickListener {

    private ActivityFindDeviceBinding binding;
    private BluetoothAdapter adapter;
    private Set<BluetoothDevice> pairedDevices;
    private List<String> deviceNames;
    private List<String> deviceMACS;

    private static final String TAG = FindDeviceActivity.class.getSimpleName();

    public FindDeviceActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityFindDeviceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        binding.buttonSearch.setOnClickListener(this);
        binding.devicesList.setOnItemClickListener(this);

        this.adapter = BluetoothAdapter.getDefaultAdapter();

        FindDeviceActivity.this.registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        FindDeviceActivity.this.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        FindDeviceActivity.this.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));

        if (this.adapter.isEnabled()) {
            if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) &&
                    (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) &&
                    (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        if (!this.adapter.isDiscovering()) {
            this.adapter.startDiscovery();
        }

        this.pairedDevices = new HashSet<>();
        this.pairedDevices.addAll(adapter.getBondedDevices());
        deviceNames = new ArrayList<>();
        deviceMACS = new ArrayList<>();
        if (this.pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                deviceMACS.add(deviceHardwareAddress);
                deviceNames.add(deviceName);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1, deviceNames);
            binding.devicesList.setAdapter(adapter);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (this.adapter.isDiscovering()) {
            this.adapter.cancelDiscovery();
        }
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        String selected = deviceMACS.get(i);

        for (BluetoothDevice dev : this.pairedDevices) {
            if (dev.getAddress().equals(selected)) {
                Intent intent = new Intent();
                intent.putExtra("bt_selected", dev);
                setResult(RESULT_OK, intent);
                break;
            }
        }
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }


        super.onBackPressed();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.buttonSearch) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            if (adapter.isEnabled()) {
                adapter.startDiscovery();
            }
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                binding.toolbar.setTitle(getString(R.string.searching));
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.progressBar.setProgress(0);
                binding.buttonSearch.setEnabled(false);
            }
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (!pairedDevices.contains(device)) {
                    pairedDevices.add(device);
                }

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(FindDeviceActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                    return;
                }
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(TAG, "Device name: " + deviceName);
                Log.d(TAG, "MAC Address: " + deviceHardwareAddress);

                if (deviceName == null || deviceHardwareAddress == null) {
                    return;
                }

                if (!deviceMACS.contains(deviceHardwareAddress)) {
                    deviceMACS.add(deviceHardwareAddress);
                }

                if (!deviceNames.contains(deviceName)) {
                    deviceNames.add(deviceName);
                }

                runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(FindDeviceActivity.this, android.R.layout.simple_list_item_1, deviceNames);
                    binding.devicesList.setAdapter(adapter);
                });


            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                binding.toolbar.setTitle(getString(R.string.located));
                binding.progressBar.setVisibility(View.GONE);
                binding.buttonSearch.setEnabled(true);
            }
        }
    };
}