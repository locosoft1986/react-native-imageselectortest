package com.imagepickerapp;

import android.content.Intent;

import com.facebook.react.ReactActivity;
import com.facebook.react.ReactPackage;
import com.facebook.react.shell.MainReactPackage;

import java.util.Arrays;
import java.util.List;
import com.locosoft.imageselector.MultiImageSelectorPackage;

public class MainActivity extends ReactActivity {
    private MultiImageSelectorPackage mImageSelectorPackage;
    /**
     * Returns the name of the main component registered from JavaScript.
     * This is used to schedule rendering of the component.
     */
    @Override
    protected String getMainComponentName() {
        return "ImagePickerApp";
    }

    /**
     * Returns whether dev mode should be enabled.
     * This enables e.g. the dev menu.
     */
    @Override
    protected boolean getUseDeveloperSupport() {
        return BuildConfig.DEBUG;
    }

   /**
   * A list of packages used by the app. If the app uses additional views
   * or modules besides the default ones, add more packages here.
   */
    @Override
    protected List<ReactPackage> getPackages() {
        mImageSelectorPackage = new MultiImageSelectorPackage(this);
        return Arrays.<ReactPackage>asList(
            new MainReactPackage(),
            mImageSelectorPackage
        );
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mImageSelectorPackage.onActivityResult(requestCode, resultCode, data);
    }
}
