package com.example.martinosama.maraduermap.SQLite;

import android.provider.BaseColumns;

public class MapContract {

    public static final class MapEntry implements BaseColumns {
        public static final String TABLE_ONE = "floorOne";
        public static final String TABLE_TWO = "floorTwo";
        public static final String TABLE_THREE = "floorThree";
        public static final String TABLE_BASEMENT = "basement";
        public static final String TABLE_GROUND = "ground";

        public static final String COLUMN_ROOM_X = "pointX";
        public static final String COLUMN_ROOM_Y = "pointY";
        public static final String COLUMN_ROOM_TYPE = "type";
    }

}
