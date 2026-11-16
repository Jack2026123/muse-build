package com.muse.android.tv.ui.dialog;

import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.muse.android.tv.App;
import com.muse.android.tv.R;
import com.muse.android.tv.api.config.VodConfig;
import com.muse.android.tv.api.loader.LocalSourceHub;
import com.muse.android.tv.databinding.DialogWebSourceBinding;
import com.muse.android.tv.impl.Callback;
import com.muse.android.tv.utils.Notify;
import com.muse.android.tv.utils.ResUtil;

/**
 * v28 网页源：电视设置页入口（手机版 v62 在设置页，电视按 leanback 写法落）。
 * 输网址（必填）+ 名称（可选，留空自动取域名），追加到当前配置 sites 数组末尾，
 * 写完重载配置，新站立刻出现在切站列表里。逻辑与手机版 WebSourceDialog 一致，
 * 控件换成 leanback 弹窗形态（BaseAlertDialog + CustomEditText + 遥控器按钮）。
 */
public class WebSourceDialog extends BaseAlertDialog {

    private DialogWebSourceBinding binding;

    public static void show(FragmentActivity activity) {
        new WebSourceDialog().show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogWebSourceBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setTitle(R.string.site_web_source_title).setView(getBinding().getRoot());
    }

    @Override
    protected void initView() {
        binding.url.requestFocus();
    }

    @Override
    protected void initEvent() {
        binding.positive.setOnClickListener(this::onPositive);
        binding.negative.setOnClickListener(view -> dismiss());
        binding.name.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) binding.positive.performClick();
            return true;
        });
    }

    private void onPositive(View view) {
        String rawUrl = binding.url.getText() == null ? "" : binding.url.getText().toString();
        String webUrl = LocalSourceHub.normalizeWebSourceUrl(rawUrl);
        if (webUrl == null) {
            Notify.show(isBlank(rawUrl) ? R.string.site_web_source_url_empty : R.string.site_web_source_url_invalid);
            return;
        }
        String rawName = binding.name.getText() == null ? "" : binding.name.getText().toString().trim();
        String webName = rawName.isEmpty() ? LocalSourceHub.deriveWebSourceName(webUrl) : rawName;
        if (webName.isEmpty()) {
            Notify.show(R.string.site_web_source_url_invalid);
            return;
        }
        dismiss();
        addWebSource(webName, webUrl);
    }

    private void addWebSource(String name, String url) {
        new Thread(() -> {
            final int result = LocalSourceHub.appendWebSource(name, url);
            App.post(() -> {
                if (result == LocalSourceHub.WEB_OK) {
                    Notify.show(ResUtil.getString(R.string.site_web_source_done, name));
                    // 写完重载配置：新站立刻进切站列表，不用手动刷新
                    VodConfig.get().init().load(new Callback() {
                        @Override
                        public void start() {
                        }

                        @Override
                        public void success() {
                        }
                    });
                } else if (result == LocalSourceHub.WEB_EXISTS) Notify.show(R.string.site_web_source_exists);
                else if (result == LocalSourceHub.WEB_NETWORK) Notify.show(R.string.site_web_source_network);
                else if (result == LocalSourceHub.WEB_NO_CONFIG) Notify.show(R.string.site_web_source_no_config);
                else Notify.show(R.string.site_web_source_fail);
            });
        }, "local-source-websource").start();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public void onStart() {
        super.onStart();
        setWidth(0.55f);
    }
}
