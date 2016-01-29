package com.locosoft.imageselector;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.ListPopupWindow;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.locosoft.imageselector.utils.FakeR;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.locosoft.imageselector.adapter.FolderAdapter;
import com.locosoft.imageselector.adapter.ImageGridAdapter;
import com.locosoft.imageselector.bean.Folder;
import com.locosoft.imageselector.bean.Image;
import com.locosoft.imageselector.utils.FileUtils;
import com.locosoft.imageselector.utils.TimeUtils;

/**
 * MultiImageSelectorFragment
 * Created by Nereo on 2015/4/7.
 */
public class MultiImageSelectorFragment extends Fragment {

    private static final String TAG = "MultiImageSelector";


    public static final String EXTRA_SELECT_COUNT = "max_select_count";

    public static final String EXTRA_SELECT_MODE = "select_count_mode";

    public static final String EXTRA_SHOW_CAMERA = "show_camera";

    public static final String EXTRA_DEFAULT_SELECTED_LIST = "default_result";
    /** single selection */
    public static final int MODE_SINGLE = 0;
    /** multi-selection */
    public static final int MODE_MULTI = 1;
    // different loader definitions
    private static final int LOADER_ALL = 0;
    private static final int LOADER_CATEGORY = 1;

    private static final int REQUEST_CAMERA = 100;



    private ArrayList<String> resultList = new ArrayList<String>();

    private ArrayList<Folder> mResultFolder = new ArrayList<Folder>();

    // Images grid view
    private GridView mGridView;
    private Callback mCallback;

    private ImageGridAdapter mImageAdapter;
    private FolderAdapter mFolderAdapter;

    private ListPopupWindow mFolderPopupWindow;


    private TextView mTimeLineText;

    private TextView mCategoryText;

    private Button mPreviewBtn;

    private View mPopupAnchorView;

    private int mDesireImageCount;

    private boolean hasFolderGened = false;
    private boolean mIsShowCamera = false;

    private int mGridWidth, mGridHeight;

    private File mTmpFile;

    private FakeR mFakeR;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (Callback) activity;
        }catch (ClassCastException e){
            throw new ClassCastException("The Activity must implement MultiImageSelectorFragment.Callback interface...");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mFakeR = new FakeR(this.getActivity());
        return inflater.inflate(mFakeR.getId("layout", "fragment_multi_image"), container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get the desired image count
        mDesireImageCount = getArguments().getInt(EXTRA_SELECT_COUNT);

        // Get the image selection mode
        final int mode = getArguments().getInt(EXTRA_SELECT_MODE);

        // Default mode is the multi-selection
        if(mode == MODE_MULTI) {
            ArrayList<String> tmp = getArguments().getStringArrayList(EXTRA_DEFAULT_SELECTED_LIST);
            if(tmp != null && tmp.size()>0) {
                resultList = tmp;
            }
        }

        // Show the camera icon, if user allow to add new pictures using camera
        mIsShowCamera = getArguments().getBoolean(EXTRA_SHOW_CAMERA, true);
        mImageAdapter = new ImageGridAdapter(getActivity(), mIsShowCamera);
        // If it is multi-selection mode, show the selection checkbox
        mImageAdapter.showSelectIndicator(mode == MODE_MULTI);

        mPopupAnchorView = view.findViewById(mFakeR.getId("id", "imageselector_footer"));

        mTimeLineText = (TextView) view.findViewById(mFakeR.getId("id", "imageselector_timeline_area"));
        // initialize phase, hide the timeline text first
        mTimeLineText.setVisibility(View.GONE);

        mCategoryText = (TextView) view.findViewById(mFakeR.getId("id", "imageselector_category_btn"));
        // load all images
        mCategoryText.setText(mFakeR.getId("string", "imageselector_folder_all"));
        mCategoryText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(mFolderPopupWindow == null){
                    createPopupFolderList(mGridWidth, mGridHeight);
                }

                if (mFolderPopupWindow.isShowing()) {
                    mFolderPopupWindow.dismiss();
                } else {
                    mFolderPopupWindow.show();
                    int index = mFolderAdapter.getSelectIndex();
                    index = index == 0 ? index : index - 1;
                    mFolderPopupWindow.getListView().setSelection(index);
                }
            }
        });

        mPreviewBtn = (Button) view.findViewById(mFakeR.getId("id", "imageselector_preview"));
        // reset all buttons' status
        if(resultList == null || resultList.size()<=0){
            mPreviewBtn.setText(mFakeR.getId("id", "imageselector_preview"));
            mPreviewBtn.setEnabled(false);
        }
        mPreviewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO Preview
            }
        });

        mGridView = (GridView) view.findViewById(mFakeR.getId("id", "imageselector_grid"));
        mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int state) {

                final Picasso picasso = Picasso.with(getActivity());
                if(state == SCROLL_STATE_IDLE || state == SCROLL_STATE_TOUCH_SCROLL){
                    picasso.resumeTag(getActivity());
                }else{
                    picasso.pauseTag(getActivity());
                }

                if(state == SCROLL_STATE_IDLE){
                    // when the scrolling is stopped, hide the timeline
                    mTimeLineText.setVisibility(View.GONE);
                }else if(state == SCROLL_STATE_FLING){
                    mTimeLineText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if(mTimeLineText.getVisibility() == View.VISIBLE) {
                    int index = firstVisibleItem + 1 == view.getAdapter().getCount() ? view.getAdapter().getCount() - 1 : firstVisibleItem + 1;
                    Image image = (Image) view.getAdapter().getItem(index);
                    if (image != null) {
                        mTimeLineText.setText(TimeUtils.formatPhotoDate(image.path));
                    }
                }
            }
        });
        mGridView.setAdapter(mImageAdapter);
        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            public void onGlobalLayout() {

                final int width = mGridView.getWidth();
                final int height = mGridView.getHeight();

                mGridWidth = width;
                mGridHeight = height;

                final int desireSize = getResources().getDimensionPixelOffset(mFakeR.getId("dimen", "image_size"));
                final int numCount = width / desireSize;
                final int columnSpace = getResources().getDimensionPixelOffset(mFakeR.getId("dimen", "space_size"));
                int columnWidth = (width - columnSpace*(numCount-1)) / numCount;
                mImageAdapter.setItemSize(columnWidth);

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                    mGridView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }else{
                    mGridView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(mImageAdapter.isShowCamera()){
                    // If using camera, the first grid will be the camera icon and dealing with camra action
                    if(i == 0){
                        showCameraAction();
                    }else{
                        // The rest of grid are images' click action
                        Image image = (Image) adapterView.getAdapter().getItem(i);
                        selectImageFromGrid(image, mode);
                    }
                }else{
                    // images' click action
                    Image image = (Image) adapterView.getAdapter().getItem(i);
                    selectImageFromGrid(image, mode);
                }
            }
        });

        mFolderAdapter = new FolderAdapter(getActivity());
    }

    /**
     * Create popup window with ListView
     */
    private void createPopupFolderList(int width, int height) {
        mFolderPopupWindow = new ListPopupWindow(getActivity());
        mFolderPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mFolderPopupWindow.setAdapter(mFolderAdapter);
        mFolderPopupWindow.setContentWidth(width);
        mFolderPopupWindow.setWidth(width);
        mFolderPopupWindow.setHeight(height * 5 / 8);
        mFolderPopupWindow.setAnchorView(mPopupAnchorView);
        mFolderPopupWindow.setModal(true);
        mFolderPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                mFolderAdapter.setSelectIndex(i);

                final int index = i;
                final AdapterView v = adapterView;

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mFolderPopupWindow.dismiss();

                        if (index == 0) {
                            getActivity().getSupportLoaderManager().restartLoader(LOADER_ALL, null, mLoaderCallback);
                            mCategoryText.setText(mFakeR.getId("string", "imageselector_folder_all"));
                            if (mIsShowCamera) {
                                mImageAdapter.setShowCamera(true);
                            } else {
                                mImageAdapter.setShowCamera(false);
                            }
                        } else {
                            Folder folder = (Folder) v.getAdapter().getItem(index);
                            if (null != folder) {
                                mImageAdapter.setData(folder.images);
                                mCategoryText.setText(folder.name);
                                // Set the default selection
                                if (resultList != null && resultList.size() > 0) {
                                    mImageAdapter.setDefaultSelected(resultList);
                                }
                            }
                            mImageAdapter.setShowCamera(false);
                        }

                        // scroll to top position
                        mGridView.smoothScrollToPosition(0);
                    }
                }, 100);

            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // loading all images at first time
        //new LoadImageTask().execute();
        getActivity().getSupportLoaderManager().initLoader(LOADER_ALL, null, mLoaderCallback);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // when completed the camera action, return the photo's path
        if(requestCode == REQUEST_CAMERA){
            if(resultCode == Activity.RESULT_OK) {
                if (mTmpFile != null) {

                    String path = mTmpFile.getPath();
                    String name = mTmpFile.getName();
                    long dateTime = mTmpFile.lastModified();
                    Image image = new Image(path, name, dateTime);
                    image.rotation = MultiImageSelectorFragment.getImageRotation(path);

                    if( !hasFolderGened ) {
                        // Fetch the folder's name
                        File imageFile = new File(path);
                        File folderFile = imageFile.getParentFile();
                        Folder folder = new Folder();
                        folder.name = folderFile.getName();
                        folder.path = folderFile.getAbsolutePath();
                        folder.cover = image;
                        if (!mResultFolder.contains(folder)) {
                            List<Image> imageList = new ArrayList<Image>();
                            imageList.add(image);
                            folder.images = imageList;
                            mResultFolder.add(folder);
                        } else {
                            // Update
                            Folder f = mResultFolder.get(mResultFolder.indexOf(folder));
                            f.images.add(image);
                        }
                    }
                    mImageAdapter.addData(image);
                    mFolderAdapter.setData(mResultFolder);

                    if (mCallback != null) {
                        mCallback.onCameraShot(mTmpFile);
                    }
                }
            }else{
                if(mTmpFile != null && mTmpFile.exists()){
                    mTmpFile.delete();
                }
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "on change");

        if(mFolderPopupWindow != null){
            if(mFolderPopupWindow.isShowing()){
                mFolderPopupWindow.dismiss();
            }
        }

        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            public void onGlobalLayout() {

                final int height = mGridView.getHeight();

                final int desireSize = getResources().getDimensionPixelOffset(mFakeR.getId("dimen", "image_size"));
                Log.d(TAG, "Desire Size = " + desireSize);
                final int numCount = mGridView.getWidth() / desireSize;
                Log.d(TAG, "Grid Size = " + mGridView.getWidth());
                Log.d(TAG, "num count = " + numCount);
                final int columnSpace = getResources().getDimensionPixelOffset(mFakeR.getId("dimen", "space_size"));
                int columnWidth = (mGridView.getWidth() - columnSpace * (numCount - 1)) / numCount;
                mImageAdapter.setItemSize(columnWidth);

                if (mFolderPopupWindow != null) {
                    mFolderPopupWindow.setHeight(height * 5 / 8);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mGridView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    mGridView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });

        super.onConfigurationChanged(newConfig);

    }

    /**
     * Dealing with camera action
     */
    private void showCameraAction() {
        // Invoke the android system camera intent
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(cameraIntent.resolveActivity(getActivity().getPackageManager()) != null){
            // Setting the image output directory
            // create temporary file
            mTmpFile = FileUtils.createTmpFile(getActivity());
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTmpFile));
            startActivityForResult(cameraIntent, REQUEST_CAMERA);
        }else{
            Toast.makeText(getActivity(), mFakeR.getId("string", "imageselector_msg_no_camera"), Toast.LENGTH_SHORT).show();
        }
    }

    public static int getImageRotation(String path) {
        int degree  = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    /**
     * Image selection from grid
     * @param image
     */
    private void selectImageFromGrid(Image image, int mode) {
        if(image != null) {
            // muti-selection mode
            if(mode == MODE_MULTI) {
                if (resultList.contains(image.path)) {
                    resultList.remove(image.path);
                    if(resultList.size() != 0) {
                        mPreviewBtn.setEnabled(true);
                        mPreviewBtn.setText(getResources().getString(mFakeR.getId("string", "imageselector_preview")) + "(" + resultList.size() + ")");
                    }else{
                        mPreviewBtn.setEnabled(false);
                        mPreviewBtn.setText(mFakeR.getId("string", "imageselector_preview"));
                    }
                    if (mCallback != null) {
                        mCallback.onImageUnselected(image);
                    }
                } else {
                    // does the amount of selection reached the desired value
                    if(mDesireImageCount == resultList.size()){
                        Toast.makeText(getActivity(), mFakeR.getId("string", "imageselector_msg_amount_limit"), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    resultList.add(image.path);
                    mPreviewBtn.setEnabled(true);
                    mPreviewBtn.setText(getResources().getString(mFakeR.getId("string", "imageselector_preview")) + "(" + resultList.size() + ")");
                    if (mCallback != null) {
                        mCallback.onImageSelected(image);
                    }
                }
                mImageAdapter.select(image);
            }else if(mode == MODE_SINGLE){
                // Single selection mode
                if(mCallback != null){
                    mCallback.onSingleImageSelected(image);
                }
            }
        }
    }

    private LoaderManager.LoaderCallbacks<Cursor> mLoaderCallback = new LoaderManager.LoaderCallbacks<Cursor>() {

        private final String[] IMAGE_PROJECTION = {
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media._ID };

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if(id == LOADER_ALL) {
                CursorLoader cursorLoader = new CursorLoader(getActivity(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_PROJECTION,
                        null, null, IMAGE_PROJECTION[2] + " DESC");
                return cursorLoader;
            }else if(id == LOADER_CATEGORY){
                CursorLoader cursorLoader = new CursorLoader(getActivity(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_PROJECTION,
                        IMAGE_PROJECTION[0]+" like '%"+args.getString("path")+"%'", null, IMAGE_PROJECTION[2] + " DESC");
                return cursorLoader;
            }

            return null;
        }



        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data != null) {
                List<Image> images = new ArrayList<Image>();
                int count = data.getCount();
                if (count > 0) {
                    data.moveToFirst();
                    do{
                        String path = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[0]));
                        String name = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[1]));
                        long dateTime = data.getLong(data.getColumnIndexOrThrow(IMAGE_PROJECTION[2]));
                        Image image = new Image(path, name, dateTime);
                        image.rotation = MultiImageSelectorFragment.getImageRotation(path);
                        images.add(image);

                        if( !hasFolderGened ) {
                            // Fetch the folder's name
                            File imageFile = new File(path);
                            File folderFile = imageFile.getParentFile();
                            Folder folder = new Folder();
                            folder.name = folderFile.getName();
                            folder.path = folderFile.getAbsolutePath();
                            folder.cover = image;
                            if (!mResultFolder.contains(folder)) {
                                List<Image> imageList = new ArrayList<Image>();
                                imageList.add(image);
                                folder.images = imageList;
                                mResultFolder.add(folder);
                            } else {
                                // Update
                                Folder f = mResultFolder.get(mResultFolder.indexOf(folder));
                                f.images.add(image);
                            }
                        }

                    }while(data.moveToNext());

                    mImageAdapter.setData(images);

                    // Set the default selection
                    if(resultList != null && resultList.size()>0){
                        mImageAdapter.setDefaultSelected(resultList);
                    }

                    mFolderAdapter.setData(mResultFolder);
                    hasFolderGened = true;

                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }
    };

    /**
     * Callback interface
     */
    public interface Callback{
        void onSingleImageSelected(Image image);
        void onImageSelected(Image image);
        void onImageUnselected(Image image);
        void onCameraShot(File imageFile);
    }
}
