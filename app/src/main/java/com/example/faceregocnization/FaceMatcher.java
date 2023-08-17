package com.example.faceregocnization;

/**
 * Created by Md Minhajul Islam on 17/08/2023.
 */
import android.graphics.Bitmap;
import android.graphics.PointF;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.ArrayList;
import java.util.List;

public class FaceMatcher {
    boolean isMatched;
    private final FaceDetector faceDetector;
    private static final float SIMILARITY_THRESHOLD = 0.7f;

    public FaceMatcher() {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build();
        faceDetector = com.google.mlkit.vision.face.FaceDetection.getClient(options);
    }

    public Task<List<Face>> detectFaces(Bitmap bitmap) {
        InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
        return faceDetector.process(inputImage);
    }



    // Calculate cosine similarity between two feature vectors
    private float calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        if (normA == 0.0f || normB == 0.0f) {
            return 0.0f; // Handle division by zero
        }

        float similarity = dotProduct / (float)(Math.sqrt(normA) * Math.sqrt(normB));
        return similarity;
    }

    // Usage

    public boolean compareFaces(Bitmap sourceBitmap, Bitmap targetBitmap) {

        Task<List<Face>> sourceFacesTask = detectFaces(sourceBitmap);
        Task<List<Face>> targetFacesTask = detectFaces(targetBitmap);

        Tasks.whenAllComplete(sourceFacesTask, targetFacesTask)
                .addOnCompleteListener(new OnCompleteListener<List<Task<?>>>() {
                    @Override
                    public void onComplete(@NonNull Task<List<Task<?>>> task) {
                        List<Face> sourceFaces = sourceFacesTask.getResult();
                        List<Face> targetFaces = targetFacesTask.getResult();

                        if (!sourceFaces.isEmpty() && !targetFaces.isEmpty()) {
                            float[] sourceFeatures = FaceMatcher.this.extractFeatures(sourceFaces.get(0));
                            float[] targetFeatures = FaceMatcher.this.extractFeatures(targetFaces.get(0));

                            float similarityScore = calculateCosineSimilarity(sourceFeatures, targetFeatures);

                            // The faces are considered a match
                            // Perform appropriate action here
                            // The faces are not a match
                            // Perform appropriate action here
                            isMatched = similarityScore > SIMILARITY_THRESHOLD;
                        }
                    }
                });
        return isMatched;
    }

    private float[] extractFeatures(Face face) {
        // Extract facial landmarks, descriptors, or other features
        // and convert them to a feature vector
        // Example: Use face.getLandmarks() to get face landmarks

        // For simplicity, let's use random features as an example
        float[] features = new float[128];
        for (int i = 0; i < 128; i++) {
            features[i] = (float) Math.random();
        }

        return features;
    }

    private PointF[] extractLandmarks(Face face) {
        PointF[] landmarks = new PointF[5]; // 5 selected landmarks

        // Extract facial landmarks from the detected face
        landmarks[0] = convertFirebasePointToPointF(face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE).getPosition());
        landmarks[1] = convertFirebasePointToPointF(face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE).getPosition());
        landmarks[2] = convertFirebasePointToPointF(face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE).getPosition());
        landmarks[3] = convertFirebasePointToPointF(face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_LEFT).getPosition());
        landmarks[4] = convertFirebasePointToPointF(face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_RIGHT).getPosition());

        return landmarks;
    }

    private PointF convertFirebasePointToPointF(PointF pointF) {
        return new PointF(pointF.x, pointF.y);
    }

    // Helper method to convert FirebaseVisionPoint to PointF
    private PointF convertFirebasePointToPointF(FirebaseVisionPoint firebasePoint) {
        return new PointF(firebasePoint.getX(), firebasePoint.getY());
    }
}


