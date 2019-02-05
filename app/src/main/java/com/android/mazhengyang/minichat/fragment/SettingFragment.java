package com.android.mazhengyang.minichat.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mazhengyang.minichat.R;
import com.android.mazhengyang.minichat.util.DayNightController;
import com.android.mazhengyang.minichat.util.SharedPreferencesHelper;
import com.android.mazhengyang.minichat.util.SoundController;
import com.android.mazhengyang.minichat.util.VibrateController;
import com.suke.widget.SwitchButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by mazhengyang on 18-12-25.
 */

public class SettingFragment extends Fragment {

    private static final String TAG = "MiniChat." + SettingFragment.class.getSimpleName();

    @BindView(R.id.iv_user_head)
    ImageView ivUserHead;
    @BindView(R.id.tv_user_nickname)
    TextView tvUserNickName;
    @BindView(R.id.sound_switch_button)
    SwitchButton soundSwitchBtn;
    @BindView(R.id.vibrate_switch_button)
    SwitchButton vibrateSwitchBtn;
    @BindView(R.id.daynight_switch_button)
    SwitchButton daynightSwitchBtn;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_setting, null);
        ButterKnife.bind(this, view);

        Context context = getContext();

        tvUserNickName.setText("");
        ivUserHead.setImageResource(R.drawable.user_self);

        //sound
        if (SharedPreferencesHelper.getSoundMode(context)
                == SharedPreferencesHelper.MODE_SOUND_ON) {
            soundSwitchBtn.setChecked(true);
            SoundController.setEnable(true);
        } else {
            soundSwitchBtn.setChecked(false);
            SoundController.setEnable(false);
        }
        soundSwitchBtn.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                if (isChecked) {
                    SharedPreferencesHelper.setSoundMode(getActivity(), SharedPreferencesHelper.MODE_SOUND_ON);
                } else {
                    SharedPreferencesHelper.setSoundMode(getActivity(), SharedPreferencesHelper.MODE_SOUND_OFF);
                }
                SoundController.setEnable(isChecked);
            }
        });

        //vibrate
        if (SharedPreferencesHelper.getVibrateMode(context)
                == SharedPreferencesHelper.MODE_VIBRATE_ON) {
            vibrateSwitchBtn.setChecked(true);
            VibrateController.setEnable(true);
        } else {
            vibrateSwitchBtn.setChecked(false);
            VibrateController.setEnable(false);
        }
        vibrateSwitchBtn.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                if (isChecked) {
                    SharedPreferencesHelper.setVibrateMode(getActivity(), SharedPreferencesHelper.MODE_VIBRATE_ON);
                } else {
                    SharedPreferencesHelper.setVibrateMode(getActivity(), SharedPreferencesHelper.MODE_VIBRATE_OFF);
                }
                VibrateController.setEnable(isChecked);
            }
        });

        //daynight
        if (SharedPreferencesHelper.getDayNightMode(context)
                == SharedPreferencesHelper.MODE_NIGHT) {
            daynightSwitchBtn.setChecked(true);
        } else {
            daynightSwitchBtn.setChecked(false);
        }
        daynightSwitchBtn.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                DayNightController.toggleThemeSetting(getActivity());
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView: ");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
    }

    @OnClick(R.id.head_layout)
    public void headClick() {
        Toast.makeText(getContext(), R.string.wanshan, Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.nickname_layout)
    public void nickNameClick() {
        Toast.makeText(getContext(), R.string.wanshan, Toast.LENGTH_SHORT).show();
    }

}
