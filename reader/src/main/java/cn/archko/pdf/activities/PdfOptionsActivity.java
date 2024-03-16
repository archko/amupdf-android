package cn.archko.pdf.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cn.archko.pdf.R;
import cn.archko.pdf.adapters.BaseRecyclerAdapter;
import cn.archko.pdf.adapters.BaseViewHolder;
import cn.archko.pdf.common.PdfOptionKeys;
import cn.archko.pdf.common.PdfOptionRepository;
import cn.archko.pdf.widgets.ColorItemDecoration;

/**
 * @author: archko 2018/12/12 :15:43
 */
public class PdfOptionsActivity extends FragmentActivity {

    public final static String TAG = "PdfOptionsActivity";

    private BaseRecyclerAdapter<Prefs> adapter;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.preferences);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());

        RecyclerView recyclerView = findViewById(android.R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new ColorItemDecoration(this));

        adapter = new BaseRecyclerAdapter<Prefs>(this) {
            @NonNull
            @Override
            public BaseViewHolder<Prefs> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                if (viewType == TYPE_LIST) {
                    View view = mInflater.inflate(R.layout.list_preferences, parent, false);
                    return new PrefsListHolder(view);
                } else {
                    View view = mInflater.inflate(R.layout.check_preferences, parent, false);
                    return new PrefsCheckHolder(view);
                }
            }

            @Override
            public int getItemViewType(int position) {
                Prefs prefs = getData().get(position);
                return prefs.type;
            }
        };

        initPrefsFromMMkv();
        adapter.setData(prefsList);
        recyclerView.setAdapter(adapter);
    }

    List<Prefs> prefsList = new ArrayList<>();

    private void initPrefsFromMMkv() {
        Prefs prefs;
        prefs = new Prefs(TYPE_LIST, getString(R.string.opts_orientation), getString(R.string.opts_orientation),
                PdfOptionKeys.PREF_ORIENTATION,
                getResources().getStringArray(R.array.opts_orientations),
                getResources().getStringArray(R.array.opts_orientation_labels),
                PdfOptionRepository.INSTANCE.getOrientation());
        prefsList.add(prefs);

        prefs = new Prefs(TYPE_CHECK, getString(R.string.opts_ocr_view), getString(R.string.opts_ocr_view), PdfOptionKeys.PREF_OCR, PdfOptionRepository.INSTANCE.getImageOcr());
        prefsList.add(prefs);

        prefs = new Prefs(TYPE_CHECK, getString(R.string.opts_fullscreen), getString(R.string.opts_fullscreen), PdfOptionKeys.PREF_FULLSCREEN, PdfOptionRepository.INSTANCE.getFullscreen());
        prefsList.add(prefs);

        prefs = new Prefs(TYPE_CHECK, getString(R.string.opts_autocrop), getString(R.string.opts_autocrop_summary), PdfOptionKeys.PREF_AUTOCROP, PdfOptionRepository.INSTANCE.getAutocrop());
        prefsList.add(prefs);

        prefs = new Prefs(TYPE_CHECK, getString(R.string.opts_keep_on), getString(R.string.opts_keep_on), PdfOptionKeys.PREF_KEEP_ON, PdfOptionRepository.INSTANCE.getKeepOn());
        prefsList.add(prefs);

        prefs = new Prefs(TYPE_CHECK, getString(R.string.opts_dirs_first), getString(R.string.opts_dirs_first), PdfOptionKeys.PREF_DIRS_FIRST, PdfOptionRepository.INSTANCE.getDirsFirst());
        prefsList.add(prefs);

        prefs = new Prefs(TYPE_CHECK, getString(R.string.opts_show_extension), getString(R.string.opts_show_extension), PdfOptionKeys.PREF_SHOW_EXTENSION, PdfOptionRepository.INSTANCE.getShowExtension());
        prefsList.add(prefs);

        //prefs = new Prefs(TYPE_CHECK, getString(R.string.opts_new_viewer), getString(R.string.opts_new_viewer), PdfOptionKeys.PREF_NEW_VIEWER, PdfOptionRepository.INSTANCE.getNewViewer());
        //prefsList.add(prefs);

        prefs = new Prefs(TYPE_CHECK, getString(R.string.opts_cropper), getString(R.string.opts_cropper), PdfOptionKeys.PREF_CROPPER, PdfOptionRepository.INSTANCE.getCropper());
        prefsList.add(prefs);
    }

    private static final int TYPE_CHECK = 0;

    private static final int TYPE_LIST = 1;

    private static class Prefs {
        int type = TYPE_CHECK;
        String[] labels;
        Object[] vals;
        String key;
        String title;
        String summary;
        Object val;
        int index;

        public Prefs(int type, String title, String summary, String key, Object val) {
            this.type = type;
            this.title = title;
            this.summary = summary;
            this.key = key;
            this.val = val;
        }

        public Prefs(int type, String title, String summary, String key, Object[] vals, String[] labels, int index) {
            this.type = type;
            this.title = title;
            this.summary = summary;
            this.key = key;
            this.vals = vals;
            this.labels = labels;
            this.index = index;
        }
    }

    private class AbsPrefsHolder extends BaseViewHolder<Prefs> {

        TextView title;
        TextView summary;

        public AbsPrefsHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            summary = itemView.findViewById(R.id.summary);
        }

        @Override
        public void onBind(Prefs data, int position) {
            title.setText(data.title);
            summary.setText(data.summary);
        }
    }

    private class PrefsListHolder extends AbsPrefsHolder {

        public PrefsListHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void onBind(Prefs data, int position) {
            super.onBind(data, position);
            summary.setText(data.labels[data.index]);
            itemView.setOnClickListener(v -> showListDialog(v, data, summary));
        }

        private void showListDialog(View v, Prefs data, TextView summary) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(PdfOptionsActivity.this);
            builder.setTitle("请选择");
            builder.setSingleChoiceItems(data.labels,
                    data.index,
                    (dialog, which) -> {
                        dialog.dismiss();
                        summary.setText(data.labels[which]);
                        setCheckVal(data.key, data.vals[which]);
                    });
            builder.create().show();
        }

        private void setCheckVal(String key, Object val) {
            if (TextUtils.equals(key, PdfOptionKeys.PREF_ORIENTATION)) {
                PdfOptionRepository.INSTANCE.setOrientation(Integer.parseInt(val.toString()));
            }
        }
    }

    private class PrefsCheckHolder extends AbsPrefsHolder {

        private CheckBox checkBox;

        public PrefsCheckHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkbox);
        }

        @Override
        public void onBind(Prefs data, int position) {
            super.onBind(data, position);
            setCheckVal(data.key, (boolean) data.val);
            checkBox.setChecked((boolean) data.val);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> setCheckVal(data.key, isChecked));
        }

        private void setCheckVal(String key, boolean val) {
            if (TextUtils.equals(key, PdfOptionKeys.PREF_OCR)) {
                PdfOptionRepository.INSTANCE.setImageOcr(val);
            } else if (TextUtils.equals(key, PdfOptionKeys.PREF_FULLSCREEN)) {
                PdfOptionRepository.INSTANCE.setFullscreen(val);
            } else if (TextUtils.equals(key, PdfOptionKeys.PREF_AUTOCROP)) {
                PdfOptionRepository.INSTANCE.setAutocrop(val);
            } else if (TextUtils.equals(key, PdfOptionKeys.PREF_KEEP_ON)) {
                PdfOptionRepository.INSTANCE.setKeepOn(val);
            } else if (TextUtils.equals(key, PdfOptionKeys.PREF_DIRS_FIRST)) {
                PdfOptionRepository.INSTANCE.setDirsFirst(val);
            } else if (TextUtils.equals(key, PdfOptionKeys.PREF_SHOW_EXTENSION)) {
                PdfOptionRepository.INSTANCE.setShowExtension(val);
            } else if (TextUtils.equals(key, PdfOptionKeys.PREF_NEW_VIEWER)) {
                PdfOptionRepository.INSTANCE.setNewViewer(val);
            } else if (TextUtils.equals(key, PdfOptionKeys.PREF_CROPPER)) {
                PdfOptionRepository.INSTANCE.setCropper(val);
            }
        }
    }

    public static void start(Context context) {
        context.startActivity(new Intent(context, PdfOptionsActivity.class));
    }
}
