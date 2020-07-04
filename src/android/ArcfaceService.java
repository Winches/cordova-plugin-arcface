package cordova.plugin.arcface;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.arcsoft.face.ActiveFileInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.FaceSimilar;
import com.arcsoft.face.util.ImageUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class ArcfaceService {

    private static final String LOG_TAG = "ArcFace";

    private FaceEngine faceEngine = new FaceEngine();
    private Context context;

    private boolean initialized = false;

    public ArcfaceService(Context context) {
        this.context = context;
    }

    private String getString(String defType, String name, Object... formatArgs) {
        int res = context.getResources().getIdentifier(name, "string", context.getPackageName());
        return context.getString(res, formatArgs);
    }

    public void initialize() throws Exception {
        if (initialized)
            return;

        int mask = FaceEngine.ASF_FACE_RECOGNITION | FaceEngine.ASF_FACE_DETECT | FaceEngine.ASF_AGE
                | FaceEngine.ASF_GENDER | FaceEngine.ASF_FACE3DANGLE | FaceEngine.ASF_LIVENESS;
        int faceEngineCode = faceEngine.init(context, FaceEngine.ASF_DETECT_MODE_IMAGE, FaceEngine.ASF_OP_0_ONLY, 16,
                10, mask);

        Log.i(LOG_TAG, "initEngine: init " + faceEngineCode);

        if (faceEngineCode != ErrorInfo.MOK)
            throw new Exception(getString("string", "init_failed", faceEngineCode));
        initialized = true;
    }

    public void destory() {
        if (faceEngine != null) {
            int faceEngineCode = faceEngine.unInit();
            Log.i(LOG_TAG, "unInitEngine: " + faceEngineCode);
        }
    }

    /**
     * 在线激活
     * 
     * @param appId
     * @param sdkKey
     * @throws Exception
     */
    public void activeEngine(String appId, String sdkKey) throws Exception {
        // if (!checkPermissions(NEEDED_PERMISSIONS)) {
        // cordova.requestPermissions(this, ACTION_REQUEST_PERMISSIONS,
        // NEEDED_PERMISSIONS);
        // return;
        // }

        ActiveFileInfo activeFileInfo = new ActiveFileInfo();
        int res = faceEngine.getActiveFileInfo(context, activeFileInfo);
        if (res == ErrorInfo.MOK) {
            Log.i(LOG_TAG, activeFileInfo.toString());
            return;
        }

        int activeCode = faceEngine.activeOnline(context, appId, sdkKey);
        Log.i(LOG_TAG, String.format("actived:%s", activeCode));

        // if (activeCode == ErrorInfo.MOK) {
        // showToast(getString("string", "active_success"));
        // } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
        // showToast(getString("string", "already_activated"));
        // } else {
        // showToast(getString("string", "active_failed", activeCode));
        // }

        if (activeCode != ErrorInfo.MOK && activeCode != ErrorInfo.MERR_ASF_ALREADY_ACTIVATED)
            throw new Exception(getString("string", "active_failed", activeCode));
    }

    /**
     * 获取特征码
     * 
     * @param img Bitmap image
     * @return FaceFeature
     */
    public FaceFeature getFaceFeature(Bitmap img) throws Exception {
        if (img == null)
            throw new Exception(getString("string", "invalid_image"));

        Bitmap bitmap = img.copy(Bitmap.Config.ARGB_8888, true);
        bitmap = ImageUtils.alignBitmapForBgr24(bitmap);

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        byte[] bgr24 = ImageUtils.bitmapToBgr24(bitmap);

        List<FaceInfo> faceInfoList = new ArrayList<>();
        long fdStartTime = System.currentTimeMillis();
        int detectCode = faceEngine.detectFaces(bgr24, width, height, FaceEngine.CP_PAF_BGR24, faceInfoList);
        if (detectCode == ErrorInfo.MOK) {
            Log.i(LOG_TAG, "processImage: fd costTime = " + (System.currentTimeMillis() - fdStartTime));
        }

        if (faceInfoList.size() == 0) {
            throw new Exception(getString("string", "detect_face_failed", detectCode));
        }

        // 特征提取
        FaceFeature faceFeature = new FaceFeature();
        long frStartTime = System.currentTimeMillis();
        int code = faceEngine.extractFaceFeature(bgr24, width, height, FaceEngine.CP_PAF_BGR24, faceInfoList.get(0),
                faceFeature);
        if (code != ErrorInfo.MOK) {
            throw new Exception(getString("string", "extract_feature_failed", code));
        }

        return faceFeature.clone();
    }

    /**
     * 比较人脸特征
     * 
     * @param left  FaceFeature
     * @param right FaceFeature
     * @return FaceSimilar
     * @throws Exception
     */
    public FaceSimilar compareFeature(FaceFeature left, FaceFeature right) throws Exception {
        FaceSimilar matching = new FaceSimilar();
        // 比对两个人脸特征获取相似度信息
        int code = faceEngine.compareFaceFeature(left, right, matching);
        if (code != ErrorInfo.MOK)
            throw new Exception(getString("string", "compare_failed", code));
        return matching;
    }
}