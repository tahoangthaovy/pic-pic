package com.tavy.picpic;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import ly.img.android.sdk.models.constant.Directory;
import ly.img.android.sdk.models.state.EditorLoadSettings;
import ly.img.android.sdk.models.state.EditorSaveSettings;
import ly.img.android.sdk.models.state.manager.SettingsList;
import ly.img.android.ui.activities.CameraPreviewBuilder;
import ly.img.android.ui.activities.ImgLyIntent;
import ly.img.android.ui.activities.PhotoEditorBuilder;
import ly.img.android.ui.utilities.PermissionRequest;

public class MainActivity extends AppCompatActivity implements PermissionRequest.Response {

    public class ImageAdapter extends BaseAdapter {

        private Context mContext;
        ArrayList<String> itemList = new ArrayList<String>();

        public ImageAdapter(Context c) {
            mContext = c;
        }

        void add(String path){
            itemList.add(path);
        }

        public String getPath(int position) {
            return  itemList.get(position);
        }
        @Override
        public int getCount() {
            return itemList.size();
        }

        @Override
        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {  // if it's not recycled, initialize some attributes
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(170, 170));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (ImageView) convertView;
            }

            Bitmap bm = decodedBitmapFromUri(itemList.get(position), 170, 170);
            bm = cropToSquare(bm);

            imageView.setImageBitmap(bm);
            return imageView;
        }

        public Bitmap decodedBitmapFromUri(String path, int reqWidth, int reqHeight) {

            Bitmap bm = null;
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSize(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            bm = BitmapFactory.decodeFile(path, options);

            return bm;
        }

        public int calculateInSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
            // Raw height and width of image
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {
                if (width > height) {
                    inSampleSize = Math.round((float)height / (float)reqHeight);
                } else {
                    inSampleSize = Math.round((float)width / (float)reqWidth);
                }
            }

            return inSampleSize;
        }

    }

    public static Bitmap cropToSquare(Bitmap bitmap){
        int width  = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = (height > width) ? width : height;
        int newHeight = (height > width)? height - ( height - width) : height;
        int cropW = (width - height) / 2;
        cropW = (cropW < 0)? 0: cropW;
        int cropH = (height - width) / 2;
        cropH = (cropH < 0)? 0: cropH;
        Bitmap cropImg = Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newHeight);

        return cropImg;
    }

    public static int CAMERA_PREVIEW_RESULT = 1;
    private static final String FOLDER = "PicPic";
    ImageAdapter myImageAdapter;
    Button openCameraViewButton;
    GridView gridview;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        openCameraViewButton = findViewById(R.id.openCameraViewButton);
        openCameraViewButton.setOnClickListener(clickHandler);
        gridview = findViewById(R.id.gridView);
        gridview.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent me) {

                int action = me.getActionMasked();
                float currentXPosition = me.getX();
                float currentYPosition = me.getY();
                int position = gridview.pointToPosition((int) currentXPosition, (int) currentYPosition);
                if (action == MotionEvent.ACTION_SCROLL) return false;
                if (action == MotionEvent.ACTION_UP) {
                    // Key was pressed here
                    if (position > -1) {
                        startEditor(myImageAdapter.getPath(position));
                        return true;
                    }
                }

                return false;
            }
        });
        loadPhotos();
    }

    View.OnClickListener clickHandler = new View.OnClickListener() {
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.openCameraViewButton:
                    startCamera();
                    break;

            }
        }
    };

    private void loadPhotos() {

        myImageAdapter = new ImageAdapter(this);
        gridview.setAdapter(myImageAdapter);

        String targetPath = getFolder();

        Toast.makeText(getApplicationContext(), targetPath, Toast.LENGTH_LONG).show();
        File targetDirector = new File(targetPath);
        if (!targetDirector.exists()) {
            targetDirector.mkdir();
        }
        else {
            File[] files = targetDirector.listFiles();
            if (files == null) return;
            for (File file : files){
                myImageAdapter.add(file.getAbsolutePath());
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();



    }

    private String getFolder() {
        String ExternalStorageDirectoryPath = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                .getAbsolutePath();

        String targetPath = ExternalStorageDirectoryPath +"/" + FOLDER;
        return targetPath;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == CAMERA_PREVIEW_RESULT) {
            String resultPath =
                    data.getStringExtra(ImgLyIntent.RESULT_IMAGE_PATH);


            String sourcePath =
                    data.getStringExtra(ImgLyIntent.SOURCE_IMAGE_PATH);

            if (resultPath != null) {
                // Scan result file
                File file =  new File(resultPath);
                Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(file);
                scanIntent.setData(contentUri);
                sendBroadcast(scanIntent);
            }

            if (sourcePath != null) {
                // Scan camera file
                File file =  new File(sourcePath);
                Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(file);
                scanIntent.setData(contentUri);
                sendBroadcast(scanIntent);
            }
            Toast.makeText(this, "Image Save on: " + resultPath, Toast.LENGTH_LONG).show();
            loadPhotos();
        }
    }
    //Important for Android 6.0 and above permisstion request, don't forget this!
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionRequest.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void permissionGranted() {

    }

    @Override
    public void permissionDenied() {
        // The Permission was rejected by the user, so the Editor was not opened because it can not save the result image.
        // TODO for you: Show a Hint to the User
    }

    // Start camera with editor access.
    public void startCamera() {
        SettingsList settingsList = new SettingsList();
        settingsList

                // Set custom editor export settings
                .getSettingsModel(EditorSaveSettings.class)
                .setExportDir(Directory.DCIM, FOLDER)
                .setExportPrefix("result_")
                .setSavePolicy(
                        EditorSaveSettings.SavePolicy.RETURN_ALWAYS_ONLY_OUTPUT
                );

        new CameraPreviewBuilder(this)
                .setSettingsList(settingsList)
                .startActivityForResult(this, CAMERA_PREVIEW_RESULT);
    }

    // Start editor only
    public void startEditor(String myPicture) {
        Uri imageUri = Uri.parse(myPicture);
        SettingsList settingsList = new SettingsList();
        settingsList
                .getSettingsModel(EditorLoadSettings.class)
                .setImageSourcePath(myPicture, false)
                .getSettingsModel(EditorSaveSettings.class)
                .setExportDir(Directory.DCIM, FOLDER)
                .setExportPrefix("result_")
                .setSavePolicy(
                        EditorSaveSettings.SavePolicy.KEEP_SOURCE_AND_CREATE_ALWAYS_OUTPUT
                );

        new PhotoEditorBuilder(this)
                .setSettingsList(settingsList)
                .startActivityForResult(this, CAMERA_PREVIEW_RESULT);
    }

    // ...
//    public void customizeMyConfig(SettingsList settingsList) {
//
//        settingsList.getConfig().setTools(
//                new CropEditorTool(R.string.imgly_tool_name_crop, R.drawable.icon_tool_crop),
//                new OrientationEditorTool(R.string.tool_name_rotate, R.drawable.imgly_icon_rotate),
//                new Divider(),
//                new FilterEditorTool(R.string.imgly_tool_name_filter, R.drawable.imgly_icon_tool_filters),
//                new ColorAdjustmentTool(R.string.imgly_tool_name_adjust, R.drawable.imgly_icon_tool_adjust),
//                new Divider(),
//                new TextEditorTool(R.string.imgly_tool_name_text, R.drawable.imgly_icon_tool_text),
//                new StickerEditorTool(R.string.imgly_tool_name_sticker, R.drawable.imgly_icon_tool_sticker),
//                new Divider(),
//                new FocusEditorTool(R.string.imgly_tool_name_focus, R.drawable.imgly_icon_tool_focus),
//                new Divider(),
//                new BrushEditorTool(R.string.imgly_tool_name_brush, R.drawable.imgly_icon_tool_brush)
//        ).setStickerLists (
//                new StickerCategoryConfig(
//                        "Internal PNG Stickers",
//                        ImageSource.create(Uri.parse("https://content.mydomain/stickers/external-stickers-category-icon.png")),
//                        new ImageStickerConfig(
//                                R.string.sticker_id_glasses_normal,
//                                R.string.sticker_name_glasses_normal,
//                                R.drawable.sticker_preview_glasses_normal,
//                                R.drawable.sticker_glasses_normal
//                        ),
//                        new ImageStickerConfig(
//                                R.string.sticker_id_glasses_nerd,
//                                R.string.sticker_name_glasses_nerd,
//                                R.drawable.sticker_preview_glasses_nerd,
//                                R.drawable.sticker_glasses_nerd
//                        ),
//                        new ImageStickerConfig(
//                                R.string.sticker_id_glasses_shutter_green,
//                                R.string.sticker_name_glasses_shutter_green,
//                                R.drawable.sticker_preview_glasses_shutter_green,
//                                R.drawable.sticker_glasses_shutter_green
//                        ),
//                        new ImageStickerConfig(
//                                R.string.sticker_id_glasses_shutter_yellow,
//                                R.string.sticker_name_glasses_shutter_yellow,
//                                R.drawable.sticker_preview_glasses_shutter_yellow,
//                                R.drawable.sticker_glasses_shutter_yellow
//                        )
//                ),
//                new StickerCategoryConfig(
//                        "Internal VectorDrawable Stickers",
//                        ImageSource.create(Uri.parse("https://content.mydomain/stickers/external-stickers-category-icon.png")),
//                        new ImageStickerConfig(
//                                R.string.imgly_sticker_id_toy_drum,
//                                R.string.imgly_sticker_name_toy_drum,
//                                R.drawable.imgly_sticker_toy_drum,
//                                R.drawable.imgly_sticker_toy_drum,
//                                ImageStickerConfig.OPTION_MODE.INK_STICKER
//                        ),
//                        new ImageStickerConfig(
//                                R.string.imgly_sticker_name_toy_crayons,
//                                R.string.imgly_sticker_name_toy_crayons,
//                                R.drawable.imgly_sticker_toy_crayons,
//                                R.drawable.imgly_sticker_toy_crayons,
//                                ImageStickerConfig.OPTION_MODE.INK_STICKER
//                        )
//                ),
//                new StickerCategoryConfig(
//                        "External Stickers",
//                        ImageSource.create(Uri.parse("https://content.mydomain/stickers/external-stickers-category-icon.png")),
//                        new ImageStickerConfig(
//                                "my_unique_id_glasses_normal_png",
//                                "My External PNG",
//                                ImageSource.create(Uri.parse("https://content.mydomain/stickers/glasses-preview-128x128.png")),
//                                ImageSource.create(Uri.parse("https://content.mydomain/stickers/glasses.png"))
//                        ),
//                        new ImageStickerConfig(
//                                "my_unique_id_glasses_normal_vector",
//                                "External VectorDrawable",
//                                ImageSource.create(Uri.parse("https://content.mydomain/stickers/glasses-vector.xml")),
//                                ImageSource.create(Uri.parse("https://content.mydomain/stickers/glasses-vector.xml"))
//                        ),
//                        new ImageStickerConfig(
//                                "my_unique_id_glasses_normal_file",
//                                "My File",
//                                ImageSource.create(Uri.fromFile(myPreviewFile)),
//                                ImageSource.create(Uri.fromFile(myFile))
//                        )
//                )
//        );
//
//        // setMyEventTracker(settingsList);
//    }

}
