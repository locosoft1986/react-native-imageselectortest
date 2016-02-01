package com.locosoft.imageselector;

import android.app.Activity;
import android.content.Intent;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.List;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by jeremy on 1/29/16.
 */
public class MultiImageSelectorPackage implements ReactPackage {
    private Activity mActivity;
    private MultiImageSelectorModule mModuleInstance;

    public MultiImageSelectorPackage(Activity activity) {
        this.mActivity = activity;
    }

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactApplicationContext) {
        mModuleInstance = new MultiImageSelectorModule(reactApplicationContext, this.mActivity);
        return Arrays.<NativeModule>asList(
                mModuleInstance
        );
    }

    @Override
    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactApplicationContext) {
        return Collections.EMPTY_LIST;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(mModuleInstance != null) {
            mModuleInstance.onActivityResult(requestCode, resultCode, data);
        }
    }
}
