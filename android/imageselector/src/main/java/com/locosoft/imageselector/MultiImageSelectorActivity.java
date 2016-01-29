package com.locosoft.imageselector;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;

import com.locosoft.imageselector.bean.Image;
import com.locosoft.imageselector.utils.FakeR;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * MultiImageSelectorActivity
 * Created by Nereo on 2015/4/7.
 */
public class MultiImageSelectorActivity extends FragmentActivity implements MultiImageSelectorFragment.Callback{

    /** Max image selection amount, default is 9 */
    public static final String EXTRA_SELECT_COUNT = "max_select_count";
    /** Image selection mode, default is multi-selection */
    public static final String EXTRA_SELECT_MODE = "select_count_mode";
    /** Show camera or not, default is shown */
    public static final String EXTRA_SHOW_CAMERA = "show_camera";
	/** Desired image width that user want to scale to, default is 0 **/
	public static final String EXTRA_WIDTH = "desired_width";
	/** Desired image height that user want to scale to, default is 0  **/
	public static final String EXTRA_HEIGHT = "desired_height";
	/** Desired image height that user want to compress, default is 100  **/
	public static final String EXTRA_QUALITY = "quality";
	/** Fix the image rotation problem before the result paths send to user, default is false  **/
	public static final String EXTRA_FIXROTATION = "fix_rotation";
	
    /** Result，should be an ArrayList<String>, the image path collection  */
    public static final String EXTRA_RESULT = "select_result";
	/** Error message for debug **/
    public static final String ERROR_RESULT = "error_message";
    /** default selection list */
    public static final String EXTRA_DEFAULT_SELECTED_LIST = "default_list";

    /** Single-Selection mode */
    public static final int MODE_SINGLE = 0;
    /** Multi-Selection mode */
    public static final int MODE_MULTI = 1;

    private ArrayList<String> resultList = new ArrayList<String>();
    private Map<String, Integer> fileNames2RotationMap = new HashMap<String, Integer>();
    private Button mSubmitButton;
    private int mDefaultCount;

    private FakeR mFakeR;

    private int desiredWidth = 0;
    private int desiredHeight = 0;
    private int quality = 100;
    private boolean fixRotation = false;

    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFakeR = new FakeR(this);
        setContentView(mFakeR.getId("layout", "activity_image_selector"));

        Intent intent = getIntent();
        mDefaultCount = intent.getIntExtra(EXTRA_SELECT_COUNT, 9);
        int mode = intent.getIntExtra(EXTRA_SELECT_MODE, MODE_MULTI);
        boolean isShow = intent.getBooleanExtra(EXTRA_SHOW_CAMERA, true);
        if(mode == MODE_MULTI && intent.hasExtra(EXTRA_DEFAULT_SELECTED_LIST)) {
            resultList = intent.getStringArrayListExtra(EXTRA_DEFAULT_SELECTED_LIST);
        }
		
		desiredWidth = intent.getIntExtra(EXTRA_WIDTH, 0);
		desiredHeight = intent.getIntExtra(EXTRA_HEIGHT, 0);
		quality = intent.getIntExtra(EXTRA_QUALITY, 0);
		fixRotation = intent.getBooleanExtra(EXTRA_FIXROTATION, false);

        Bundle bundle = new Bundle();
        bundle.putInt(MultiImageSelectorFragment.EXTRA_SELECT_COUNT, mDefaultCount);
        bundle.putInt(MultiImageSelectorFragment.EXTRA_SELECT_MODE, mode);
        bundle.putBoolean(MultiImageSelectorFragment.EXTRA_SHOW_CAMERA, isShow);
        bundle.putStringArrayList(MultiImageSelectorFragment.EXTRA_DEFAULT_SELECTED_LIST, resultList);

        getSupportFragmentManager().beginTransaction()
                .add(mFakeR.getId("id", "imageselector_image_grid"), Fragment.instantiate(this, MultiImageSelectorFragment.class.getName(), bundle))
                .commit();

        // back button
        findViewById(mFakeR.getId("id", "imageselector_btn_back")).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        // done button
        mSubmitButton = (Button) findViewById(mFakeR.getId("id", "imageselector_commit"));
        if(resultList == null || resultList.size()<=0){
            mSubmitButton.setText(mFakeR.getId("string", "imageselector_done"));
            mSubmitButton.setEnabled(false);
        }else{
            mSubmitButton.setText(
                    getResources().getString(mFakeR.getId("string", "imageselector_done"))
                            + "("+resultList.size()+"/"+mDefaultCount+")"
            );

            mSubmitButton.setEnabled(true);
        }
        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(resultList != null && resultList.size() >0){
                    // return the selection array list
                    if(desiredHeight == 0 && desiredWidth == 0 && quality == 100 && !fixRotation) {
                        Intent data = new Intent();
                        data.putStringArrayListExtra(EXTRA_RESULT, resultList);
                        setResult(RESULT_OK, data);
                        finish();
                    } else {
                        postProcessImages();
                    }
                }
            }
        });

        progress = new ProgressDialog(this);
        progress.setTitle(getResources().getString(mFakeR.getId("string", "imageselector_progress_title")));
        progress.setMessage(getResources().getString(mFakeR.getId("string", "imageselector_progress_content")));
    }

    @Override
    public void onSingleImageSelected(Image image) {
        String path = image.path;
        Intent data = new Intent();
        resultList.add(path);
        fileNames2RotationMap.put(image.path, image.rotation);

        if(desiredHeight == 0 && desiredWidth == 0 && quality == 100 && !fixRotation) {
            data.putStringArrayListExtra(EXTRA_RESULT, resultList);
            setResult(RESULT_OK, data);
            finish();
        } else {
            postProcessImages();
        }



    }

    @Override
    public void onImageSelected(Image image) {
        String path = image.path;
        if(!resultList.contains(path)) {
            resultList.add(path);

        }
        if(!fileNames2RotationMap.containsKey(path)) {
            fileNames2RotationMap.put(image.path, image.rotation);
        }
        // if there is any image selected, change the button status
        if(resultList.size() > 0){
            mSubmitButton.setText(
                    getResources().getString(mFakeR.getId("string", "imageselector_done"))
                        + "("+resultList.size()+"/"+mDefaultCount+")");
            if(!mSubmitButton.isEnabled()){
                mSubmitButton.setEnabled(true);
            }
        }
    }

    @Override
    public void onImageUnselected(Image image) {
        String path = image.path;
        if(resultList.contains(path)){
            resultList.remove(path);
            mSubmitButton.setText(
                    getResources().getString(mFakeR.getId("string", "imageselector_done"))
                        + "("+resultList.size()+"/"+mDefaultCount+")");
        }else{
            mSubmitButton.setText(
                    getResources().getString(mFakeR.getId("string", "imageselector_done"))
                            + "(" + resultList.size() + "/" + mDefaultCount + ")");
        }

        if(fileNames2RotationMap.containsKey(path)) {
            fileNames2RotationMap.remove(path);
        }
        // When there is no image selected, disable the Done button
        if(resultList.size() == 0){
            mSubmitButton.setText(mFakeR.getId("string", "imageselector_done"));
            mSubmitButton.setEnabled(false);
        }
    }

    @Override
    public void onCameraShot(File imageFile) {
        //Do not finish the activity, update the grid view only
        //Because in common scene, user has not finished the image choosing yet.
        /*if(imageFile != null) {
            Intent data = new Intent();
            resultList.add(imageFile.getAbsolutePath());
            data.putStringArrayListExtra(EXTRA_RESULT, resultList);
            setResult(RESULT_OK, data);
            finish();
        }*/
    }

    private void postProcessImages() {
        new ResizeImagesTask().execute(fileNames2RotationMap.entrySet());
    }


    private class ResizeImagesTask extends AsyncTask<Set<Map.Entry<String, Integer>>, Void, ArrayList<String>> {
        private Exception asyncTaskError = null;

        @Override
        protected ArrayList<String> doInBackground(Set<Map.Entry<String, Integer>>... fileSets) {
            Set<Map.Entry<String, Integer>> fileNames = fileSets[0];
            ArrayList<String> al = new ArrayList<String>();
            try {
                Iterator<Map.Entry<String, Integer>> i = fileNames.iterator();
                Bitmap bmp;
                while(i.hasNext()) {
                    Map.Entry<String, Integer> imageInfo = i.next();
                    File file = new File(imageInfo.getKey());
                    int rotate = imageInfo.getValue().intValue();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 1;
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                    int width = options.outWidth;
                    int height = options.outHeight;
                    float scale = calculateScale(width, height);
                    if (scale < 1) {
                        int finalWidth = (int)(width * scale);
                        int finalHeight = (int)(height * scale);
                        int inSampleSize = calculateInSampleSize(options, finalWidth, finalHeight);
                        options = new BitmapFactory.Options();
                        options.inSampleSize = inSampleSize;
                        try {
                            bmp = this.tryToGetBitmap(file, options, rotate, true);
                        } catch (OutOfMemoryError e) {
                            options.inSampleSize = calculateNextSampleSize(options.inSampleSize);
                            try {
                                bmp = this.tryToGetBitmap(file, options, rotate, false);
                            } catch (OutOfMemoryError e2) {
                                throw new IOException("Unable to load image into memory.");
                            }
                        }
                    } else {
                        try {
                            bmp = this.tryToGetBitmap(file, null, rotate, false);
                        } catch(OutOfMemoryError e) {
                            options = new BitmapFactory.Options();
                            options.inSampleSize = 2;
                            try {
                                bmp = this.tryToGetBitmap(file, options, rotate, false);
                            } catch(OutOfMemoryError e2) {
                                options = new BitmapFactory.Options();
                                options.inSampleSize = 4;
                                try {
                                    bmp = this.tryToGetBitmap(file, options, rotate, false);
                                } catch (OutOfMemoryError e3) {
                                    throw new IOException("Unable to load image into memory.");
                                }
                            }
                        }
                    }

                    file = this.storeImage(bmp, file.getName());
                    al.add(Uri.fromFile(file).toString());
                }
                return al;
            } catch(IOException e) {
                try {
                    asyncTaskError = e;
                    for (int i = 0; i < al.size(); i++) {
                        URI uri = new URI(al.get(i));
                        File file = new File(uri);
                        file.delete();
                    }
                } catch(Exception exception) {
                    // the finally does what we want to do
                } finally {
                    return new ArrayList<String>();
                }
            }
        }

        @Override
        protected void onPostExecute(ArrayList<String> al) {
            Intent data = new Intent();

            if (asyncTaskError != null) {
                Bundle res = new Bundle();
                res.putString(ERROR_RESULT, asyncTaskError.getMessage());
                data.putExtras(res);
                setResult(RESULT_CANCELED, data);
            } else if (al.size() > 0) {

                data.putStringArrayListExtra(EXTRA_RESULT, al);
                setResult(RESULT_OK, data);

            } else {
                setResult(RESULT_CANCELED, data);
            }

            progress.dismiss();
            finish();
        }

        private Bitmap tryToGetBitmap(File file, BitmapFactory.Options options, int rotate, boolean shouldScale) throws IOException, OutOfMemoryError {
            Bitmap bmp;
            if (options == null) {
                bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
            } else {
                bmp = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            }
            if (bmp == null) {
                throw new IOException("The image file could not be opened.");
            }
            if (options != null && shouldScale) {
                float scale = calculateScale(options.outWidth, options.outHeight);
                bmp = this.getResizedBitmap(bmp, scale);
            }
            if (rotate != 0) {
                Matrix matrix = new Matrix();
                matrix.setRotate(rotate);
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
            }
            return bmp;
        }

        /*
        * The following functions are originally from
        * https://github.com/raananw/PhoneGap-Image-Resizer
        *
        * They have been modified by Andrew Stephan for Sync OnSet
        *
        * The software is open source, MIT Licensed.
        * Copyright (C) 2012, webXells GmbH All Rights Reserved.
        */
        private File storeImage(Bitmap bmp, String fileName) throws IOException {
            int index = fileName.lastIndexOf('.');
            String name = fileName.substring(0, index);
            String ext = fileName.substring(index);
            File file = File.createTempFile(name, ext);
            OutputStream outStream = new FileOutputStream(file);
            if (ext.compareToIgnoreCase(".png") == 0) {
                bmp.compress(Bitmap.CompressFormat.PNG, quality, outStream);
            } else {
                bmp.compress(Bitmap.CompressFormat.JPEG, quality, outStream);
            }
            outStream.flush();
            outStream.close();
            return file;
        }

        private Bitmap getResizedBitmap(Bitmap bm, float factor) {
            int width = bm.getWidth();
            int height = bm.getHeight();
            // create a matrix for the manipulation
            Matrix matrix = new Matrix();
            // resize the bit map
            matrix.postScale(factor, factor);
            // recreate the new Bitmap
            Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
            return resizedBitmap;
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private int calculateNextSampleSize(int sampleSize) {
        double logBaseTwo = (int)(Math.log(sampleSize) / Math.log(2));
        return (int)Math.pow(logBaseTwo + 1, 2);
    }

    private float calculateScale(int width, int height) {
        float widthScale = 1.0f;
        float heightScale = 1.0f;
        float scale = 1.0f;
        if (desiredWidth > 0 || desiredHeight > 0) {
            if (desiredHeight == 0 && desiredWidth < width) {
                scale = (float)desiredWidth/width;
            } else if (desiredWidth == 0 && desiredHeight < height) {
                scale = (float)desiredHeight/height;
            } else {
                if (desiredWidth > 0 && desiredWidth < width) {
                    widthScale = (float)desiredWidth/width;
                }
                if (desiredHeight > 0 && desiredHeight < height) {
                    heightScale = (float)desiredHeight/height;
                }
                if (widthScale < heightScale) {
                    scale = widthScale;
                } else {
                    scale = heightScale;
                }
            }
        }

        return scale;
    }
}
