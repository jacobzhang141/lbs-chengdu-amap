package com.byd.chengdulbs;

import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.amap.api.maps.MapsInitializer;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.GroundOverlayOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;

import com.amap.api.maps.model.MyLocationStyle;
import com.byd.chengdulbs.R;

public class MainActivity extends AppCompatActivity {

    private AMap amap;
    private MapView mapview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //高德地图部分开始
        setContentView(R.layout.activity_main);

        // 设置隐私合规
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);


//         * 设置离线地图存储目录，在下载离线地图或初始化地图设置;
//         * 使用过程中可自行设置, 若自行设置了离线地图存储的路径，
//         * 则需要在离线地图下载和使用地图页面都进行路径设置
        //Demo中为了其他界面可以使用下载的离线地图，使用默认位置存储，屏蔽了自定义设置
//        MapsInitializer.sdcardDir =OffLineMapUtils.getSdCacheDir(this);

        //获取地图控件引用
        mapview = (MapView) findViewById(R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mapview.onCreate(savedInstanceState);// 此方法必须重写
        init();
        //高德地图部分结束
        MyLocationStyle myLocationStyle;
        myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类
//        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);//连续定位、蓝点不会移动到地图中心点，定位点依照设备方向旋转，并且蓝点会跟随设备移动。
        myLocationStyle.interval(1000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。/设置定位频次方法，单位：毫秒，默认值：1000毫秒，如果传小于1000的任何值将按照1000计算。该方法只会作用在会执行连续定位的工作模式上。
        // 自定义精度圆圈的样式
        //myLocationStyle.strokeColor(Color.BLUE); // 设置精度圆圈的边框颜色
        //myLocationStyle.radiusFillColor(Color.argb(100, 0, 0, 255)); // 设置精度圆圈的填充颜色
        // 隐藏精度圆圈
        myLocationStyle.strokeColor(Color.TRANSPARENT); // 设置边框颜色为透明
        myLocationStyle.radiusFillColor(Color.TRANSPARENT); // 设置填充颜色为透明
        myLocationStyle.strokeWidth(0.1f); // 设置精度圆圈的边框宽度
        // 应用样式到地图
        amap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        amap.getUiSettings().setMyLocationButtonEnabled(true);//设置默认定位按钮是否显示，非必需设置。
        amap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。

//        amap.moveCamera(CameraUpdateFactory.zoomTo(17));
        amap.moveCamera(CameraUpdateFactory.zoomTo(18));

        EdgeToEdge.enable(this);
        //关闭文字
        amap.showMapText(false);

    }


//     * 初始化AMap对象*

    private void init() {
        if (amap == null) {
            amap = mapview.getMap();

            // 获取 UI 设置对象
            UiSettings settings = amap.getUiSettings();
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(30.465527, 103.983152))// 看向成都的位置
//                      .target(new LatLng(30.085993,120.64883)) //看向绍兴的位置
                    .bearing(25.5f) // 设置地图初始旋转角度 -25.5 //成都的旋转角度
//                      .bearing(12f) //绍兴的旋转角度
//                    .tilt(45)        // 可选：设置倾斜角度以增强 3D 效果
                    .zoom(17)        // 设置缩放级别
                    .build();
            amap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

            // 启用 3D 楼块
            amap.showBuildings(true);

            // 禁用旋转手势
//            settings.setRotateGesturesEnabled(false);
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

//     * 往地图上添加一个groundoverlay覆盖物//

    private void addOverlayToMap() {
        amap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(30.465527, 103.983152), 18)); // 设置当前地图显示为指定位置
        LatLngBounds bounds_chengdu = new LatLngBounds.Builder()
                .include(new LatLng(30.462238, 103.979006))
                .include(new LatLng(30.470831, 103.991467)).build();

//        amap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(39.936713,
//                116.386475), 18));// 设置当前地图显示为北京市恭王府
//        LatLngBounds bounds = new LatLngBounds.Builder()
//                .include(new LatLng(39.935029, 116.384377))
//                .include(new LatLng(39.939577, 116.388331)).build();

        amap.addGroundOverlay(new GroundOverlayOptions()
                .anchor(0.5f, 0.5f)
                .transparency(0.1f)
//				.zIndex(GlobalConstants.ZindexLine - 1)
                .image(BitmapDescriptorFactory
                        .fromResource(R.drawable.chengdu1))

                .positionFromBounds(bounds_chengdu));

        //绍兴地图
        LatLngBounds bounds_shaoxing = new LatLngBounds.Builder()
                .include(new LatLng(30.083, 120.6454))
                .include(new LatLng(30.088371, 120.651524)).build();

        amap.addGroundOverlay(new GroundOverlayOptions()
                .anchor(0.5f, 0.5f)
                .transparency(0.1f)
//				.zIndex(GlobalConstants.ZindexLine - 1)
                .image(BitmapDescriptorFactory
                        .fromResource(R.drawable.shaoxing))

                .positionFromBounds(bounds_shaoxing));
    }

//     * 方法必须重写

    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mapview.onResume();
    }


//     * 方法必须重写

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mapview.onPause();
    }

//     * 方法必须重写

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mapview.onSaveInstanceState(outState);
    }


//     * 方法必须重写

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mapview.onDestroy();
    }







}