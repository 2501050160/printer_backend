package com.saipraveen.login_registration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.saipraveen.login_registration.entity.SystemSetting;
import com.saipraveen.login_registration.repository.SystemSettingRepository;

@Service
public class SystemSettingService {

    @Autowired
    private SystemSettingRepository repository;

    public String getSetting(String key, String defaultValue) {
        return repository.findById(key)
                .map(SystemSetting::getKeyValue)
                .orElse(defaultValue);
    }

    public void setSetting(String key, String value) {
        SystemSetting setting = new SystemSetting(key, value);
        repository.save(setting);
    }

    public boolean getSettingBool(String key, boolean defaultValue) {
        String val = getSetting(key, null);
        return val != null ? Boolean.parseBoolean(val) : defaultValue;
    }

    public double getSettingDouble(String key, double defaultValue) {
        String val = getSetting(key, null);
        try {
            return val != null ? Double.parseDouble(val) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
