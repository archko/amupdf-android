package com.baidu.ai.edge.ui.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.baidu.ai.edge.ui.R;
import com.baidu.ai.edge.ui.view.ResultMaskView;
import com.baidu.ai.edge.ui.view.adapter.DetectResultAdapter;
import com.baidu.ai.edge.ui.view.model.BasePolygonResultModel;
import com.baidu.ai.edge.ui.view.model.BaseResultModel;
import com.baidu.ai.edge.ui.view.model.DetectResultModel;
import com.baidu.ai.edge.ui.view.model.OcrViewResultModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cn.archko.pdf.AppExecutors;

/**
 * Created by ruanshimin on 2018/5/3.
 */

public abstract class AbsOcrActivity extends BaseActivity {

    public static final int MODEL_OCR = 100;

    protected int model = MODEL_OCR;

    private ResultMaskView resultMaskView;
    private ResultMaskView realtimeResultMaskView;

    private RecyclerView detectResultView;

    private ImageView resultImage;
    private ViewGroup mResultPageView;
    private DetectResultAdapter adapter;

    boolean isRealtimeStatusRunning = false;

    private static final int REQUEST_PERMISSION_CODE_CAMERA = 100;
    private static final int REQUEST_PERMISSION_CODE_STORAGE = 101;

    private static final int INTENT_CODE_PICK_IMAGE = 100;

    protected static final int PAGE_CAMERA = 100;
    protected static final int PAGE_RESULT = 101;

    private float resultConfidence;

    private float realtimeConfidence = 0.1f;

    Handler uiHandler;

    private String name;

    public abstract void onActivityCreate();

    public abstract void onActivityDestory();

    public abstract void onOcrBitmap(Bitmap bitmap, float confidence, ResultListener.OcrListener listener);

    protected void setConfidence(float value) {
        resultConfidence = value;
        realtimeConfidence = value;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        name = getIntent().getStringExtra("name");
        //model = getIntent().getIntExtra("model_type", MODEL_DETECT);

        init();
    }

    private void init() {
        uiHandler = new Handler(getMainLooper());

        setContentView(R.layout.result_page);
        resultImage = findViewById(R.id.result_image);
        //mResultPageView = findViewById(R.id.result_page);

        resultMaskView = findViewById(R.id.result_mask);
        resultMaskView.setHandler(uiHandler);

        adapter = new DetectResultAdapter(this);
        detectResultView = findViewById(R.id.result_list_view);
        detectResultView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        resultMaskView = findViewById(R.id.result_mask);

        //((TextView) findViewById(R.id.model_name)).setText(name);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        findViewById(R.id.save).setOnClickListener(v -> save(adapter.getData()));

        addListener();
        updateRealtimeResultPopViewGroup();
        onActivityCreate();
    }

    public void save(List<BaseResultModel> models) {
    }

    protected boolean canAutoRun = false;

    private void updateRealtimeResultPopViewGroup() {
        List<Integer> modelList = Arrays.asList(MODEL_OCR);
        if (modelList.contains(model)) {
        }
    }

    protected void showMessage(final String msg) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        runOnUiThread(() -> builder.setTitle("提示")
                .setMessage(msg)
                .setNegativeButton("关闭", (dialog, which) -> {
                    finish();
                })
                .show());

    }

    protected void showMessage(final int errorCode, final String msg) {
        showMessage("errorcode: " + errorCode + ", " + msg);
    }

    public void showResultPage(final Bitmap bitmap) {
        AppExecutors.Companion.getInstance().mainThread().execute(new Runnable() {
            @Override
            public void run() {
                resultImage.setImageBitmap(bitmap);
            }
        });
        resolveDetectResult(bitmap, 0.1f,
                (ResultListener.ListListener<DetectResultModel>) results -> {
                    if (results != null) {
                        updateResultImageAndList(results, bitmap, resultConfidence);
                    }
                });
    }

    private List<BasePolygonResultModel> ocrResultModelCache;

    private void addListener() {
        /*confidenceSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                resultConfidence = seekBar.getProgress() / 100f;
                seekbarText.setText(StringUtil.formatFloatString(resultConfidence));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (model == MODEL_OCR && ocrResultModelCache != null) {
                    updateResultImageAndList(ocrResultModelCache, detectBitmapCache, resultConfidence);
                }
            }
        });*/
    }

    private void resolveDetectResult(Bitmap bitmap, float confidence, final ResultListener.ListListener listener) {
        if (model == MODEL_OCR) {
            onOcrBitmap(bitmap, confidence, models -> {
                if (models == null) {
                    listener.onResult(null);
                    return;
                }
                ocrResultModelCache = models;
                listener.onResult(models);
            });
        }
    }

    private void updateResultImageAndList(List<? extends BaseResultModel> results, final Bitmap bitmap, float min) {
        if (model == MODEL_OCR) {
            List<OcrViewResultModel> filteredList = filterOcrListByConfidence((List<OcrViewResultModel>) results, min);
            fillDetectList(filteredList);
            fillRectImageView(filteredList, bitmap);
        }
    }

    private List<OcrViewResultModel> filterOcrListByConfidence(List<OcrViewResultModel> results, float min) {
        List<OcrViewResultModel> filteredList = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < results.size(); i++) {
            OcrViewResultModel mOcrResultModel = results.get(i);
            if (mOcrResultModel.getConfidence() > min) {
                filteredList.add(new OcrViewResultModel(
                        ++j,
                        mOcrResultModel.getName(),
                        mOcrResultModel.getConfidence(),
                        mOcrResultModel.getBounds()));
            }
        }
        return filteredList;
    }

    private void fillRectImageView(List<? extends BasePolygonResultModel> result, Bitmap bitmap) {
        resultMaskView.setPolygonListInfo((List<BasePolygonResultModel>) result,
                bitmap.getWidth(),
                bitmap.getHeight());
    }

    /**
     * 绘制 编号 名字 置信度的列表
     *
     * @param results
     */
    private void fillDetectList(List<? extends BaseResultModel> results) {
        adapter.setData((List<BaseResultModel>) results);

        uiHandler.post(() -> detectResultView.setAdapter(adapter));
    }

    /**
     * 监听Back键按下事件,方法1:
     * 注意:
     * super.onBackPressed()会自动调用finish()方法,关闭
     * 当前Activity.
     * 若要屏蔽Back键盘,注释该行代码即可
     */
    @Override
    public void onBackPressed() {
        isRealtimeStatusRunning = false;
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRealtimeStatusRunning = false;
        onActivityDestory();
    }
}
