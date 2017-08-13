package com.cooper.wheellog;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.cooper.wheellog.utils.Constants;
import com.cooper.wheellog.utils.SettingsUtil;
import com.pavelsikun.seekbarpreference.SeekBarPreference;

public class PreferencesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    enum SettingsScreen {
        Main,
        Speed,
        Logs,
        Alarms,
        Watch,
		Wheel
    }

    private boolean mDataWarningDisplayed = false;
    private SettingsScreen currentScreen = SettingsScreen.Main;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        setup_screen();
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case "alarms_enabled":
                hideShowSeekBars();
                break;
            case "auto_upload":
                if (SettingsUtil.isAutoUploadEnabled(getActivity()) && !mDataWarningDisplayed) {
                    SettingsUtil.setAutoUploadEnabled(getActivity(), false);
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Enable Auto Upload?")
                            .setMessage("Automatic uploading while not connected to WiFi will use your mobile data.  This may result in charges from your network provider if you do not have a data plan.")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    mDataWarningDisplayed = true;
                                    SettingsUtil.setAutoUploadEnabled(getActivity(), true);
                                    refreshVolatileSettings();
                                    getActivity().sendBroadcast(new Intent(Constants.ACTION_PREFERENCE_CHANGED));
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    mDataWarningDisplayed = false;
                                    refreshVolatileSettings();
                                    getActivity().sendBroadcast(new Intent(Constants.ACTION_PREFERENCE_CHANGED));
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .show();
                } else
                    mDataWarningDisplayed = false;
                break;
            case "use_mph":
                getActivity().sendBroadcast(new Intent(Constants.ACTION_PEBBLE_AFFECTING_PREFERENCE_CHANGED));
                break;
            case "max_speed":
                getActivity().sendBroadcast(new Intent(Constants.ACTION_PEBBLE_AFFECTING_PREFERENCE_CHANGED));
                break;
			case "light_enabled":
				getActivity().sendBroadcast(new Intent(Constants.ACTION_WHEEL_SETTING_CHANGED).putExtra(Constants.INTENT_EXTRA_WHEEL_LIGHT, true));
				break;
			case "led_enabled":
				getActivity().sendBroadcast(new Intent(Constants.ACTION_WHEEL_SETTING_CHANGED).putExtra(Constants.INTENT_EXTRA_WHEEL_LED, true));
				break;
			case "handle_button_disabled":
				getActivity().sendBroadcast(new Intent(Constants.ACTION_WHEEL_SETTING_CHANGED).putExtra(Constants.INTENT_EXTRA_WHEEL_BUTTON, true));
				break;
			case "wheel_max_speed":
				new AlertDialog.Builder(getActivity())
                            .setTitle("Are you sure?")
                            .setMessage("Setting a speed limit higher than 30 km/h is unsafe, this is an undocumented feature, USE IT ON YOUR OWN RISK. Neither the Inmotion Company nor the developers of this application are liable for any damages to your health, EUC or third party resulting by using of this feature.")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    getActivity().sendBroadcast(new Intent(Constants.ACTION_WHEEL_SETTING_CHANGED).putExtra(Constants.INTENT_EXTRA_WHEEL_MAX_SPEED, true));
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    getActivity().sendBroadcast(new Intent(Constants.ACTION_WHEEL_SETTING_CHANGED).putExtra(Constants.INTENT_EXTRA_WHEEL_REFRESH, true));
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .show();
				//getActivity().sendBroadcast(new Intent(Constants.ACTION_WHEEL_SETTING_CHANGED).putExtra(Constants.INTENT_EXTRA_WHEEL_MAX_SPEED, true));
				break;
			case "speaker_volume":
				getActivity().sendBroadcast(new Intent(Constants.ACTION_WHEEL_SETTING_CHANGED).putExtra(Constants.INTENT_EXTRA_WHEEL_SPEAKER_VOLUME, true));
				break;
        }
        getActivity().sendBroadcast(new Intent(Constants.ACTION_PREFERENCE_CHANGED));
    }

    private void setup_screen() {
        Toolbar tb = (Toolbar) getActivity().findViewById(R.id.preference_toolbar);
        if (currentScreen == SettingsScreen.Main)
            tb.setNavigationIcon(null);
        else {
            tb.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
            tb.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    show_main_menu();
                }
            });
        }

        switch (currentScreen) {
            case Main:
                tb.setTitle("Settings");
                Preference speed_button = findPreference(getString(R.string.speed_preferences));
                Preference logs_button = findPreference(getString(R.string.log_preferences));
                Preference alarm_button = findPreference(getString(R.string.alarm_preferences));
                Preference watch_button = findPreference(getString(R.string.watch_preferences));
				Preference wheel_button = findPreference(getString(R.string.wheel_settings));

                if (speed_button != null) {
                    speed_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            currentScreen = SettingsScreen.Speed;
                            getPreferenceScreen().removeAll();
                            addPreferencesFromResource(R.xml.preferences_speed);
                            setup_screen();
                            return true;
                        }
                    });
                }
                if (logs_button != null) {
                    logs_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            currentScreen = SettingsScreen.Logs;
                            getPreferenceScreen().removeAll();
                            addPreferencesFromResource(R.xml.preferences_logs);
                            setup_screen();
                            return true;
                        }
                    });
                }
                if (alarm_button != null) {
                    alarm_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            currentScreen = SettingsScreen.Alarms;
                            getPreferenceScreen().removeAll();
                            addPreferencesFromResource(R.xml.preferences_alarms);
                            setup_screen();
                            return true;
                        }
                    });
                }
                if (watch_button != null) {
                    watch_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            currentScreen = SettingsScreen.Watch;
                            getPreferenceScreen().removeAll();
                            addPreferencesFromResource(R.xml.preferences_watch);
                            setup_screen();
                            return true;
                        }
                    });
                }
		        if (wheel_button != null) {
                    wheel_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            currentScreen = SettingsScreen.Wheel;
                            getPreferenceScreen().removeAll();
                            addPreferencesFromResource(R.xml.preferences_inmotionwheel);
                            setup_screen();
                            return true;
                        }
                    });
                }
				
                break;
            case Speed:
                tb.setTitle("Speed Settings");
                break;
            case Logs:
                tb.setTitle("Log Settings");
                break;
            case Alarms:
                tb.setTitle("Alarm Settings");
                hideShowSeekBars();
                break;
            case Watch:
                tb.setTitle("Watch Settings");
                break;
			case Wheel:
                tb.setTitle("Wheel Settings");
				getActivity().sendBroadcast(new Intent(Constants.ACTION_WHEEL_SETTING_CHANGED).putExtra(Constants.INTENT_EXTRA_WHEEL_REFRESH, true));
                break;
        }
    }

    public void refreshVolatileSettings() {
        if (currentScreen == SettingsScreen.Logs) {
            correctCheckState(getString(R.string.auto_log));
            correctCheckState(getString(R.string.log_location_data));
            correctCheckState(getString(R.string.auto_upload));
        }
    }
	
    public void refreshWheelSettings(boolean isLight, boolean isLed, boolean isButton, int maxSpeed, int speakerVolume) {
        correctWheelCheckState(getString(R.string.light_enabled), isLight);
        correctWheelCheckState(getString(R.string.led_enabled), isLed);
        correctWheelCheckState(getString(R.string.handle_button_disabled), isButton);
        correctWheelBarState(getString(R.string.wheel_max_speed), maxSpeed);
		correctWheelBarState(getString(R.string.speaker_volume), speakerVolume);

    }	
	
	private void correctWheelCheckState(String preference, boolean state) {
        CheckBoxPreference cb_preference = (CheckBoxPreference) findPreference(preference);
        if (cb_preference == null)
            return;

        boolean check_state = cb_preference.isChecked();

        if (state != check_state)
            cb_preference.setChecked(state);
    }
	
	private void correctWheelBarState(String preference, int state) {
        SeekBarPreference sb_preference = (SeekBarPreference) findPreference(preference);
		if (sb_preference == null)
			return;
		int sb_value = sb_preference.getCurrentValue();
		if (state != sb_value)
			sb_preference.setCurrentValue(state);
			
    }

    private void correctCheckState(String preference) {
        boolean setting_state = SettingsUtil.getBoolean(getActivity(), preference);
        CheckBoxPreference cb_preference = (CheckBoxPreference) findPreference(preference);
        if (cb_preference == null)
            return;

        boolean check_state = cb_preference.isChecked();

        if (setting_state != check_state)
            cb_preference.setChecked(setting_state);
    }

    private void hideShowSeekBars() {
        boolean alarms_enabled = getPreferenceManager().getSharedPreferences()
                .getBoolean(getString(R.string.alarms_enabled), false);
        String[] seekbar_preferences = {
                getString(R.string.alarm_1_speed),
                getString(R.string.alarm_2_speed),
                getString(R.string.alarm_3_speed),
                getString(R.string.alarm_1_battery),
                getString(R.string.alarm_2_battery),
                getString(R.string.alarm_3_battery),
                getString(R.string.alarm_current)};

        for (String preference : seekbar_preferences) {
            SeekBarPreference seekbar = (SeekBarPreference) findPreference(preference);
            if (seekbar != null)
                seekbar.setEnabled(alarms_enabled);
        }
    }

    public boolean show_main_menu() {
        if (currentScreen == SettingsScreen.Main)
            return false;
        getPreferenceScreen().removeAll();
        addPreferencesFromResource(R.xml.preferences);
        currentScreen = SettingsScreen.Main;
        setup_screen();
        return true;
    }
}
