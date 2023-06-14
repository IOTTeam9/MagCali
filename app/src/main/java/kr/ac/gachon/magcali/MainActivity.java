package kr.ac.gachon.magcali;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] accelerationValues;
    private float[] magneticFieldValues;
    private float azimuth;
    private float calibratedAzimuth;
    private float previousVelocityX, previousVelocityY, previousVelocityZ;
    private float previousPositionX, previousPositionY, previousPositionZ;
    private long previousTimestamp;
    private static final float GRAVIT   Y_EARTH = SensorManager.GRAVITY_EARTH;
    private static final float NANOSECONDS_TO_SECONDS = 1.0f / 1000000000.0f;
    private static final float MIN_MOVEMENT_THRESHOLD = 1.5f;

    private TextView azimuthTextView;
    private TextView calibratedAzimuthTextView;
    private TextView xMoveTextView;
    private TextView yMoveTextView;
    private TextView zMoveTextView;
    private TextView timeChangeTextView;
    private Button calibrateButton;
    private LineChart graphChart;
    private ArrayList<Entry> graphData;
    private LineDataSet graphDataSet;
    private ArrayList<ILineDataSet> graphDataSets;
    private LineData graphDataLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerationValues = new float[3];
        magneticFieldValues = new float[3];

        azimuthTextView = findViewById(R.id.azimuthTextView);
        calibratedAzimuthTextView = findViewById(R.id.calibratedAzimuthTextView);
        xMoveTextView = findViewById(R.id.xMoveTextView);
        yMoveTextView = findViewById(R.id.yMoveTextView);
        zMoveTextView = findViewById(R.id.zMoveTextView);
        timeChangeTextView = findViewById(R.id.timeChangeTextView);
        calibrateButton = findViewById(R.id.calibrateButton);
        graphChart = findViewById(R.id.graphChart);

        calibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calibrateAzimuth();
            }
        });

        graphData = new ArrayList<>();
        graphDataSet = new LineDataSet(graphData, "이동 거리");
        graphDataSets = new ArrayList<>();
        graphDataSets.add(graphDataSet);
        graphDataLine = new LineData(graphDataSets);

        Description graphDescription = new Description();
        graphDescription.setText("이동 거리 그래프");
        graphChart.setDescription(graphDescription);
        graphChart.setData(graphDataLine);
        graphChart.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        previousTimestamp = System.nanoTime();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelerationValues = event.values.clone();
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticFieldValues = event.values.clone();
        }

        if (accelerationValues != null && magneticFieldValues != null) {
            float[] rotationMatrix = new float[9];
            boolean success = SensorManager.getRotationMatrix(rotationMatrix, null, accelerationValues, magneticFieldValues);
            if (success) {
                float[] orientationValues = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientationValues);

                azimuth = (float) Math.toDegrees(orientationValues[0]);

                float linearAccelerationX = accelerationValues[0] - GRAVITY_EARTH;
                float linearAccelerationY = accelerationValues[1] - GRAVITY_EARTH;
                float linearAccelerationZ = accelerationValues[2] - GRAVITY_EARTH;
                long currentTimestamp = System.nanoTime();
                float deltaTime = (currentTimestamp - previousTimestamp) * NANOSECONDS_TO_SECONDS;
                previousVelocityX += linearAccelerationX * deltaTime;
                previousVelocityY += linearAccelerationY * deltaTime;
                previousVelocityZ += linearAccelerationZ * deltaTime;
                previousPositionX += previousVelocityX * deltaTime;
                previousPositionY += previousVelocityY * deltaTime;
                previousPositionZ += previousVelocityZ * deltaTime;

                if (Math.abs(previousPositionX) < MIN_MOVEMENT_THRESHOLD) {
                    previousPositionX = 0.0f;
                }
                if (Math.abs(previousPositionY) < MIN_MOVEMENT_THRESHOLD) {
                    previousPositionY = 0.0f;
                }
                if (Math.abs(previousPositionZ) < MIN_MOVEMENT_THRESHOLD) {
                    previousPositionZ = 0.0f;
                }

                azimuthTextView.setText(getString(R.string.azimuth_label) + " " + String.valueOf(azimuth) + getString(R.string.degrees_symbol));
                calibratedAzimuthTextView.setText(getString(R.string.calibrated_azimuth_label) + " " + String.valueOf(calibratedAzimuth) + getString(R.string.degrees_symbol));
                xMoveTextView.setText(getString(R.string.x_movement_label) + " " + String.valueOf(previousPositionX) + getString(R.string.meters_symbol));
                yMoveTextView.setText(getString(R.string.y_movement_label) + " " + String.valueOf(previousPositionY) + getString(R.string.meters_symbol));
                zMoveTextView.setText(getString(R.string.z_movement_label) + " " + String.valueOf(previousPositionZ) + getString(R.string.meters_symbol));
                timeChangeTextView.setText(getString(R.string.time_change_label) + " " + String.valueOf(deltaTime) + getString(R.string.seconds_symbol));

                graphData.add(new Entry(currentTimestamp, previousPositionX));

                graphDataSet.notifyDataSetChanged();
                graphDataLine.notifyDataChanged();
                graphChart.notifyDataSetChanged();
                graphChart.invalidate();

                previousTimestamp = currentTimestamp;
                accelerationValues = null;
                magneticFieldValues = null;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 정확도 변경 이벤트 처리
    }

    private void calibrateAzimuth() {
        // 방위각 캘리브레이션 로직 구현
        // TODO: 방위각 캘리브레이션 코드 작성
        calibratedAzimuth = azimuth;
    }
}
