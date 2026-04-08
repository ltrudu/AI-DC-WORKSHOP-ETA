package zebra.BarcodeDetector;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.camera2.CameraMetadata;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.view.LifecycleCameraController;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import zebra.BarcodeDetector.databinding.ActivityCameraXactivityBinding;

public class CameraXActivity extends AppCompatActivity {

    private static final String TAG = "ZETA-WORSKOP";
    public static boolean isZEBRA = false;
    public static final long VIEW_RESET_PERIOD_MS = 100L;

    private ActivityCameraXactivityBinding viewBinding;

    // -103- define a private EntityTrackerAnalyzer entityTrackerAnalyzer;

    private ExecutorService workerExecutor;
    public final ExecutorService executor = Executors.newFixedThreadPool(3);
    private SharedPreferences sharedPreferences;

    private LifecycleCameraController cameraController;

    private int readCounter = 0;
    private final ArrayDeque<Long> fpsQueue = new ArrayDeque<>(1);

    // -108- bc variable definition: private BarcodeDecoder barcodeDecoder = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("zebra/ai/entityTracker exerciser");
        ActionBar bar = getSupportActionBar();
        if (bar != null) bar.hide();

        Log.i(TAG, "isZebra device = " + isZEBRA);
        Log.i(TAG, "Device details:\n" + getDeviceDetails());

        viewBinding = ActivityCameraXactivityBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        ToggleButton toggle = viewBinding.toggleAnalyzerMode;
        toggle.setOnClickListener(this::onClickToggleAnalyzerMode);

        sharedPreferences = getSharedPreferences("SettingsPreferences", MODE_PRIVATE);
        loadSettings();

        if (Build.MANUFACTURER == null || !Build.MANUFACTURER.toUpperCase().contains("ZEBRA")) {
            isZEBRA = false;
            viewBinding.toggleAnalyzerMode.setVisibility(View.GONE);
            Log.w(TAG, "Non-Zebra device detected, forcing isZEBRA = false");
        } else {
            isZEBRA = true;
            viewBinding.toggleAnalyzerMode.setChecked(true);
            Log.i(TAG, "Zebra device detected in onCreate()");
        }

        workerExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (cameraController != null) {
            cameraController.clearImageAnalysisAnalyzer();
        }
        // -106- uncomment when defining the barcode decoder to properly dispose of it
        // if (barcodeDecoder != null) { barcodeDecoder.dispose(); barcodeDecoder = null; }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraController != null) {
            cameraController.clearImageAnalysisAnalyzer();
        }
        // -107- uncomment when defining the barcode decoder
        // if (barcodeDecoder != null) { barcodeDecoder.dispose(); barcodeDecoder = null; }
        if (workerExecutor != null) {
            workerExecutor.shutdown();
        }
    }

    private void onClickToggleAnalyzerMode(View view) {
        // no-op, matches source
    }

    @Override
    protected void onResume() {
        super.onResume();

        long timebegin = System.currentTimeMillis();

        setupCameraAndAnalyzers();

        if (isZEBRA) {
            initZETA();

            // -102- setting entityTrackerAnalyzer as current analyzer

            // -109- remove this sample analyzer when adding the -102- eta analyzer
            cameraController.setImageAnalysisAnalyzer(
                    workerExecutor,
                    (ImageAnalysis.Analyzer) (ImageProxy imageProxy) -> {
                        long currentTime = System.currentTimeMillis();
                        Bitmap bitmap = imageProxy.toBitmap();
                        float rotation = imageProxy.getImageInfo().getRotationDegrees();
                        Matrix matrix = new Matrix();
                        matrix.postRotate(rotation);
                        Bitmap debugBMP = Bitmap.createBitmap(
                                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        Log.d(TAG, "#WORKSHOP Frame received at " + currentTime
                                + " with resolution " + imageProxy.getWidth() + "x" + imageProxy.getHeight());
                        imageProxy.close();
                    });
            Log.i(TAG, "A sample analyzer has been set to show how frames are provisioned to the app.");
            // end of -109-
        }

        periodJobOnCanvas(VIEW_RESET_PERIOD_MS);

        viewBinding.tvOCRout.setVisibility(View.VISIBLE);
        viewBinding.overlayView.setVisibility(View.VISIBLE);
    }

    private void loadSettings() {
        int imageSize = sharedPreferences.getInt("IMAGESIZE", 1);
        switch (imageSize) {
            case 0:
                OverlayView.CAMERA_RESOLUTION_WIDTH = 480;
                OverlayView.CAMERA_RESOLUTION_HEIGHT = 640;
                break;
            case 1:
                OverlayView.CAMERA_RESOLUTION_WIDTH = 1080;
                OverlayView.CAMERA_RESOLUTION_HEIGHT = 1920;
                break;
            case 2:
                OverlayView.CAMERA_RESOLUTION_WIDTH = 1536;
                OverlayView.CAMERA_RESOLUTION_HEIGHT = 2048;
                break;
            case 3:
                OverlayView.CAMERA_RESOLUTION_WIDTH = 1920;
                OverlayView.CAMERA_RESOLUTION_HEIGHT = 2560;
                break;
            case 4:
                OverlayView.CAMERA_RESOLUTION_WIDTH = -1;
                OverlayView.CAMERA_RESOLUTION_HEIGHT = -1;
                break;
        }

        int scene = sharedPreferences.getInt("SCENE", 0);
        switch (scene) {
            case 0:
                OverlayView.CHOSEN_SCENE = CameraMetadata.CONTROL_SCENE_MODE_STEADYPHOTO;
                break;
            case 1:
                OverlayView.CHOSEN_SCENE = CameraMetadata.CONTROL_SCENE_MODE_ACTION;
                break;
            case 2:
                OverlayView.CHOSEN_SCENE = CameraMetadata.CONTROL_SCENE_MODE_SPORTS;
                break;
            case 3:
                OverlayView.CHOSEN_SCENE = CameraMetadata.CONTROL_SCENE_MODE_BARCODE;
                break;
        }

        int zoom = sharedPreferences.getInt("ZOOM", 0);
        switch (zoom) {
            case 0: OverlayView.ZOOM_RATIO = 1.0; break;
            case 1: OverlayView.ZOOM_RATIO = 1.5; break;
            case 2: OverlayView.ZOOM_RATIO = 2.0; break;
            case 3: OverlayView.ZOOM_RATIO = 3.0; break;
            case 4: OverlayView.ZOOM_RATIO = 5.0; break;
        }

        String highlight = sharedPreferences.getString("BARCODESTOHIGHLIGHT", "");
        OverlayView.BARCODETOHIGHLIGHT = highlight == null ? "" : highlight;

        isZEBRA = sharedPreferences.getBoolean("IS_ZEBRA_MODE", false);
    }

    private void periodJobOnCanvas(final long timeInterval) {
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                viewBinding.overlayView.readRate = readCounter;
                readCounter = 0;

                viewBinding.overlayView.invalidate();
                viewBinding.overlayView.clq.clear();

                handler.postDelayed(this, timeInterval);
            }
        };
        handler.postDelayed(runnable, timeInterval);
    }

    private void initZETA() {
        // -105- here init the AISuite SDK

        // -104- here prepare the barcode settings

        // try {
        //     // -100- here instantiate a barcode object.
        //     // -101- here use entityTrackerAnalyzer: pass the barcode detector
        //     //       and take care of the decoding results
        // } catch (IOException e) {
        //     Log.e(TAG, "Fatal error: load failed - " + e.getMessage());
        //     CameraXActivity.this.finish();
        // }
    }

    private void setOutputtextInMainThread(final String txt) {
        runOnUiThread(() -> viewBinding.tvOCRout.setText(txt));
    }

    private void appendOutputtextInMainThread(final String txt) {
        runOnUiThread(() -> viewBinding.tvOCRout.setText(txt + "" + viewBinding.tvOCRout.getText()));
    }

    // load png
    private byte[] loadBitmapFromAsset() throws IOException {
        InputStream inputStream = getAssets().open("technologies.png");
        byte[] buffer = new byte[inputStream.available()];
        //noinspection ResultOfMethodCallIgnored
        inputStream.read(buffer);
        inputStream.close();
        return buffer;
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private void setupCameraAndAnalyzers() {
        // 0. Init vals from settings
        ResolutionSelector resolutionSelectorHighest = new ResolutionSelector.Builder()
                .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                .setAllowedResolutionMode(ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
                .build();

        ResolutionSelector resolutionSelectorAsSettings = new ResolutionSelector.Builder()
                .setResolutionFilter((supportedSizes, rotationDegrees) -> {
                    int targetW = OverlayView.CAMERA_RESOLUTION_WIDTH;
                    int targetH = OverlayView.CAMERA_RESOLUTION_HEIGHT;

                    Size exactMatch = null;
                    for (Size s : supportedSizes) {
                        if ((s.getWidth() == targetW && s.getHeight() == targetH)
                                || (s.getWidth() == targetH && s.getHeight() == targetW)) {
                            exactMatch = s;
                            break;
                        }
                    }

                    if (exactMatch != null) {
                        return Collections.singletonList(exactMatch);
                    } else {
                        return new ArrayList<>(supportedSizes);
                    }
                })
                .setResolutionStrategy(new ResolutionStrategy(
                        new Size(OverlayView.CAMERA_RESOLUTION_WIDTH, OverlayView.CAMERA_RESOLUTION_HEIGHT),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                .build();

        // 1. Initialize the LifecycleCameraController and bind it to the lifecycle
        cameraController = new LifecycleCameraController(this);
        cameraController.bindToLifecycle(this);
        cameraController.setCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA);
        cameraController.setImageAnalysisBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);
        cameraController.setImageCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY);
        cameraController.setImageAnalysisResolutionSelector(
                OverlayView.CAMERA_RESOLUTION_WIDTH > -1
                        ? resolutionSelectorAsSettings
                        : resolutionSelectorHighest);
        cameraController.setZoomRatio((float) OverlayView.ZOOM_RATIO);

        // 2. Link the controller to the PreviewView
        viewBinding.viewFinder.setController(cameraController);
    }

    private void requestPermissions() {
    }

    private String getDeviceDetails() {
        String androidId = "A_ID=" + Settings.Secure.getString(
                getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        return Build.MANUFACTURER + "\n"
                + Build.MODEL + "\n"
                + Build.DISPLAY + "\n"
                + BuildConfig.APPLICATION_ID + "-"
                + BuildConfig.VERSION_NAME + ","
                + androidId + "\n";
    }
}
