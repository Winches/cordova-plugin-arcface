package cordova.plugin.arcface;

import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceSimilar;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.util.ImageUtils;

import org.apache.cordova.BuildConfig;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.util.Base64;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;

import cordova.plugin.arcface.util.face.LivenessType;

/**
 * This class echoes a string called from JavaScript.
 */
public class Arcface extends CordovaPlugin {

    private static final String LOG_TAG = "ArcFace";
    private static final String APP_ID = "AppID";
    private static final String SDK_KEY = "SdkKey";
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    private static final int ACTION_CHOOSE_IMAGE = 0x201;
    private static final String[] NEEDED_PERMISSIONS = new String[] { Manifest.permission.READ_PHONE_STATE };

    private static final String ACTION_ACTIVE_ENGINE = "activeEngine";
    private static final String ACTION_GET_FACE_FEATURE = "getFaceFeature";
    private static final String ACTION_GET_FACE_FEATURE_BY_LOCALIMAGE = "getFaceFeatureByLocalImage";
    private static final String ACTION_COMPARE_FEATURE = "compareFeature";
    private static final String ACTION_GET_LIVENESS = "getLiveness";

    private Context context;
    private CallbackContext callbackContext;
    private Toast toast;
    private ArcfaceService faceService;

    // #region leftcycle
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        context = cordova.getActivity();
        faceService = new ArcfaceService(context);
    }

    @Override
    public void onDestroy() {
        // unInitEngine();
        faceService.destory();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults)
            throws JSONException {
        super.onRequestPermissionResult(requestCode, permissions, grantResults);
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            boolean isAllGranted = true;
            for (int grantResult : grantResults) {
                isAllGranted &= (grantResult == PackageManager.PERMISSION_GRANTED);
            }
            if (isAllGranted) {
                activeEngine(callbackContext);
            } else {
                getString("string", "permission_denied");
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTION_CHOOSE_IMAGE) {
            if (data == null || data.getData() == null) {
                showToast(getString("string", "get_picture_failed"));
                return;
            }

            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        String path = data.getDataString();
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), data.getData());

                        FaceFeature face = faceService.getFaceFeature(bitmap);
                        callbackContext.success(Base64.encodeToString(face.getFeatureData(), Base64.DEFAULT));
                    } catch (Exception ex) {
                        callbackContext.error(ex.getMessage());
                    }
                }
            });
        }
    }
    // #endregion

    // #region export method
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        switch (action) {
            case ACTION_ACTIVE_ENGINE:
                activeEngine(callbackContext);
                return true;
            case ACTION_GET_FACE_FEATURE:
                String path = args.getString(0);
                getFaceFeature(path, callbackContext);
                return true;
            case ACTION_GET_FACE_FEATURE_BY_LOCALIMAGE:
                if (BuildConfig.DEBUG) {
                    chooseLocalImage();
                } else {
                    showToast("invalid invocation");
                }
                return true;
            case ACTION_COMPARE_FEATURE:
                JSONArray arr = args.getJSONArray(0);
                compareFeature(arr, callbackContext);
                return true;
            case ACTION_GET_LIVENESS:
                String imgpath = args.getString(0);
                getLiveness(imgpath, callbackContext);
                return true;
            default:
                return false;
        }
    }

    private void activeEngine(CallbackContext callbackContext) {
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            cordova.requestPermissions(this, ACTION_REQUEST_PERMISSIONS, NEEDED_PERMISSIONS);
            return;
        }

        cordova.getThreadPool().execute(new Runnable() {

            @Override
            public void run() {
                try {
                    final String appId = preferences.getString(APP_ID, "");
                    final String sdkKey = preferences.getString(SDK_KEY, "");
                    if (appId == null || appId.length() == 0)
                        throw new Exception(getString("string", "invalid_app_id"));
                    if (sdkKey == null || sdkKey.length() == 0)
                        throw new Exception(getString("string", "invalid_sdk_key"));

                    faceService.activeEngine(appId, sdkKey);
                    callbackContext.success(getString("string", "active_success"));
                } catch (Exception ex) {
                    callbackContext.error(ex.getMessage());
                }
            }
        });
    }

    private void getFaceFeature(String path, CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {

            @Override
            public void run() {
                try {
                    faceService.initialize();
                    if (path == null || path.length() == 0)
                        throw new Exception(getString("string", "invalid_image"));

                    Uri uri = Uri.parse(path);
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);

                    FaceFeature face = faceService.getFaceFeature(bitmap);
                    callbackContext.success(Base64.encodeToString(face.getFeatureData(), Base64.DEFAULT));
                } catch (Exception ex) {
                    callbackContext.error(ex.getMessage());
                }
            }
        });
    }

    private void compareFeature(JSONArray arr, CallbackContext callbackContext) {
        try {
            faceService.initialize();
            String left = arr.getString(0);
            String right = arr.getString(1);
            if (left == null || right == null)
                throw new Exception("invalid arguments");

            FaceFeature leftFeature = new FaceFeature();
            FaceFeature rightFeature = new FaceFeature();
            leftFeature.setFeatureData(Base64.decode(left, Base64.DEFAULT));
            rightFeature.setFeatureData(Base64.decode(right, Base64.DEFAULT));

            FaceSimilar ret = faceService.compareFeature(leftFeature, rightFeature);
            callbackContext.success(JSONObject.numberToString(ret.getScore()));
        } catch (Exception ex) {
            callbackContext.error(ex.getMessage());
        }
    }

    private void getLiveness(String path, CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {

            @Override
            public void run() {
                try {
                    faceService.initialize();
                    if (path == null || path.length() == 0)
                        throw new Exception(getString("string", "invalid_image"));

                    Uri uri = Uri.parse(path);
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);

                    bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    bitmap = ImageUtils.alignBitmapForBgr24(bitmap);
                    byte[] bgr24 = ImageUtils.bitmapToBgr24(bitmap);

                    LivenessInfo liveness = faceService.getLiveness(bgr24, bitmap.getWidth(), bitmap.getHeight(),
                            FaceEngine.CP_PAF_BGR24, LivenessType.RGB);
                    callbackContext.success(liveness.getLiveness());
                } catch (Exception ex) {
                    callbackContext.error(ex.getMessage());
                }
            }
        });
    }

    // #endregion

    private boolean checkPermissions(String[] neededPermissions) {
        if (neededPermissions == null || neededPermissions.length == 0) {
            return true;
        }
        boolean allGranted = true;
        for (String neededPermission : neededPermissions) {
            allGranted &= cordova.hasPermission(neededPermission);
        }
        return allGranted;
    }

    private void showToast(String s) {
        if (toast == null) {
            toast = Toast.makeText(context, s, Toast.LENGTH_SHORT);
            toast.show();
        } else {
            toast.setText(s);
            toast.show();
        }
    }

    private String getString(String defType, String name, Object... formatArgs) {
        int res = context.getResources().getIdentifier(name, "string", context.getPackageName());
        return context.getString(res, formatArgs);
    }

    public void chooseLocalImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        cordova.setActivityResultCallback(this);
        cordova.getActivity().startActivityForResult(intent, ACTION_CHOOSE_IMAGE);
    }

}
