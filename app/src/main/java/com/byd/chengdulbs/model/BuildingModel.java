package com.byd.chengdulbs.model;

import android.text.TextUtils;
import com.amap.api.maps.model.LatLng;

public class BuildingModel {
    private String id;          // 对应 CSV: 栋号 (如 "1", "2A")
    private String name;        // 对应 CSV: 厂房名称
    private String commonName;  // 对应 CSV: 通俗名称
    private String coords;      // 对应 CSV: amap坐标 "103.xxx,30.xxx"
    private String area;        // 对应 CSV: 建筑面积
    private String dept;        // 对应 CSV: 使用部门

    // === 辅助方法 ===

    /**
     * 解析坐标字符串为高德 LatLng 对象
     * CSV格式是 "经度,纬度" (103..., 30...)
     * 高德构造函数是 new LatLng(纬度, 经度)
     */
    public LatLng getLatLng() {
        if (TextUtils.isEmpty(coords)) return null;
        try {
            String[] split = coords.split(",");
            double lng = Double.parseDouble(split[0]); // 经度
            double lat = Double.parseDouble(split[1]); // 纬度
            return new LatLng(lat, lng);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 自动根据 ID 生成音频路径
     * 例如 ID="2A" -> "audio/audio_2a.mp3"
     */
    public String getAudioPath() {
        return "audio/" + id.toLowerCase() + ".m4a";
    }

    /**
     * 自动根据 ID 生成字幕路径
     * 例如 ID="1" -> "subtitle/subtitle_1.srt"
     */
    public String getSrtPath() {
        return "subtitle/" + id.toLowerCase() + ".srt";
    }

    // === Getters ===
    public String getName() { return name; }
    public String getCommonName() { return commonName; }
    public String getArea() { return area; }
    public String getDept() { return dept; }
    public String getId() { return id; }
}