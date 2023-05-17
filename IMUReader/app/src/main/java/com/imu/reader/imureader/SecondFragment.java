package com.imu.reader.imureader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.imu.reader.imureader.BLE.IMUGattCallback;
import com.imu.reader.imureader.databinding.FragmentSecondBinding;

import java.text.DecimalFormat;

public class SecondFragment extends Fragment  {

    private FragmentSecondBinding binding;
    float gravidade[] =new float[3];
    float acel_linear[] =new float[3];
    private float acelX, acelY, acelZ = 0;

    private Receiver receiver;

    private int updateCont = 0;

    private static final String TAG = SecondFragment.class.getSimpleName();
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.receiver = new Receiver();
        getActivity().registerReceiver(receiver, new IntentFilter(IMUGattCallback.ACEL_UPDATE_INTENT));
        getActivity().registerReceiver(receiver, new IntentFilter(IMUGattCallback.GYRO_UPDATE_INTENT));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        getActivity().unregisterReceiver(receiver);
    }

    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent intent) {
            if (intent.getAction().equals(IMUGattCallback.ACEL_UPDATE_INTENT)) {
                float aux = intent.getFloatExtra("value", 0);
                int axis = intent.getIntExtra("axis", -1);
                if (axis == IMUGattCallback.X_AXIS) {
                    acelX = aux;
                } else if (axis == IMUGattCallback.Y_AXIS) {
                    acelY = aux;
                } else if (axis == IMUGattCallback.Z_AXIS) {
                    acelZ = aux;
                }
                if (acelX != 0 && acelY != 0 && acelZ != 0) {
                    final float alpha = (float) 0.8;

                    // Isolate the force of gravity with the low-pass filter.
                    gravidade[0] = alpha * gravidade[0] + (1 - alpha) * acelX;
                    gravidade[1] = alpha * gravidade[1] + (1 - alpha) * acelY;
                    gravidade[2] = alpha * gravidade[2] + (1 - alpha) * acelZ;

                    acel_linear[0] = acelX - gravidade[0];
                    acel_linear[1] = acelY - gravidade[1];
                    acel_linear[2] = acelZ - gravidade[2];


                    if ( updateCont % 10 == 0 ) {
                        DecimalFormat df = new DecimalFormat();
                        df.setMaximumFractionDigits(4);
                        binding.acelGravidade.setText(df.format(Math.abs(gravidade[0] + gravidade[1] + gravidade[2])) + " M/s\u00B2");
                        binding.acelLinear.setText(df.format(Math.abs(acel_linear[0] + acel_linear[1] + acel_linear[2])) + " M/s\u00B2");
                    }
                    updateCont++;
                }
                return;
            }
        }
    }
}