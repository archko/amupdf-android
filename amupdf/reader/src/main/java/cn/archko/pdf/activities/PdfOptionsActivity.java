package cn.archko.pdf.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.View;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.appbar.MaterialToolbar;
import com.umeng.analytics.MobclickAgent;

import cn.archko.pdf.R;
import cn.archko.pdf.common.SensorHelper;

/**
 * @author: archko 2018/12/12 :15:43
 */
public class PdfOptionsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    public final static String TAG = "PdfOptionsActivity";
    public final static String PREF_SHOW_EXTENSION = "showExtension";
    //public final static String PREF_ORIENTATION = "orientation";
    public final static String PREF_FULLSCREEN = "fullscreen";
    public final static String PREF_AUTOCROP = "autocrop";
    public final static String PREF_VERTICAL_SCROLL_LOCK = "verticalScrollLock";
    public final static String PREF_SIDE_MARGINS = "sideMargins2"; // sideMargins was boolean
    public final static String PREF_TOP_MARGIN = "topMargin";
    public final static String PREF_KEEP_ON = "keepOn";
    public final static String PREF_LIST_STYLE = "list_style";

    private Resources resources;

    private static final String[] summaryKeys = {SensorHelper.PREF_ORIENTATION, PREF_SIDE_MARGINS,
            PREF_TOP_MARGIN, PREF_LIST_STYLE};
    private static final int[] summaryEntryValues = {R.array.opts_orientations, R.array.opts_margins,
            R.array.opts_margins, R.array.opts_list_styles};
    private static final int[] summaryEntries = {R.array.opts_orientation_labels, R.array.opts_margin_labels,
            R.array.opts_margin_labels, R.array.opts_list_style_labels};
    private static final int[] summaryDefaults = {R.string.opts_default_orientation, R.string.opts_default_side_margin,
            R.string.opts_default_top_margin, R.string.opts_default_list_style};

    public String getString(SharedPreferences options, String key) {
        return getString(this.resources, options, key);
    }

    public static String getString(Resources resources, SharedPreferences options, String key) {
        for (int i = 0; i < summaryKeys.length; i++)
            if (summaryKeys[i].equals(key))
                return options.getString(key, resources.getString(summaryDefaults[i]));
        return options.getString(key, "");
    }

    public void setSummaries() {
        for (int i = 0; i < summaryKeys.length; i++) {
            setSummary(i);
        }
    }

    public void setSummary(String key) {
        for (int i = 0; i < summaryKeys.length; i++) {
            if (summaryKeys[i].equals(key)) {
                setSummary(i);
                return;
            }
        }
    }

    public void setSummary(int i) {
        SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(this);

        Preference pref = findPreference(summaryKeys[i]);
        String value = options.getString(summaryKeys[i], resources.getString(summaryDefaults[i]));

        String[] valueArray = resources.getStringArray(summaryEntryValues[i]);
        String[] entryArray = resources.getStringArray(summaryEntries[i]);

        for (int j = 0; j < valueArray.length; j++)
            if (valueArray[j].equals(value)) {
                pref.setSummary(entryArray[j]);
                return;
            }
    }

    private AppCompatDelegate mDelegate;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.preferences);

        mDelegate = AppCompatDelegate.create(this, null);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        mDelegate.setSupportActionBar(toolbar);

        mDelegate.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mDelegate.getSupportActionBar().setDisplayShowHomeEnabled(true);
        mDelegate.getSupportActionBar().setTitle(R.string.options);

        this.resources = getResources();

        addPreferencesFromResource(R.xml.options);
    }

    @Override
    public void onResume() {
        super.onResume();

        //setOrientation(this);

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        setSummaries();
        MobclickAgent.onResume(this); // 基础指标统计，不能遗漏
        MobclickAgent.onPageStart(TAG);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this); // 基础指标统计，不能遗漏
        MobclickAgent.onPageEnd(TAG);
    }

    public void onSharedPreferenceChanged(SharedPreferences options, String key) {
        setSummary(key);
    }

    public static void start(Context context) {
        context.startActivity(new Intent(context, PdfOptionsActivity.class));
    }
}
