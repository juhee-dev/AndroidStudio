package com.example.basicmaptest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ_LOC = 100;

    private GoogleMap mGoogleMap;
    private LocationManager locationManager;

    private Marker centerMarker;
    private PolylineOptions pOptions;
    // 과제
    private ArrayList<Marker> markers = new ArrayList();
    private Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(mapReadyCallback); // map loading

        pOptions = new PolylineOptions();
        pOptions.color(Color.RED);
        pOptions.width(5);

        geocoder = new Geocoder(this, Locale.getDefault());
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnStart:
                locationUpdate();
                break;
            case R.id.btnStop:
                locationManager.removeUpdates(locationListener);
                break;
        }
    }

    private void locationUpdate() {
        if (checkPermission()) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    3000, 5, locationListener);
        }
    }

    private void clearMap(GoogleMap googleMap) {
        googleMap.clear();
    }

    // 과제 - 실행후 생긴 첫 번째 마커가 이동 동선 따라 선으로 연결됨....이 아니라 새로운 마커가 생성되어 동선 선으로 연결됨.
    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            LatLng currentLoc = new LatLng(location.getLatitude(), location.getLongitude());
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLoc, 17));
            centerMarker = markers.get(0);
            centerMarker.setPosition(currentLoc); // 마커

            pOptions.add(currentLoc); // 경로 직선으로 표시
            mGoogleMap.addPolyline(pOptions);
        }
    };

    OnMapReadyCallback mapReadyCallback = new OnMapReadyCallback() { // map 객체 위치 설정
        @Override
        public void onMapReady(@NonNull GoogleMap googleMap) {
            mGoogleMap = googleMap;
            LatLng currentLoc = new LatLng(37.606320, 127.041808);

            // 과제 - 가장 마지막으로 수신한 위치로 지도 이동
            Location lastKnownLocation = null;
            if (checkPermission()) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            if (lastKnownLocation != null) {
                double lat = lastKnownLocation.getLatitude();
                double lng = lastKnownLocation.getLongitude();
                currentLoc = new LatLng(lat, lng);
                Log.d("Main", "longtitude=" + lng + ", latitude=" + lat);
            }

//            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLoc, 17));
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLoc, 17));

            MarkerOptions options = new MarkerOptions();
            options.position(currentLoc);
            options.title("가장 최근 방문한 위치");
//            options.snippet("");
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

            markers.add(mGoogleMap.addMarker(options));
            markers.get(0).showInfoWindow();
//            centerMarker = mGoogleMap.addMarker(options);
//            centerMarker.showInfoWindow();

            // 과제 - ArrayList로 마커 읽기
            for (int i = 0; i < markers.size(); i++) {
                options = new MarkerOptions();
                options.position(markers.get(i).getPosition());
                options.title(markers.get(i).getTitle());
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                mGoogleMap.addMarker(options);
            }

            mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() { // map 클릭 시
                @Override
                public void onMapClick(@NonNull LatLng latLng) {
                    Toast.makeText(MainActivity.this, "위도:"+ latLng.latitude +"\n경도: "+ latLng.longitude, Toast.LENGTH_SHORT).show();
                }
            });

            // 과제 - 롱클릭시 새 마커 생성
            mGoogleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(@NonNull LatLng latLng) {
                    MarkerOptions options = new MarkerOptions();
                    options.position(latLng);
                    String lat = String.format("%.6f", latLng.latitude);
                    String lng = String.format("%.6f", latLng.longitude);
                    options.title("위도:"+ lat +", 경도: "+ lng);
//                    options.snippet("위도:"+ lat +", 경도: "+ lng);
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    markers.add(mGoogleMap.addMarker(options));
                    markers.get(markers.size() - 1).showInfoWindow();
//                    centerMarker = mGoogleMap.addMarker(options);
//                    centerMarker.showInfoWindow();
                }
            });

            // 과제 - 지오코더 사용하여 주소 출력
            mGoogleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() { // info 창 클릭 시
                @Override
                public void onInfoWindowClick(@NonNull Marker marker) {
                    List<String> address = getAddress(marker.getPosition().latitude, marker.getPosition().longitude);
                    String add = address.get(0);
                    Toast.makeText(MainActivity.this, "주소: "+ add, Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    //    Geocoding
    private List<String> getAddress(double latitude, double longitude) {

        List<Address> addresses = null;
        ArrayList<String> addressFragments = null;

//        위도/경도에 해당하는 주소 정보를 Geocoder 에게 요청
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        if (addresses == null || addresses.size()  == 0) {
            return null;
        } else {
            Address addressList = addresses.get(0);
            addressFragments = new ArrayList<String>();

            for(int i = 0; i <= addressList.getMaxAddressLineIndex(); i++) {
                addressFragments.add(addressList.getAddressLine(i));
            }
        }

        return addressFragments;
    }

    /*위치 관련 권한 확인 메소드 - 필요한 부분이 여러 곳이므로 메소드로 구성*/
    /*ACCESS_FINE_LOCATION - 상세 위치 확인에 필요한 권한
    ACCESS_COARSE_LOCATION - 대략적 위치 확인에 필요한 권한*/
    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQ_LOC);
                return false;
            } else
                return true;
        }
        return false;
    }


    /*권한승인 요청에 대한 사용자의 응답 결과에 따른 수행*/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case PERMISSION_REQ_LOC:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    /*권한을 승인받았을 때 수행하여야 하는 동작 지정*/
                    locationUpdate();
                } else {
                    /*사용자에게 권한 제약에 따른 안내*/
                    Toast.makeText(this, "Permissions are not granted.", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}