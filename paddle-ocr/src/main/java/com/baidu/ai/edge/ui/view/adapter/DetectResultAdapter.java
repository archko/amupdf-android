package com.baidu.ai.edge.ui.view.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.ai.edge.ui.R;
import com.baidu.ai.edge.ui.util.StringUtil;
import com.baidu.ai.edge.ui.view.model.BaseResultModel;

import java.util.List;

import androidx.annotation.NonNull;
import cn.archko.pdf.core.App;
import cn.archko.pdf.core.adapters.BaseRecyclerAdapter;
import cn.archko.pdf.core.adapters.BaseViewHolder;

public class DetectResultAdapter extends BaseRecyclerAdapter<BaseResultModel> {

    private ClipboardManager clipboardManager;

    public DetectResultAdapter(Context context) {
        super(context);
    }

    public DetectResultAdapter(Context context, List<BaseResultModel> data) {
        super(context, data);
    }

    @NonNull
    @Override
    public BaseViewHolder<BaseResultModel> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.result_detect_item, parent, false);
        Holder holder = new Holder(view);
        return holder;
    }

    private class Holder extends BaseViewHolder<BaseResultModel> {

        TextView indexText;
        TextView nameText;
        TextView confidenceText;

        public Holder(View view) {
            super(view);
            indexText = view.findViewById(R.id.index);
            nameText = view.findViewById(R.id.name);
            confidenceText = view.findViewById(R.id.confidence);
        }

        @Override
        public void onBind(BaseResultModel model, int position) {
            indexText.setText(String.valueOf(model.getIndex()));
            nameText.setText(String.valueOf(model.getName()));
            confidenceText.setText(StringUtil.formatFloatString(model.getConfidence()));
            itemView.setOnClickListener(v -> saveToClip(model));
        }

        private void saveToClip(BaseResultModel model) {
            if (null == clipboardManager) {
                clipboardManager = (ClipboardManager) App.Companion.getInstance().getSystemService(Context.CLIPBOARD_SERVICE);
            }
            clipboardManager.setPrimaryClip(ClipData.newPlainText("Label", model.getName()));
            Toast.makeText(App.Companion.getInstance(), "save text to clipboard", Toast.LENGTH_SHORT).show();
        }
    }
}
