package cn.archko.pdf.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.archko.pdf.R;
import cn.archko.pdf.common.PdfOptionKeys;
import cn.archko.pdf.common.PdfOptionRepository;
import cn.archko.pdf.core.adapters.BaseRecyclerAdapter;
import cn.archko.pdf.core.adapters.BaseViewHolder;
import cn.archko.pdf.core.common.Logcat;
import cn.archko.pdf.core.widgets.ColorItemDecoration;

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

        adapter = new BaseRecyclerAdapter<>(this) {
            @NonNull
            @Override
            public BaseViewHolder<Prefs> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                if (viewType == TYPE_LIST) {
                    View view = mInflater.inflate(R.layout.list_preferences, parent, false);
                    return new PrefsListHolder(view);
                } else if (viewType == TYPE_EDIT) {
                    View view = mInflater.inflate(R.layout.list_preferences, parent, false);
                    return new PrefsEditHolder(view);
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

    private List<Prefs> prefsList = new ArrayList<>();

    private void initPrefsFromMMkv() {
        Prefs prefs;
        prefs = new Prefs(TYPE_LIST, getString(R.string.opts_orientation), getString(R.string.opts_orientation),
                PdfOptionKeys.PREF_ORIENTATION,
                getResources().getStringArray(R.array.opts_orientations),
                getResources().getStringArray(R.array.opts_orientation_labels),
                PdfOptionRepository.INSTANCE.getOrientation());
        prefsList.add(prefs);

        //prefs = new Prefs(TYPE_CHECK, getString(R.string.opts_ocr_view), getString(R.string.opts_ocr_view), PdfOptionKeys.PREF_OCR, PdfOptionRepository.INSTANCE.getImageOcr());
        //prefsList.add(prefs);

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

        prefs = new Prefs(TYPE_CHECK, getString(R.string.opts_scan), getString(R.string.opts_scan), PdfOptionKeys.PREF_AUTO_SCAN, PdfOptionRepository.INSTANCE.getAutoScan());
        prefsList.add(prefs);

        prefs = new Prefs(TYPE_EDIT, getString(R.string.opts_scan_folder), getString(R.string.opts_scan_folder), PdfOptionKeys.PREF_SCAN_FOLDER, PdfOptionRepository.INSTANCE.getScanFolder());
        prefsList.add(prefs);

        /*prefs = new Prefs(TYPE_LIST, getString(R.string.opts_list_style), getString(R.string.opts_list_style),
                PdfOptionKeys.PREF_STYLE,
                getResources().getStringArray(R.array.opts_list_styles),
                getResources().getStringArray(R.array.opts_list_style_labels),
                PdfOptionRepository.INSTANCE.getStyle());
        prefsList.add(prefs);*/

        prefs = new Prefs(TYPE_LIST, getString(R.string.opts_color_mode), getString(R.string.opts_color_mode),
                PdfOptionKeys.PREF_COLORMODE,
                getResources().getStringArray(R.array.opts_color_modes),
                getResources().getStringArray(R.array.opts_color_mode_labels),
                PdfOptionRepository.INSTANCE.getColorMode());
        prefsList.add(prefs);
    }

    private static final int TYPE_CHECK = 0;
    private static final int TYPE_LIST = 1;
    private static final int TYPE_EDIT = 2;

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

        @Override
        public String toString() {
            return "Prefs{" +
                    "type=" + type +
                    ", labels=" + Arrays.toString(labels) +
                    ", vals=" + Arrays.toString(vals) +
                    ", key='" + key + '\'' +
                    ", title='" + title + '\'' +
                    ", summary='" + summary + '\'' +
                    ", val=" + val +
                    ", index=" + index +
                    '}';
        }
    }

    private static class AbsPrefsHolder extends BaseViewHolder<Prefs> {

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
            itemView.setOnClickListener(v -> showListDialog(data, summary));
        }

        private void showListDialog(Prefs data, TextView summary) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(PdfOptionsActivity.this);
            builder.setTitle("请选择");
            builder.setSingleChoiceItems(data.labels,
                    data.index,
                    (dialog, which) -> {
                        dialog.dismiss();
                        summary.setText(data.labels[which]);
                        setCheckListVal(data.key, data.vals[which]);
                    });
            builder.create().show();
        }

        private void setCheckListVal(String key, Object val) {
            if (TextUtils.equals(key, PdfOptionKeys.PREF_ORIENTATION)) {
                PdfOptionRepository.INSTANCE.setOrientation(Integer.parseInt(val.toString()));
            } else if (TextUtils.equals(key, PdfOptionKeys.PREF_COLORMODE)) {
                PdfOptionRepository.INSTANCE.setColorMode(Integer.parseInt(val.toString()));
            } else if (TextUtils.equals(key, PdfOptionKeys.PREF_STYLE)) {
                PdfOptionRepository.INSTANCE.setStyle(Integer.parseInt(val.toString()));
            }
        }
    }

    private static class PrefsCheckHolder extends AbsPrefsHolder {

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
            }
        }
    }

    private class PrefsEditHolder extends AbsPrefsHolder {

        public PrefsEditHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void onBind(Prefs data, int position) {
            super.onBind(data, position);
            Logcat.d(String.format("bind:%s", data));
            title.setText(data.title);
            summary.setText(String.format("%s:%s", data.summary, data.val));
            itemView.setOnClickListener(v -> showEditDialog(data, summary));
        }

        private void showEditDialog(Prefs data, TextView summary) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(PdfOptionsActivity.this);
            EditText editText = new EditText(PdfOptionsActivity.this);
            editText.setText(String.valueOf(data.val));
            builder.setTitle("Scan Folder");
            builder.setView(editText);
            builder.setNegativeButton("OK", (dialog, which) -> {
                String path = editText.getText().toString();
                data.val = path;
                setEditVal(data.key, path);
                adapter.notifyDataSetChanged();
                dialog.dismiss();
            });
            builder.setPositiveButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder.create().show();
        }

        private void setEditVal(String key, String val) {
            Logcat.d(String.format("key:%s-%s", key, val));
            if (TextUtils.equals(key, PdfOptionKeys.PREF_SCAN_FOLDER)) {
                PdfOptionRepository.INSTANCE.setScanFolder(val);
            }
        }
    }

    public static void start(Context context) {
        context.startActivity(new Intent(context, PdfOptionsActivity.class));
    }
}
