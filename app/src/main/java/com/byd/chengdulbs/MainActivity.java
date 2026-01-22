package com.byd.chengdulbs;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.GroundOverlayOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.byd.chengdulbs.model.BuildingModel;
import com.byd.chengdulbs.util.DataUtils;
import com.byd.chengdulbs.view.AudioGuideDialog;

import java.util.List;

public class MainActivity extends AppCompatActivity implements AMap.OnMarkerClickListener {

    private AMap amap;
    private MapView mapview;
    // 保存数据列表，方便点击时查询
    private List<BuildingModel> buildingList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //高德地图部分开始
        setContentView(R.layout.activity_main);

        // 设置隐私合规
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);

        //获取地图控件引用
        mapview = (MapView) findViewById(R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mapview.onCreate(savedInstanceState);// 此方法必须重写
        init();

        //高德地图部分结束
        MyLocationStyle myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类

        /*
         * 模式说明：
         * LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER:
         * 连续定位、蓝点不会移动到地图中心点，
         * 【定位点依照设备方向旋转】(由高德SDK内部接管)，
         * 并且蓝点会跟随设备移动。
         */
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);

        myLocationStyle.interval(1000); //设置连续定位模式下的定位间隔

        // 隐藏精度圆圈
        myLocationStyle.strokeColor(Color.TRANSPARENT); // 设置边框颜色为透明
        myLocationStyle.radiusFillColor(Color.TRANSPARENT); // 设置填充颜色为透明
        myLocationStyle.strokeWidth(0.1f); // 设置精度圆圈的边框宽度

        // 应用样式到地图
        amap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        amap.getUiSettings().setMyLocationButtonEnabled(true);//设置默认定位按钮是否显示
        amap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点

        amap.moveCamera(CameraUpdateFactory.zoomTo(18));

        EdgeToEdge.enable(this);
        //关闭文字
        amap.showMapText(false);

        // 2. 加载数据并绘制 Marker
        loadAndDrawMarkers();
    }

    private void loadAndDrawMarkers() {
        if (amap == null) return;

        buildingList = DataUtils.loadBuildings(this);
        amap.setOnMarkerClickListener(this);

        for (BuildingModel building : buildingList) {
            LatLng latLng = building.getLatLng();

            if (latLng != null) {
                // 1. 创建 View (每次循环都新造一个)
                android.view.View markerView = android.view.LayoutInflater.from(this)
                        .inflate(R.layout.marker_layout, null);
                TextView tvName = markerView.findViewById(R.id.tv_marker_name);

                // 2. 设置文字
                String labelText = building.getCommonName();
                if (labelText == null || labelText.isEmpty()) labelText = building.getName();
                tvName.setText(labelText);

                // ▼▼▼▼▼▼▼▼▼▼ 核心修改：动态变色 ▼▼▼▼▼▼▼▼▼▼
                try {
                    // (A) 获取文字背景
                    android.graphics.drawable.GradientDrawable bgDrawable =
                            (android.graphics.drawable.GradientDrawable) tvName.getBackground();

                    // (B) 关键一步：mutate()
                    bgDrawable.mutate();

                    // (C) 设置颜色
                    bgDrawable.setColor(getMarkerColor(building));

                } catch (Exception e) {
                    e.printStackTrace();
                }
                // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

                // 3. 转成图片
                com.amap.api.maps.model.BitmapDescriptor customIcon =
                        BitmapDescriptorFactory.fromView(markerView);

                MarkerOptions options = new MarkerOptions()
                        .position(latLng)
                        .title(building.getName())
                        .snippet(building.getCommonName())
                        .icon(customIcon)
                        .anchor(0.5f, 0.5f);

                Marker marker = amap.addMarker(options);
                marker.setObject(building);
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        // 从 Marker 中取出我们绑定的 BuildingModel 对象
        Object obj = marker.getObject();

        if (obj instanceof BuildingModel) {
            BuildingModel building = (BuildingModel) obj;
            try {
                AudioGuideDialog dialog = new AudioGuideDialog(this, building);
                dialog.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true; // 消费点击事件
    }

    /**
     * 初始化AMap对象
     */
    private void init() {
        if (amap == null) {
            amap = mapview.getMap();

            // 获取 UI 设置对象
            UiSettings settings = amap.getUiSettings();
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(30.465527, 103.983152))// 看向成都的位置
                    .bearing(25.5f) // 设置地图初始旋转角度
                    .zoom(17)        // 设置缩放级别
                    .build();
            amap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

            // 启用 3D 楼块
            amap.showBuildings(true);

            // 禁用旋转手势
            settings.setRotateGesturesEnabled(false);

            // 设置 Logo 的底部边距
            settings.setLogoBottomMargin(-100);

            addOverlayToMap();//添加地图遮罩png图片

            // 启用/禁用各种控件和手势
            mapview.getMap().getUiSettings().setCompassEnabled(true);         // 控制指南针控件是否显示
            mapview.getMap().getUiSettings().setScaleControlsEnabled(true);    // 控制比例尺控件是否显示
            mapview.getMap().getUiSettings().setTiltGesturesEnabled(true);     // 手指调整俯仰角开关
            mapview.getMap().getUiSettings().setZoomGesturesEnabled(true);     // 手指调整缩放开关
            mapview.getMap().moveCamera(CameraUpdateFactory.zoomTo(17f)); // 设置缩放级别为17
        }
    }

    /**
     * 根据建筑的【通俗名称】来决定颜色
     */
    private int getMarkerColor(BuildingModel building) {
        String name = building.getCommonName();
        // 如果通俗名为空，就用正式名作为补充判断
        if (name == null || name.isEmpty()) {
            name = building.getName();
        }
        if (name == null) name = "";

        // --- 1. (红色) ---
        if (name.contains("大宗气站")) {
            return android.graphics.Color.parseColor("#CCFF4444"); // 🔴 警示红
        }

        // --- 2. 仓库/ 燃气站类 (橙色) ---
        if (name.contains("仓") || name.contains("库") || name.contains("燃气")) {
            return android.graphics.Color.parseColor("#CCFF8800"); // 🟠 活力橙
        }

        // --- 3. 动力/环保/基建类 (青绿色) ---
        if (name.contains("水") || name.contains("综合动力") || name.contains("泵")
                || name.contains("废") || name.contains("特气") || name.contains("硅烷")
                || name.contains("气化") || name.contains("生产") || name.contains("柴油")
                || name.contains("变电")) {
            return android.graphics.Color.parseColor("#CC00BFA5"); // 🟢 青松绿
        }

        // --- 4. 核心生产厂房 (深蓝色) ---
        return android.graphics.Color.parseColor("#CC2E5BFF");    // 🔵 科技蓝
    }

    /**
     * 往地图上添加一个groundoverlay覆盖物
     */
    private void addOverlayToMap() {
        amap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(30.465527, 103.983152), 18));
        LatLngBounds bounds_chengdu = new LatLngBounds.Builder()
                .include(new LatLng(30.462238, 103.979006))
                .include(new LatLng(30.470831, 103.991467)).build();

        amap.addGroundOverlay(new GroundOverlayOptions()
                .anchor(0.5f, 0.5f)
                .transparency(0.1f)
                .image(BitmapDescriptorFactory.fromResource(R.drawable.chengdu1))
                .positionFromBounds(bounds_chengdu));

        //绍兴地图
        LatLngBounds bounds_shaoxing = new LatLngBounds.Builder()
                .include(new LatLng(30.083, 120.6454))
                .include(new LatLng(30.088371, 120.651524)).build();

        amap.addGroundOverlay(new GroundOverlayOptions()
                .anchor(0.5f, 0.5f)
                .transparency(0.1f)
                .image(BitmapDescriptorFactory.fromResource(R.drawable.shaoxing))
                .positionFromBounds(bounds_shaoxing));
    }

    // --- 生命周期方法 ---

    @Override
    protected void onResume() {
        super.onResume();
        mapview.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapview.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapview.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapview.onDestroy();
    }
}