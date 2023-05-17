package com.imu.reader.imureader.BLE;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public class IMUGattCallback extends BluetoothGattCallback implements Serializable {

    private static final String TAG = IMUGattCallback.class.getSimpleName();

    private Context context;

    private int x = 0;

    public static final int X_AXIS = 0;
    public static final int Y_AXIS = 1;
    public static final int Z_AXIS = 2;

    public static final String ACEL_UPDATE_INTENT = "imu.reader.acel.value";

    public static final String GYRO_UPDATE_INTENT = "imu.reader.gyro.value";

    private static final String CHARAC_ACEL_X = "e7890e92-ed43-11ed-a05b-0242ac120003";
    private static final String CHARAC_ACEL_Y = "9b788ed0-f04d-11ed-a05b-0242ac120003";
    private static final String CHARAC_ACEL_Z = "9b789218-f04d-11ed-a05b-0242ac120003";

    private static final String CHARAC_GYRO_X = "9b789470-f04d-11ed-a05b-0242ac120003";
    private static final String CHARAC_GYRO_Y = "9b789632-f04d-11ed-a05b-0242ac120003";
    private static final String CHARAC_GYRO_Z = "9b7897d6-f04d-11ed-a05b-0242ac120003";

    private static final String CHARAC_TEMP_X = "";


    public IMUGattCallback(Context context) {
        this.context = context;
    }

    private void sendBroadcastAcelUpdate(int axis, float value) {
        Intent intent = new Intent();
        intent.setAction(ACEL_UPDATE_INTENT);
        intent.putExtra("axis",axis);
        intent.putExtra("value",value);
        context.sendBroadcast(intent);
    }

    private void sendBroadcastGyriUpdate(int axis, float value) {
        Intent intent = new Intent();
        intent.setAction(GYRO_UPDATE_INTENT);
        intent.putExtra("axis",axis);
        intent.putExtra("value",value);
        context.sendBroadcast(intent);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.d(TAG, "Cnnected");
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            gatt.discoverServices();
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.d(TAG, "Disconnected");
        }
    }


    @Override
    public void onCharacteristicRead(
            BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic,
            int status
    ) {
        Log.d(TAG, "Leu");
        // For all other profiles, writes the data formatted in HEX.
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            Log.d(TAG, "dados: " + stringBuilder.toString());
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        final byte[] data = characteristic.getValue();
        if ( data == null ) {
            return;
        }

        if ( data.length == 0 ) {
            return;
        }

        float aux = 0;

        if (characteristic.getUuid().toString().equals(CHARAC_ACEL_X)) {
            aux = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getFloat();
            sendBroadcastAcelUpdate(X_AXIS, aux);
            return;
        }

        if (characteristic.getUuid().toString().equals(CHARAC_ACEL_Y)) {
            aux = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getFloat();
            sendBroadcastAcelUpdate(Y_AXIS, aux);
            return;
        }

        if (characteristic.getUuid().toString().equals(CHARAC_ACEL_Z)) {
            aux = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getFloat();
            sendBroadcastAcelUpdate(Z_AXIS, aux);
            return;
        }

        if (characteristic.getUuid().toString().equals(CHARAC_GYRO_X)) {
            aux = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getFloat();
            sendBroadcastGyriUpdate(X_AXIS, aux);
            return;
        }

        if (characteristic.getUuid().toString().equals(CHARAC_GYRO_Y)) {
            aux = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getFloat();
            sendBroadcastGyriUpdate(Y_AXIS, aux);
            return;
        }

        if (characteristic.getUuid().toString().equals(CHARAC_GYRO_Z)) {
            aux = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getFloat();
            sendBroadcastGyriUpdate(Z_AXIS, aux);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        for (BluetoothGattService service : gatt.getServices()) {
            Log.d(TAG, "Service UUID: " + service.getUuid().toString());
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                Log.d(TAG, "characteristic: " + characteristic.getUuid());
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                Log.d(TAG, "Properties: " + characteristic.getProperties());
                if((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == BluetoothGattCharacteristic.PROPERTY_NOTIFY){
                    Log.d(TAG, "Suporta Notificação" );
                    gatt.setCharacteristicNotification(characteristic, true);
                    UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(uuid);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                }
            }

        }
    }
}