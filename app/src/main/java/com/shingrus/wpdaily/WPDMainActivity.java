package com.shingrus.wpdaily;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.ContextMenu;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;


import com.facebook.FacebookSdk;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;

import java.util.ArrayList;

public class WPDMainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {


    private SwipeRefreshLayout mSwipeRefreshLayout;
    private static final String _log_tag = "WPD/WPDMainActivity";
    private static boolean isUpdating = false;
    private ImagesAdapter imagesAdapter = null;
    private UpdateReceiver updateReceiver = new UpdateReceiver();
    IntentFilter filter = new IntentFilter(WPUpdateService.UPDATE_ACTIVITY_ACTION);
    public static final String SKIP_WELCOME_CHECK_ACTION = "com.shingrus.wpdaily.action.skip_welcome";
    private boolean isFBinstalled = false;


    /**
     * Implements OnRefreshListener
     */
    @Override
    public void onRefresh() {
        Log.d(_log_tag, "OnRefresh");
        if (!isUpdating) {
            updateImages();
        }

    }

    public class UpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.hasExtra(WPUpdateService.RESULT_EXTRA)) {
                SetWallPaper.UpdateResult updateResult = (SetWallPaper.UpdateResult) intent.getSerializableExtra(WPUpdateService.RESULT_EXTRA);
                if (updateResult == SetWallPaper.UpdateResult.SUCCESS || updateResult == SetWallPaper.UpdateResult.ALREADY_SET)
                    getImagesFromStorage();
                Log.d(_log_tag, "Got update  to activity result: " + updateResult);
            }
            if (mSwipeRefreshLayout.isRefreshing())
                mSwipeRefreshLayout.setRefreshing(false);
            isUpdating = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(updateReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(updateReceiver);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        updateImages();
    }

    private void updateImages() {
        Intent intent = new Intent(this.getApplicationContext(), WPUpdateService.class);
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(true);
            }
        });
        startService(intent);
        isUpdating = true;

    }

    private void deleteImage(final long itemId) {

        Log.d(_log_tag, "Ask for image delete:" + itemId);
        new AsyncTask<Long, Void, ArrayList<ImageDescription>>() {
            @Override
            protected ArrayList<ImageDescription> doInBackground(Long... imageId) {
                ImageStorage storage = ImageStorage.getInstance();
                int deleteResult = storage.deleteImage(imageId[0]);
                if (deleteResult > 0) {
                    Log.d(_log_tag, "Removed images number: " + deleteResult);
                    return storage.getLastImages();
                }
                return null;
            }

            @Override
            protected void onPostExecute(ArrayList<ImageDescription> images) {

                if (imagesAdapter != null) {
                    imagesAdapter.swapItems(images);
                }
            }

        }.execute(itemId, null, null);

    }


    private void getImagesFromStorage() {

        new AsyncTask<Void, Void, ArrayList<ImageDescription>>() {

            @Override
            protected void onPreExecute() {
//                super.onPreExecute();
            }

            @Override
            protected ArrayList<ImageDescription> doInBackground(Void... params) {
                ImageStorage storage = ImageStorage.getInstance();
                return storage.getLastImages();
            }

            @Override
            protected void onPostExecute(ArrayList<ImageDescription> images) {

                if (imagesAdapter != null) {
                    imagesAdapter.swapItems(images);
                }
            }

        }.execute(null, null, null);
    }

    private void setAsWallpaper(long id) {
        new AsyncTask<Long, Void, ImageDescription>() {
            @Override
            protected void onPostExecute(ImageDescription imageDescription) {
                if (imageDescription != null) {

                    if (SetWallPaper.getSetWallPaper().setWallPaperImage(imageDescription.getData())) {
                        //wallpaper has been set successfully
                        Toast t = Toast.makeText(getApplicationContext(), getString(R.string.toastWPChanged), Toast.LENGTH_SHORT);
                        t.show();
                    }
                }
            }

            @Override
            protected ImageDescription doInBackground(Long... params) {
                return ImageStorage.getInstance().getImageById(params[0]);
            }
        }.execute(id);
    }

    private void shareAsImageOverExternalStorage(long id) {
        new AsyncTask<Long, Void, Intent>() {
            @Override
            protected void onPostExecute(Intent intent) {
                if (intent != null) {
                    startActivity(Intent.createChooser(intent, getText(R.string.ShareToTitle)));
                }
            }

            @Override
            protected Intent doInBackground(Long... params) {
                Intent intent = null;
                ImageDescription imageDescription = ImageStorage.getInstance().getImageById(params[0]);
                if (imageDescription != null) {

                    byte[] imageData = imageDescription.getData();
                    Bitmap bm = BitmapFactory.decodeByteArray(imageData, 0, imageData.length, null);
                    Uri fileUri = ImageStorage.getInstance().saveImageToExternal(bm);
                    if (fileUri != null) {
                        intent = new Intent(Intent.ACTION_SEND);
                        intent.putExtra(Intent.EXTRA_TEXT, imageDescription.getLinkPage());
                        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
                        intent.setType("image/*");
                    }
                }
                return intent;
            }
        }.execute(id);
    }
    private void shareToFacebook(long id) {
        new AsyncTask<Long, Void, ShareLinkContent>() {

            @Override
            protected ShareLinkContent doInBackground(Long... params) {
                ShareLinkContent linkContent = null;
                ImageDescription imageDescription = ImageStorage.getInstance().getImageById(params[0]);
                if (imageDescription != null ) {

                    Uri contentUri = Uri.parse(imageDescription.getLinkPage());
                    Uri imageUri = Uri.parse(imageDescription.getUrl());
                    linkContent = new ShareLinkContent.Builder()
                            .setImageUrl(imageUri)
                            .setContentUrl(contentUri)
                            .build();

                }
                return linkContent;
            }

            @Override
            protected void onPostExecute(ShareLinkContent linkContent) {
                if (linkContent != null) {
                    ShareDialog.show(WPDMainActivity.this, linkContent);

                }

            }
        }.execute(id);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo
            menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.images_list) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.image_item_menu, menu);
            if (!isFBinstalled) {
                Log.d(_log_tag, "Remove facebook Item");
                menu.getItem(0).setEnabled(false);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case R.id.menu_action_delete: {
                deleteImage(info.id);
                return true;
            }
            case R.id.menu_action_sharetoFB: {
                shareToFacebook(info.id);
                return true;
            }
            case R.id.menu_action_shareimage: {
                shareAsImageOverExternalStorage(info.id);
                return true;
            }
            case R.id.menu_action_browser: {
                String link = imagesAdapter.getItem(info.position).getLinkPage();
                if (link != null) {
                    Intent sendIntent;
                    if (item.getItemId() == R.id.menu_action_browser) {
                        sendIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                        startActivity(sendIntent);
                    }

                }
                return true;
            }
            case R.id.menu_action_set_wallpaper: {
                setAsWallpaper(info.id);
                return true;
            }
        }

        return super.onContextItemSelected(item);
    }

    private boolean checkFBinstalled () {
        try{
            ApplicationInfo info = getPackageManager().
                    getApplicationInfo("com.facebook.katana", 0 );
            Log.d(_log_tag, "Facebook app is installed");
            return true;
        } catch( PackageManager.NameNotFoundException e ) {
            Log.d(_log_tag, "No Facebook app");
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        Intent startIntent = getIntent();
        FacebookSdk.sdkInitialize(getApplicationContext());
        isFBinstalled = checkFBinstalled();
        if (!SKIP_WELCOME_CHECK_ACTION.equals(startIntent.getAction()) &&
                !pref.getBoolean(getString(R.string.WelcomeScreenShowedKey), false)) {
            Intent newActivity = new Intent(this, WelcomeActivity.class);
            startActivity(newActivity);
            finish();
        } else {
            setContentView(R.layout.activity_main);
            mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
            mSwipeRefreshLayout.setOnRefreshListener(this);
            ListView listView = (ListView) findViewById(R.id.images_list);

            imagesAdapter = new ImagesAdapter(WPDMainActivity.this);


            listView.setAdapter(imagesAdapter);
            registerForContextMenu(listView);


            //Load images
            getImagesFromStorage();
            //start update
            updateImages();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(_log_tag, "Destroy main activity");
    }
}
