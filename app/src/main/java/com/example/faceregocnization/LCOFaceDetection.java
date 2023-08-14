package com.example.faceregocnization;

import android.app.Application;

import com.google.firebase.FirebaseApp;

/**
 * Created by Md Minhajul Islam on 14/08/2023.
 */
public class LCOFaceDetection extends Application {
    public final static String RESULT_TEXT = "RESULT_TEXT";
    public final static String RESULT_DIALOG = "RESULT_DIALOG";

    // initializing our firebase
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}
