package com.locosoft.imageselector;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;

import java.util.ArrayList;

/**
 * Created by jeremy on 1/29/16.
 */
public class MultiImageSelectorModule extends ReactContextBaseJavaModule {
    private static final int REQUEST_IMAGE = 2;
    private ReactApplicationContext mReactContext;
    private Activity mActivity;
    private Callback mCallback;
    private ArrayList<String> mSelectPaths;
    private String mError;

    public MultiImageSelectorModule(ReactApplicationContext context, Activity activity) {
        super(context);
        mReactContext = context;
        mActivity = activity;
    }


    @Override
    public String getName() {
        return "ImageSelectorManager";
    }

    @ReactMethod
    public void launch(ReadableMap options, Callback callback) {
        mCallback = callback;

        int selectedMode = MultiImageSelectorActivity.MODE_MULTI;

        boolean showCamera = true;

        int maxNum = 9;

        if(options.hasKey("singleMode")) {
            boolean isSingleMode = options.getBoolean("singleMode");

            if (isSingleMode) {
                selectedMode = MultiImageSelectorActivity.MODE_SINGLE;
            } else {
                selectedMode = MultiImageSelectorActivity.MODE_MULTI;
            }
        }
        if(options.hasKey("showCamera")) {
            showCamera = options.getBoolean("showCamera");
        }
        if(options.hasKey("max")) {
            maxNum = options.getInt("max");
        }



        Intent intent = new Intent(mActivity, MultiImageSelectorActivity.class);

        intent.putExtra(MultiImageSelectorActivity.EXTRA_SHOW_CAMERA, showCamera);

        intent.putExtra(MultiImageSelectorActivity.EXTRA_SELECT_COUNT, maxNum);

        intent.putExtra(MultiImageSelectorActivity.EXTRA_SELECT_MODE, selectedMode);

        mActivity.startActivityForResult(intent, REQUEST_IMAGE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_IMAGE){
            if(resultCode == Activity.RESULT_OK){
                mSelectPaths = data.getStringArrayListExtra(MultiImageSelectorActivity.EXTRA_RESULT);
                WritableArray resultArray = new WritableNativeArray();
                for(String p: mSelectPaths){
                    resultArray.pushString(p);
                }
                mCallback.invoke(null, resultArray);
            } else if (resultCode == Activity.RESULT_CANCELED && data != null) {
                mError = data.getStringExtra(MultiImageSelectorActivity.ERROR_RESULT);
                mCallback.invoke(mError, null);
            }
        }
    }

}
