package com.feicui.TreasureMap.home.map;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMapOptions;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.feicui.TreasureMap.R;
import com.feicui.TreasureMap.commons.ActivityUtils;
import com.feicui.TreasureMap.home.Treasure;
import com.feicui.TreasureMap.home.TreasureRepo;
import com.feicui.TreasureMap.home.TreasureView;
import com.hannesdorfmann.mosby.mvp.MvpFragment;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.feicui.TreasureMap.home.Area;

/**
 * Created by Administrator on 16-7-20.
 */
public class MapFragment extends MvpFragment<MapMvpView, MapPresenter> implements MapMvpView {

    private ActivityUtils activityUtils;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activityUtils = new ActivityUtils(this);
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public MapPresenter createPresenter() {
        return new MapPresenter();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        //初始化百度地图
        initBaiduMap();
        // 初始定位相关
        initLocation();
    }

    @Bind(R.id.map_frame)
    FrameLayout frameLayout;
    private MapView mapView;//地图视图
    private BaiduMap baiduMap;//地图视图操作类

    private void initBaiduMap() {
        //地图的状态
        MapStatus mapStatus = new MapStatus.Builder()
                .zoom(15)//缩放大小
                .overlook(0)
                .build();
        BaiduMapOptions baiduMapOptions = new BaiduMapOptions()
                .mapStatus(mapStatus)//地图相关状态
                .overlookingGesturesEnabled(false) // 俯仰关闭
                .zoomControlsEnabled(false);//缩放是否激活
        //地图视图
        mapView = new MapView(getActivity(), baiduMapOptions);
        //拿到当前MapView的控制器
        baiduMap = mapView.getMap();
        //在当前布局添加mapView
        frameLayout.addView(mapView, 0);

        // 对Marker的监听
        baiduMap.setOnMarkerClickListener(markerClickListener);
        // 对地图状态进行监听
        baiduMap.setOnMapStatusChangeListener(mapStatusChangeListener);
    }

    // 定位核心API
    private LocationClient locationClient;
    // 我的位置(通过定位得到的当前位置经纬度)
    private LatLng myLocation;

    private void initLocation() {
        // 激活定位图层
        baiduMap.setMyLocationEnabled(true);
        // 定位实例化
        locationClient = new LocationClient(getActivity().getApplicationContext());
        // 进行一些定位的一般常规性设置
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开GPS
        option.setScanSpan(60000);// 扫描周期
        option.setCoorType("bd09ll");// 百度坐标类型
        locationClient.setLocOption(option);
        // 注册定位监听
        locationClient.registerLocationListener(locationListener);
        // 开始定位
        locationClient.start();
        locationClient.requestLocation(); // 请求位置(解决部分机器,初始定位不成功问题)
    }

    private final BitmapDescriptor dot = BitmapDescriptorFactory.fromResource(R.drawable.treasure_dot);
    private final BitmapDescriptor iconExpanded = BitmapDescriptorFactory.fromResource(R.drawable.treasure_expanded);
    // 定位监听
    private final BDLocationListener locationListener = new BDLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            // 定位不成功 -- 最好UI上有表现
            if (bdLocation == null) {
                locationClient.requestLocation();
                return;
            }
            double lon = bdLocation.getLongitude();// 经度
            double lat = bdLocation.getLatitude();// 纬度
            myLocation = new LatLng(lat, lon);
            MyLocationData myLocationData = new MyLocationData.Builder()
                    .longitude(lon)
                    .latitude(lat)
                    .accuracy(100f) // 精度
                    .build();
            // 设置定位图层“我的位置”
            baiduMap.setMyLocationData(myLocationData);
            // 移动到我的位置上去
            animateMoveToMyLocation();
        }
    };

    @OnClick(R.id.tv_located)
    public void animateMoveToMyLocation() {
        MapStatus mapStatus = new MapStatus.Builder()
                .target(myLocation)// 当前位置
                .rotate(0)// 地图摆正
                .zoom(19)
                .build();
        MapStatusUpdate update = MapStatusUpdateFactory.newMapStatus(mapStatus);
        baiduMap.animateMapStatus(update);
    }

    // 地图缩放操作
    @OnClick({R.id.iv_scaleDown, R.id.iv_scaleUp})
    public void scaleMap(View view) {
        switch (view.getId()) {
            case R.id.iv_scaleUp:
                baiduMap.setMapStatus(MapStatusUpdateFactory.zoomIn());
                break;
            case R.id.iv_scaleDown:
                baiduMap.setMapStatus(MapStatusUpdateFactory.zoomOut());
                break;
        }
    }

    // 地图类别更新
    @OnClick(R.id.tv_satellite)
    public void switchMapType() {
        int type = baiduMap.getMapType();
        type = type == BaiduMap.MAP_TYPE_NORMAL ? BaiduMap.MAP_TYPE_SATELLITE : BaiduMap.MAP_TYPE_NORMAL;
        baiduMap.setMapType(type);
    }

    // 指南针更新
    @OnClick(R.id.tv_compass)
    public void switchCompass() {
        boolean isCompass = baiduMap.getUiSettings().isCompassEnabled();
        baiduMap.getUiSettings().setCompassEnabled(!isCompass);
    }

    // 对地图状态进行监听(缩放?移动等等)
    private final BaiduMap.OnMapStatusChangeListener mapStatusChangeListener = new BaiduMap.OnMapStatusChangeListener() {
        @Override public void onMapStatusChangeStart(MapStatus mapStatus) {
        }

        @Override public void onMapStatusChange(MapStatus mapStatus) {
        }

        @Override public void onMapStatusChangeFinish(MapStatus mapStatus) {
            updateMapArea();
        }
    };

    /**
     * 更新地图区域，重新执行业务获取宝藏(目前是只要移动，就会重新获取,会N次获取同区域数据)
     */
    private void updateMapArea() {
        // 先得到你当前所在位置
        MapStatus mapStatus = baiduMap.getMapStatus();
        double lng = mapStatus.target.longitude;
        double lat = mapStatus.target.latitude;
        // 计算出你的Area  23.999  15.130
        //              24,23  ,  16,15去确定Area
        Area area = new Area();
        area.setMaxLat(Math.ceil(lat));  // lat向上取整
        area.setMaxLng(Math.ceil(lng));  // lng向上取速
        area.setMinLat(Math.floor(lat));  // lat向下取整
        area.setMinLng(Math.floor(lng));  // lng向下取整
        // 执行业务,根据Area去获取宝藏
        getPresenter().getTreasure(area);
    }

    @Override
    public void showMessage(String msg) {
        activityUtils.showToast(msg);
    }

    @Override
    public void setData(List<Treasure> datas) {
        // 将业务获取到的“附近宝藏”添加为一个个的Marker
        for (Treasure treasure : datas) {
            LatLng position = new LatLng(treasure.getLatitude(), treasure.getLongitude());
            addMarker(position, treasure.getId());
        }
    }

    private void addMarker(final LatLng position, final int treasureId) {
        // 测试代码(添加Marker)-------------------------------------------
        // 显示出一个Marker(标记)
        MarkerOptions options = new MarkerOptions();
        options.position(position);// 设置Marker位置
        options.icon(dot);// 设置Marker图标
        options.anchor(0.5f, 0.5f);// 设置Marker的锚点(中)
        // 将当前宝藏的ID号存到Marker里去()
        Bundle bundle = new Bundle();
        bundle.putInt("id", treasureId);
        options.extraInfo(bundle);
        baiduMap.addOverlay(options); // 添加孚盖物
    }

    private Marker currentMarker;
    // 宝藏信息展示卡片
    @Bind(R.id.treasureView)
    TreasureView treasureView;

    // 对Marker的监听
    private final BaiduMap.OnMarkerClickListener markerClickListener = new BaiduMap.OnMarkerClickListener() {
        @Override public boolean onMarkerClick(Marker marker) {
            currentMarker = marker;
            // 设置Marker不可见
            currentMarker.setVisible(false);
            InfoWindow infoWindow = new InfoWindow(iconExpanded, marker.getPosition(), 0, infoWindowClickListener);
            // 显示一个信息窗口(icon,位置,Y,监听)
            baiduMap.showInfoWindow(infoWindow);
            // 从当前Marker中取出这个宝藏的id号(@see addMarker时的操作)
            int id = marker.getExtraInfo().getInt("id");
            // 从宝藏仓库中，根据id号取出宝藏
            Treasure treasure = TreasureRepo.getInstance().getTreasure(id);
            treasureView.bindTreasure(treasure);
            // 更新UI模式(进入选择模式)
            changUiMode(UI_MODE_SELECT);
            return false;
        }
    };

    // 对InfoWindow的监听
    private final InfoWindow.OnInfoWindowClickListener infoWindowClickListener = new InfoWindow.OnInfoWindowClickListener() {
        @Override public void onInfoWindowClick() {
            currentMarker.setVisible(true);
            baiduMap.hideInfoWindow();
            // 回到普通模式
            changUiMode(UI_MODE_NORMAL);
        }
    };

    public boolean onBackPressed() {
        if (this.uiMode != UI_MODE_NORMAL) {
            changUiMode(UI_MODE_NORMAL);
            return false;
        }
        // 当前为普通浏览模式的话，返回true，表明当前fragment不要做其他操作,你可以退出
        return true;
    }

    /**
     * 进入埋藏宝藏模式(在按下藏宝时调用的)
     */
    public void hideTreasure() {
        changUiMode(UI_MODE_HIDE);
    }

    /**
     * 宝藏信息提示,默认隐藏的(在屏幕下方位置,包括两种模式下的布局,选中模式时的信息展示卡片,埋藏模式时的信息录入)
     */
    @Bind(R.id.layout_bottom) FrameLayout bottomLayout;
    /**
     * 埋藏宝藏时需要(中心位置藏宝控件)
     */
    @Bind(R.id.centerLayout)
    RelativeLayout conterLayout;
    /**
     * 埋藏宝藏时的信息录入卡片(在屏幕下方位置)
     */
    @Bind(R.id.hide_treasure) RelativeLayout hideTreasure;
    /**
     * 埋藏宝藏时"藏在这里"的按钮
     */
    @Bind(R.id.btn_HideHere)
    Button btnHideHere;

    private static final int UI_MODE_NORMAL = 0;// 普通
    private static final int UI_MODE_SELECT = 1;// 选中
    private static final int UI_MODE_HIDE = 2; // 埋藏

    private int uiMode = UI_MODE_NORMAL;

    private void changUiMode(int uiMode) {
        if (this.uiMode == uiMode) return;
        this.uiMode = uiMode;
        switch (uiMode) {
            // 进入普通模式(下方布局不可见,藏宝操作布局不可见)
            case UI_MODE_NORMAL:
                bottomLayout.setVisibility(View.GONE);// 隐藏下方的宝藏信息layout
                conterLayout.setVisibility(View.GONE);// 隐藏中间位置藏宝layout
                break;
            // 进入选中模式
            case UI_MODE_SELECT:
                bottomLayout.setVisibility(View.VISIBLE);// 显示下方的宝藏信息layout
                treasureView.setVisibility(View.VISIBLE);// 显示宝藏信息卡片
                conterLayout.setVisibility(View.GONE); // 隐藏中间位置藏宝layout
                hideTreasure.setVisibility(View.GONE); // 隐藏宝藏录入信息卡片
                break;
            // 进入埋藏模式
            case UI_MODE_HIDE:
                conterLayout.setVisibility(View.VISIBLE);// 显示中间位置藏宝layout
                bottomLayout.setVisibility(View.GONE);// 隐藏下方的宝藏信息layout
                // 按下藏宝时
                btnHideHere.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        bottomLayout.setVisibility(View.VISIBLE);// 显示下方的宝藏信息layout
                        hideTreasure.setVisibility(View.VISIBLE);// 显示宝藏录入信息卡片
                        treasureView.setVisibility(View.GONE);// 隐藏宝藏信息卡片
                    }
                });
                break;
        }
    }
}