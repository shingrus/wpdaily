package com.shingrus.wpdaily;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;


/**
 * Image storage - Singleton object, has all necessary methods for database access
 * Created by shingrus on 06/12/15.
 */
public class ImageStorage {
    private static int KEEP_LAST_IMAGES_NUMBER = 10;
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "Images.db";
    private static final String IMAGES_TABLE_NAME = "Images";
    public static final String IMAGES_COLUMN_ID = "_id";
    public static final String IMAGES_COLUMN_URL = "url";
    public static final String IMAGES_COLUMN_IMAGE = "image";
    public static final String IMAGES_COLUMN_DATE_INSERTED = "inserted_at";
    public static final String IMAGES_COLUMN_PROVIDER = "provider";
    private static final String IMAGES_LAST_IMAGES_LIMIT = "10";
    static final String CREATE_IMAGES_TABLE = "CREATE TABLE '" + IMAGES_TABLE_NAME + "' (" +
            "'_id' INTEGER PRIMARY KEY AUTOINCREMENT," +
            "'url' TEXT UNIQ, " +
            "'inserted_at' INTEGER default (strftime('%s','now'))," +
            "'provider' TEXT default ''," +
            "'image' BLOB" +
            ")";


    private static ImageStorage sInstance;
    //    private Context ctx;
    private ImageDBHelper mImageDBHelper;

    private ImageStorage(Context ctx) {
//        ctx = context;
        mImageDBHelper = new ImageDBHelper(ctx);
    }


    static final String INSERT_IMAGE_STMNT = "INSERT INTO " + IMAGES_TABLE_NAME + " (" +
            IMAGES_COLUMN_URL + "," + IMAGES_COLUMN_PROVIDER + "," + IMAGES_COLUMN_IMAGE + ") VALUES(?,?,?)";

    /*
    there is actuaaly ugly request, but it should works on very small databases
     */
    private static final String DROP_OLD_RECORDS = "DELETE FROM " + IMAGES_TABLE_NAME +
            " WHERE " + IMAGES_COLUMN_ID + " not in (SELECT " +
            IMAGES_COLUMN_ID + " FROM " +
            IMAGES_TABLE_NAME + " ORDER BY " + IMAGES_COLUMN_DATE_INSERTED + " Desc limit " + KEEP_LAST_IMAGES_NUMBER + ")";


    public boolean isUrlAlreadyDownloaded(String url) {
        SQLiteDatabase db = mImageDBHelper.getReadableDatabase();
        Cursor c = db.query(IMAGES_TABLE_NAME,
                new String[]{IMAGES_COLUMN_ID},
                IMAGES_COLUMN_URL + " = ?",
                new String[]{url},
                null,
                null,
                null
        );
        if (c.getCount() >0){
            return true;
        }
        c.close();
        return false;
    }

    /**
     *
     * @param url - Image url
     * @param provider - String name human readable of the image provider like Flickr, natgeo, gopro...
     * @param buffer - bunary data of the image
     */
    public void putImage(String url, String provider, byte[] buffer) {
        //put image with now date
        SQLiteDatabase db = mImageDBHelper.getWritableDatabase();
        if (db != null) {
            SQLiteStatement insertStmt = db.compileStatement(INSERT_IMAGE_STMNT);
            insertStmt.bindString(1, url);
            insertStmt.bindString(2, provider);
            insertStmt.bindBlob(3, buffer);
            insertStmt.execute();
            insertStmt.clearBindings();
        }

    }

    public int deleteImage(long id) {
        SQLiteDatabase db = mImageDBHelper.getWritableDatabase();
        if (db!=null) {
            return db.delete(IMAGES_TABLE_NAME, IMAGES_COLUMN_ID + "=?", new String[]{Long.toString(id)});
        }
        return -1;
    }

    /**
     * @param id - int, Image id
     * @return Image object
     */
    public Image getImageById(long id) {
        Image retImage = null;
        SQLiteDatabase db = mImageDBHelper.getReadableDatabase();
        String where = Long.toString(id);
        Cursor c = db.query(IMAGES_TABLE_NAME,
                new String[]{IMAGES_COLUMN_IMAGE, IMAGES_COLUMN_DATE_INSERTED,
                        IMAGES_COLUMN_URL, IMAGES_COLUMN_PROVIDER},
                IMAGES_COLUMN_ID + " = ?",
                new String[]{where},
                null,
                null,
                null
        );
        if (c.moveToNext()) {

            byte[] b = c.getBlob(0);
            if (b != null) {
                retImage =
                        new Image(c.getString(2), c.getInt(1), c.getString(3), b);
            }
        }
        return retImage;
    }

    /**
     * @return ArrayList<Image> - array list of images
     */
    public Cursor getLastImagesCursor() {
        SQLiteDatabase db = mImageDBHelper.getReadableDatabase();

        return db.query(IMAGES_TABLE_NAME,
                new String[]{IMAGES_COLUMN_IMAGE, IMAGES_COLUMN_DATE_INSERTED, IMAGES_COLUMN_URL, IMAGES_COLUMN_PROVIDER, IMAGES_COLUMN_ID},
                IMAGES_COLUMN_DATE_INSERTED + "> ?",
                new String[]{"0"},
                null,
                null,
                IMAGES_COLUMN_DATE_INSERTED + " desc",
                IMAGES_LAST_IMAGES_LIMIT);

    }

    private class ImageDBHelper extends SQLiteOpenHelper {
        public ImageDBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_IMAGES_TABLE);


        }

        @Override
        public synchronized void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion == 1 && newVersion == 2) {
                db.execSQL("DROP TABLE IF EXISTS Images");
                this.onCreate(db);
            }
        }
    }

    public static synchronized ImageStorage getInstance() {
        return sInstance;
    }

    public static synchronized ImageStorage getInstance(Context context) {
        if (sInstance == null && context != null) {
            sInstance = new ImageStorage(context);
        }
        return sInstance;
    }
}

