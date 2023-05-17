package com.imu.reader.imureader;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.imu.reader.imureader.BLE.IMUGattCallback;
import com.imu.reader.imureader.databinding.FragmentFirstBinding;

import java.util.ArrayList;
import java.util.List;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private BluetoothDevice device;

    private static final String TAG = MainActivity.class.getSimpleName();
    private List<Entry> valuesAcelX = new ArrayList<>();
    private List<Entry> valuesAcelY = new ArrayList<>();
    private ArrayList<Entry> valuesAcelZ = new ArrayList<>();

    private ArrayList<Entry> valuesGyroX = new ArrayList<>();
    private ArrayList<Entry> valuesGyroY = new ArrayList<>();
    private ArrayList<Entry> valuesGyroZ = new ArrayList<>();

    private int x = -1;
    private int x2 = -1;
    private LineDataSet setAcelX;
    private LineDataSet setAcelY;
    private LineDataSet setAcelZ;

    private LineDataSet setGyroX;
    private LineDataSet setGyroY;
    private LineDataSet setGyroZ;

    private float acelX;
    private float acelY;
    private float acelZ;


    private float gyroX;
    private float gyroY;
    private float gyroZ;

    private IMUGattCallback imuGattCallback;

    private Receiver receiver;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prepareAcelChart();
        prepareGyroChart();

        binding.experience.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               Bundle bundle = new Bundle();
               bundle.putSerializable("IMUCallback", getImuGattCallback());
               NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment, bundle);
            }
        });

        this.receiver = new Receiver();
        getActivity().registerReceiver(receiver, new IntentFilter(IMUGattCallback.ACEL_UPDATE_INTENT));
        getActivity().registerReceiver(receiver, new IntentFilter(IMUGattCallback.GYRO_UPDATE_INTENT));
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        getImuGattCallback();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        getActivity().unregisterReceiver(receiver);
    }

    private void prepareAcelChart(){
        binding.chart1.getDescription().setEnabled(false);
        binding.chart1.setTouchEnabled(false);
        binding.chart1.setBackgroundColor(Color.WHITE);

        XAxis xAxis = binding.chart1.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
        //xAxis.setTypeface(tfLight);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawAxisLine(false);
        xAxis.setCenterAxisLabels(true);


        YAxis leftAxis = binding.chart1.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        //leftAxis.setTypeface(tfLight);
        leftAxis.setTextColor(ColorTemplate.getHoloBlue());
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(-70f);
        leftAxis.setAxisMaximum(70f);
        leftAxis.setYOffset(-9f);
        leftAxis.setTextColor(Color.BLACK);

        YAxis rightAxis = binding.chart1.getAxisRight();
        rightAxis.setEnabled(false);

        // create a dataset and give it a type
        setAcelX = new LineDataSet(valuesAcelX, "Acelrômetro X");
        setAcelX.setAxisDependency(YAxis.AxisDependency.LEFT);
        setAcelX.setColor(Color.RED);
        setAcelX.setValueTextColor(ColorTemplate.getHoloBlue());
        setAcelX.setLineWidth(1.5f);
        setAcelX.setDrawCircles(false);
        setAcelX.setDrawValues(false);
        setAcelX.setFillAlpha(65);
        setAcelX.setFillColor(ColorTemplate.getHoloBlue());
        setAcelX.setHighLightColor(Color.GREEN);
        setAcelX.setDrawCircleHole(false);

        setAcelY = new LineDataSet(valuesAcelY, "Acelrômetro Y");
        setAcelY.setAxisDependency(YAxis.AxisDependency.LEFT);
        setAcelY.setColor(Color.GREEN);
        setAcelY.setValueTextColor(ColorTemplate.getHoloBlue());
        setAcelY.setLineWidth(1.5f);
        setAcelY.setDrawCircles(false);
        setAcelY.setDrawValues(false);
        setAcelY.setFillAlpha(65);
        setAcelY.setFillColor(ColorTemplate.getHoloBlue());
        setAcelY.setHighLightColor(Color.rgb(244, 117, 117));
        setAcelY.setDrawCircleHole(false);

        setAcelZ = new LineDataSet(valuesAcelZ, "Acelrômetro Z");
        setAcelZ.setAxisDependency(YAxis.AxisDependency.LEFT);
        setAcelZ.setColor(Color.BLUE);
        setAcelZ.setValueTextColor(ColorTemplate.getHoloBlue());
        setAcelZ.setLineWidth(1.5f);
        setAcelZ.setDrawCircles(false);
        setAcelZ.setDrawValues(false);
        setAcelZ.setFillAlpha(65);
        setAcelZ.setFillColor(ColorTemplate.getHoloBlue());
        setAcelZ.setHighLightColor(Color.BLUE);
        setAcelZ.setDrawCircleHole(false);

        // create a data object with the data sets
        LineData data = new LineData(setAcelX);
        data.addDataSet(setAcelY);
        data.addDataSet(setAcelZ);

        data.setValueTextColor(Color.WHITE);
        data.setValueTextSize(9f);

        // set data
        binding.chart1.setData(data);

    }

    private void prepareGyroChart(){
        binding.chart2.getDescription().setEnabled(false);
        binding.chart2.setTouchEnabled(false);
        binding.chart2.setBackgroundColor(Color.WHITE);

        XAxis xAxis = binding.chart2.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
        //xAxis.setTypeface(tfLight);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawAxisLine(false);
        xAxis.setCenterAxisLabels(true);


        YAxis leftAxis = binding.chart2.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        //leftAxis.setTypeface(tfLight);
        leftAxis.setTextColor(ColorTemplate.getHoloBlue());
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(-700f);
        leftAxis.setAxisMaximum(700f);
        leftAxis.setYOffset(-9f);
        leftAxis.setTextColor(Color.BLACK);

        YAxis rightAxis = binding.chart2.getAxisRight();
        rightAxis.setEnabled(false);

        // create a dataset and give it a type
        setGyroX = new LineDataSet(valuesGyroX, "Giroscópio X");
        setGyroX.setAxisDependency(YAxis.AxisDependency.LEFT);
        setGyroX.setColor(Color.RED);
        setGyroX.setValueTextColor(ColorTemplate.getHoloBlue());
        setGyroX.setLineWidth(1.5f);
        setGyroX.setDrawCircles(false);
        setGyroX.setDrawValues(false);
        setGyroX.setFillAlpha(65);
        setGyroX.setFillColor(ColorTemplate.getHoloBlue());
        setGyroX.setHighLightColor(Color.GREEN);
        setGyroX.setDrawCircleHole(false);

        setGyroY = new LineDataSet(valuesGyroY, "Giroscópio Y");
        setGyroY.setAxisDependency(YAxis.AxisDependency.LEFT);
        setGyroY.setColor(Color.GREEN);
        setGyroY.setValueTextColor(ColorTemplate.getHoloBlue());
        setGyroY.setLineWidth(1.5f);
        setGyroY.setDrawCircles(false);
        setGyroY.setDrawValues(false);
        setGyroY.setFillAlpha(65);
        setGyroY.setFillColor(ColorTemplate.getHoloBlue());
        setGyroY.setHighLightColor(Color.rgb(244, 117, 117));
        setGyroY.setDrawCircleHole(false);

        setGyroZ = new LineDataSet(valuesGyroZ, "Giroscópio Z");
        setGyroZ.setAxisDependency(YAxis.AxisDependency.LEFT);
        setGyroZ.setColor(Color.BLUE);
        setGyroZ.setValueTextColor(ColorTemplate.getHoloBlue());
        setGyroZ.setLineWidth(1.5f);
        setGyroZ.setDrawCircles(false);
        setGyroZ.setDrawValues(false);
        setGyroZ.setFillAlpha(65);
        setGyroZ.setFillColor(ColorTemplate.getHoloBlue());
        setGyroZ.setHighLightColor(Color.BLUE);
        setGyroZ.setDrawCircleHole(false);

        // create a data object with the data sets
        LineData data = new LineData(setGyroX);
        data.addDataSet(setGyroY);
        data.addDataSet(setGyroZ);

        data.setValueTextColor(Color.WHITE);
        data.setValueTextSize(9f);

        // set data
        binding.chart2.setData(data);
    }

    public void startBLEConnection(BluetoothDevice device) {
        this.device = device;
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        this.device.connectGatt(getActivity(), true, getImuGattCallback());
    }

    private synchronized void updateAcelDataSet(int axis, float value) throws Exception {
        if ( binding == null) {
            return;
        }
        if(axis < 0){
            return;
        }
        x++;
        getActivity().runOnUiThread(() -> {
        if ( axis == IMUGattCallback.X_AXIS ) {
            if ( setAcelX.getEntryCount() > 100 ) {
                setAcelX.removeFirst();
            }
            setAcelX.addEntry(new Entry(x, value));
            acelX = value;
        } else if ( axis == IMUGattCallback.Y_AXIS ) {
            if ( setAcelY.getEntryCount() > 100 ) {
                setAcelY.removeFirst();
            }
            setAcelY.addEntry(new Entry(x, value));
            acelY = value;
        } else if ( axis == IMUGattCallback.Z_AXIS ){
            if ( setAcelZ.getEntryCount() > 100 ) {
                setAcelZ.removeFirst();
            }
            setAcelZ.addEntry(new Entry(x,value));
            acelZ = value;
        }

            if ( binding == null) {
                return;
            }
            binding.acelerometro.setText(String.format("Acelerômetro X: %.4f Y: %.4f Z: %.4f",acelX,acelY,acelZ));
            LineData data = new LineData(setAcelX);
            data.addDataSet(setAcelY);
            data.addDataSet(setAcelZ);
            binding.chart1.setData(data);
            binding.chart1.notifyDataSetChanged();
            binding.chart1.invalidate();
        });
    }

    private synchronized void updateGyroDataSet(int axis, float value) throws Exception {
        if ( binding == null) {
            return;
        }
        x2++;
        getActivity().runOnUiThread(() -> {
        if ( axis == IMUGattCallback.X_AXIS ) {
            if ( setGyroX.getEntryCount() > 100 ) {
                setGyroX.removeFirst();
            }
            gyroX = value;
            setGyroX.addEntry(new Entry(x, value));
        } else if ( axis == IMUGattCallback.Y_AXIS ) {
            if ( setGyroY.getEntryCount() > 100 ) {
                setGyroY.removeFirst();
            }
            setGyroY.addEntry(new Entry(x, value));
            gyroY = value;
        } else if ( axis == IMUGattCallback.Z_AXIS ){
            if ( setGyroZ.getEntryCount() > 100 ) {
                setGyroZ.removeFirst();
            }
            gyroZ = value;
            setGyroZ.addEntry(new Entry(x,value));
        }

            if ( binding == null) {
                return;
            }
            binding.gyro.setText(String.format("Giroscópio X: %.4f Y: %.4f Z: %.4f",gyroX,gyroY,gyroZ));
            LineData data = new LineData(setGyroX);
            data.addDataSet(setGyroY);
            data.addDataSet(setGyroZ);
            binding.chart2.setData(data);
            binding.chart2.notifyDataSetChanged();
            binding.chart2.invalidate();
        });
    }

    private IMUGattCallback getImuGattCallback(){
        if ( this.imuGattCallback == null)
            imuGattCallback = new IMUGattCallback(getActivity());
        return imuGattCallback;
    }


    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent intent) {
            if ( intent.getAction().equals(IMUGattCallback.ACEL_UPDATE_INTENT) ) {
                float aux = intent.getFloatExtra("value", 0);
                int axis = intent.getIntExtra("axis", -1);
                try {
                    updateAcelDataSet(axis, aux);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return;
            }

            if ( intent.getAction().equals(IMUGattCallback.GYRO_UPDATE_INTENT) ) {
                float aux = intent.getFloatExtra("value", 0);
                int axis = intent.getIntExtra("axis", -1);
                try {
                    updateGyroDataSet(axis, aux);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return;
            }
        }
    }
}