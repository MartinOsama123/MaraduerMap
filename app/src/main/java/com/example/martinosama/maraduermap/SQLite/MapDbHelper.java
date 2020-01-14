package com.example.martinosama.maraduermap.SQLite;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.martinosama.maraduermap.SQLite.MapContract.MapEntry;


public class MapDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "rooms.db";

    public static final int DATABASE_VERSION = 28;

    public MapDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        final String SQL_CREATE_TABLE_ONE = "CREATE TABLE " + MapEntry.TABLE_ONE + " (" +
                MapEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                MapEntry. COLUMN_ROOM_X + " REAL NOT NULL, " +
                MapEntry.COLUMN_ROOM_Y + " REAL NOT NULL, " +
                MapEntry.COLUMN_ROOM_TYPE + " TEXT NOT NULL " +
                "); ";

        final String SQL_CREATE_TABLE_TWO = "CREATE TABLE " + MapEntry.TABLE_TWO + " (" +
                MapEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                MapEntry. COLUMN_ROOM_X + " REAL NOT NULL, " +
                MapEntry.COLUMN_ROOM_Y + " REAL NOT NULL, " +
                MapEntry.COLUMN_ROOM_TYPE + " TEXT NOT NULL " +
                "); ";

        final String SQL_CREATE_TABLE_THREE = "CREATE TABLE " + MapEntry.TABLE_THREE + " (" +
                MapEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                MapEntry. COLUMN_ROOM_X + " REAL NOT NULL, " +
                MapEntry.COLUMN_ROOM_Y + " REAL NOT NULL, " +
                MapEntry.COLUMN_ROOM_TYPE + " TEXT NOT NULL " +
                "); ";

        final String SQL_CREATE_TABLE_BASEMENT = "CREATE TABLE " + MapEntry.TABLE_BASEMENT + " (" +
                MapEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                MapEntry. COLUMN_ROOM_X + " REAL NOT NULL, " +
                MapEntry.COLUMN_ROOM_Y + " REAL NOT NULL, " +
                MapEntry.COLUMN_ROOM_TYPE + " TEXT NOT NULL " +
                "); ";

        final String SQL_CREATE_TABLE_GROUND = "CREATE TABLE " + MapEntry.TABLE_GROUND + " (" +
                MapEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                MapEntry. COLUMN_ROOM_X + " REAL NOT NULL, " +
                MapEntry.COLUMN_ROOM_Y + " REAL NOT NULL, " +
                MapEntry.COLUMN_ROOM_TYPE + " TEXT NOT NULL " +
                "); ";

        sqLiteDatabase.execSQL(SQL_CREATE_TABLE_ONE);
        sqLiteDatabase.execSQL(SQL_CREATE_TABLE_TWO);
        sqLiteDatabase.execSQL(SQL_CREATE_TABLE_THREE);
        sqLiteDatabase.execSQL(SQL_CREATE_TABLE_GROUND);
        sqLiteDatabase.execSQL(SQL_CREATE_TABLE_BASEMENT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MapEntry.TABLE_ONE);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MapEntry.TABLE_TWO);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MapEntry.TABLE_THREE);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MapEntry.TABLE_GROUND);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MapEntry.TABLE_BASEMENT);
        onCreate(sqLiteDatabase);
    }
}