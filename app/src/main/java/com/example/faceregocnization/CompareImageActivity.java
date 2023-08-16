package com.example.faceregocnization;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.TensorFlowLite;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageOperator;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

public class CompareImageActivity extends AppCompatActivity {

    private static final String TAG = CompareImageActivity.class.getSimpleName();
    private Interpreter tflite;
    private int imageSizeX;
    private int imageSizeY;
    private boolean isEnabled = false;

    private static final float IMAGE_MEAN = 0.0f;
    private static final float IMAGE_STD = 1.0f;

    public Bitmap oribitMap, testbitmap;
    public static Bitmap cropped;
    private Uri imageUri;

    private ImageView oriImage, testImage;
    private Button buverify;
    private TextView result_text;

    private float[][] ori_embedding = new float[1][128];
    private float[][] test_embedding = new float[1][128];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare_image);
        initComponents();
    }

    private void initComponents() {
        oriImage = findViewById(R.id.img1);
        testImage = findViewById(R.id.img2);
        buverify = findViewById(R.id.btnCompare);
        result_text = findViewById(R.id.resultTxt);

        try {
            MappedByteBuffer tfliteModel = loadModelFile();
            Interpreter.Options tfliteOptions = new Interpreter.Options();
            tflite = new Interpreter(tfliteModel, tfliteOptions);
        } catch (Exception e) {
            e.printStackTrace();
        }

        oriImage.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picturres"), 112);
        });

        testImage.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picturres"), 113);
        });

        buverify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double distance = calculate_distance(ori_embedding, test_embedding);

                if (distance < 6.0) {
                    result_text.setText("Result : same faces");
                } else {
                    result_text.setText("Result : not matched");
                }
            }
        });

    }


    private double calculate_distance(float[][] ori_embedding, float[][] test_embedding) {
        double sum =0.0;
        for (int i=0;i<128;i++) {
            sum = sum + Math.pow((ori_embedding[0][i] - test_embedding[0][i]), 2.0);
        }
        return Math.sqrt(sum);
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        // Load your .tflite model file from assets
        AssetFileDescriptor fileDescriptor = getAssets().openFd("facenet_int_quantized.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 112 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            try {
                oribitMap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                oriImage.setImageBitmap(oribitMap);
                face_detector(oribitMap, "original");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (requestCode == 113 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            try {
                testbitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                testImage.setImageBitmap(testbitmap);
                face_detector(testbitmap, "test");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void face_detector(Bitmap bitmap, String imageType) {
        final InputImage image = InputImage.fromBitmap(bitmap, 0);
        FaceDetector faceDetector = FaceDetection.getClient();
        faceDetector.process(image).addOnSuccessListener(new OnSuccessListener<List<Face>>() {
            @Override
            public void onSuccess(List<Face> faces) {
                for (Face face : faces) {
                    Rect bounds = face.getBoundingBox();
                    cropped = Bitmap.createBitmap(bitmap, bounds.left, bounds.top, bounds.width(), bounds.height());
                    get_embaddings(cropped, imageType);
                    if (imageType.equals("test")){
                        buverify.setEnabled(true);
                    }else {
                        Toast.makeText(CompareImageActivity.this, "wait...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(CompareImageActivity.this, "wait...", Toast.LENGTH_SHORT).show();
            }
        });

//        FirebaseVisionFaceDetectorOptions options =
//                new FirebaseVisionFaceDetectorOptions.Builder()
//                        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
//                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
//                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
//                        .build();

      //  FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
       // FirebaseVisionFaceDetector detector = FirebaseVision.getInstance().getVisionFaceDetector(options);
//               faceDetector.pr(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
//                    @Override
//                    public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
//                        for (FirebaseVisionFace face : firebaseVisionFaces) {
//                            Rect bounds = face.getBoundingBox();
//                            cropped = Bitmap.createBitmap(bitmap, bounds.left, bounds.top, bounds.width(), bounds.height());
//                            get_embaddings(cropped, imageType);
//                            if (imageType.equals("test")){
//                                buverify.setEnabled(true);
//                            }else {
//                                Toast.makeText(CompareImageActivity.this, "wait...", Toast.LENGTH_SHORT).show();
//                            }
//                        }
//
//                    }
//                }) // adding an onfailure listener as well if
//                // something goes wrong.
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
    }

    private void get_embaddings(Bitmap bitmap, String imageType) {
        TensorImage inputImageBuffer;
        float[][] embedding = new float[1][128];
        int imageTensorIndex = 0;
        int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape();

        imageSizeY = imageShape[1];
        imageSizeX = imageShape[2];
        DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();

        inputImageBuffer = new TensorImage(imageDataType);
        inputImageBuffer = loadImage(bitmap, inputImageBuffer);

        tflite.run(inputImageBuffer.getBuffer(), embedding);

        if (imageType.equals("original")) {
            ori_embedding = embedding;
        } else if (imageType.equals("test")) {
            test_embedding = embedding;
        }

    }

    private TensorImage loadImage(final Bitmap bitmap, TensorImage inputImageBuffer) { // Loads bitmap into a TensorImage.
        inputImageBuffer.load(bitmap);    // Creates processor for the TensorImage.
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        // TODO (b/143564309): Fuse ops inside ImageProcessor.
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                        .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                        .add(getPreprocessNormalizeOp())
                        .build();
        return imageProcessor.process(inputImageBuffer);
    }

    private TensorOperator getPreprocessNormalizeOp() {
        return new NormalizeOp(IMAGE_MEAN, IMAGE_STD);
    }


}