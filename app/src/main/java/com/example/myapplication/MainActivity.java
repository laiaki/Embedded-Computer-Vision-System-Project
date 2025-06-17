package com.example.myapplication;

import static com.google.ar.sceneform.lullmodel.MaterialTextureUsage.Light;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import android.net.Uri;

import com.google.android.filament.Engine;

import dev.romainguy.kotlin.math.Float3;
import io.github.sceneview.SceneView;
import io.github.sceneview.node.Node;
import io.github.sceneview.loaders.ModelLoader;

import com.example.myapplication.ModelLoaderWrapper;

import android.graphics.Color;

import io.github.sceneview.node.ViewNode;


public class MainActivity extends AppCompatActivity {
    private Engine engine;
    private Node modelNode;

    private PreviewView previewView;
    private TextView resultTextView;

    private ExecutorService cameraExecutor;
    private BarcodeScanner scanner;
    private boolean scanned = false;

    private SceneView sceneView;

    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        engine = Engine.create();

        previewView = findViewById(R.id.previewView);
        resultTextView = findViewById(R.id.resultTextView);
        sceneView = findViewById(R.id.arSceneView);
        sceneView.setBackgroundColor(Color.TRANSPARENT);


        cameraExecutor = Executors.newSingleThreadExecutor();

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        scanner = BarcodeScanning.getClient(options);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private final ActivityResultLauncher<String> cameraPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "需要攝影機權限才能運作", Toast.LENGTH_LONG).show();
                }
            });

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, selector, preview, imageAnalysis);
            } catch (Exception e) {
                Log.e("CameraX", "Camera start failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeImage(@NonNull ImageProxy image) {
        if (scanned) {
            image.close();
            return;
        }

        @android.annotation.SuppressLint("UnsafeOptInUsageError")
        android.media.Image mediaImage = image.getImage();
        if (mediaImage != null) {
            InputImage inputImage = InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());
            scanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        if (barcodes.size() > 0) {
                            Barcode barcode = barcodes.get(0);
                            String rawValue = barcode.getRawValue();
                            scanned = true;

                            runOnUiThread(() -> {
                                handleScannedString(rawValue);
                            });
                        }
                    })
                    .addOnFailureListener(e -> Log.e("Barcode", "解析失敗", e))
                    .addOnCompleteListener(task -> image.close());
        } else {
            image.close();
        }
    }

    private void handleScannedString(String scannedString) {
        if (scannedString.startsWith("http")) {
            fetchJsonFromUrl(scannedString);
        } else {
            parseJsonAndLoadModel(scannedString);
        }
    }

    private void fetchJsonFromUrl(String url) {
        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "無法下載 JSON 資料", Toast.LENGTH_LONG).show();
                    Log.e("HTTP", "下載失敗", e);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonStr = response.body().string();
                    runOnUiThread(() -> parseJsonAndLoadModel(jsonStr));
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "下載 JSON 失敗，回傳非成功狀態", Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    private void parseJsonAndLoadModel(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            String title = json.optString("title", "");
            String description = json.optString("description", "");
            String modelUrl = json.optString("model_url", "");

            resultTextView.append("\n\n標題: " + title + "\n介紹: " + description);

            if (!modelUrl.isEmpty() && (modelUrl.endsWith(".glb") || modelUrl.endsWith(".gltf"))) {
                //resultTextView.append("\n\n模型 URL: " + modelUrl);

                // 加載模型
                loadModelToScene(modelUrl);

            } else {
                Toast.makeText(this, "URL 不是有效的 glb/gltf 模型", Toast.LENGTH_LONG).show();
            }

        } catch (JSONException e) {
            Toast.makeText(this, "JSON 解析錯誤", Toast.LENGTH_LONG).show();
            Log.e("JSON", "解析錯誤", e);
        }
    }

    private void loadModelToScene(String modelUrl) {
        ModelLoaderWrapper modelLoader = new ModelLoaderWrapper(sceneView, this);
        modelLoader.loadModel(
                modelUrl,
                modelInstance -> {
                    sceneView.addChildNode(modelInstance);
                    //sceneView.setSkybox();
                    sceneView.setBackgroundColor(Color.TRANSPARENT);
                    sceneView.clearAnimation();
                    sceneView.invalidate();


                    modelInstance.setPosition(new Float3(0f, -1f, -8f));
                    modelInstance.setScale(new Float3(1f, 1f, 1f));
                    modelInstance.setRotation(new Float3(0, 0, 0));

                    Log.i("SceneView", "模型加載完成");
                },
                error -> {
                    Toast.makeText(MainActivity.this, "模型加載失敗：" + error.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("SceneView", "模型載入失敗", error);
                }
        );


    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        sceneView.destroy();
    }
}