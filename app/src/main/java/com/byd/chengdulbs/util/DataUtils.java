package com.byd.chengdulbs.util;

import android.content.Context;
import com.byd.chengdulbs.model.BuildingModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DataUtils {

    public static List<BuildingModel> loadBuildings(Context context) {
        try {
            // 读取 assets/building_data.json
            InputStream is = context.getAssets().open("building_data.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            // 解析 JSON
            Gson gson = new Gson();
            Type listType = new TypeToken<List<BuildingModel>>() {}.getType();
            return gson.fromJson(json, listType);

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>(); // 失败返回空列表
        }
    }
}