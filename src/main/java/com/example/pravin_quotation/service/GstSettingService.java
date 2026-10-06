package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.GstSetting;
import com.example.pravin_quotation.repository.GstSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GstSettingService {

    private final GstSettingRepository gstSettingRepository;

    public GstSettingService(GstSettingRepository gstSettingRepository) {
        this.gstSettingRepository = gstSettingRepository;
    }

    public List<GstSetting> getAllGstSettings() {
        return gstSettingRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<GstSetting> getActiveGstSettings() {
        return gstSettingRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    public GstSetting getById(Long id) {

        if (id == null) {
            throw new IllegalArgumentException("GST setting ID is required.");
        }

        return gstSettingRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("GST setting not found."));
    }

    public GstSetting getActiveGstSetting() {
        return gstSettingRepository
                .findFirstByActiveTrueOrderByCreatedAtDesc()
                .orElse(null);
    }

    @Transactional
    public GstSetting create(
            BigDecimal gstPercentage,
            String description
    ) {

        validateGstPercentage(gstPercentage);

        GstSetting gstSetting = new GstSetting();

        gstSetting.setGstPercentage(gstPercentage);
        gstSetting.setDescription(cleanText(description));
        gstSetting.setActive(true);

        /*
         * Only one GST setting should normally be active.
         * Existing active settings are deactivated when
         * a new GST setting is created.
         */
        deactivateAll();

        return gstSettingRepository.save(gstSetting);
    }

    @Transactional
    public GstSetting update(
            Long id,
            BigDecimal gstPercentage,
            String description
    ) {

        GstSetting gstSetting = getById(id);

        validateGstPercentage(gstPercentage);

        gstSetting.setGstPercentage(gstPercentage);
        gstSetting.setDescription(cleanText(description));

        return gstSettingRepository.save(gstSetting);
    }

    @Transactional
    public GstSetting activate(Long id) {

        GstSetting gstSetting = getById(id);

        /*
         * Only one GST rate should be active at a time.
         */
        deactivateAll();

        gstSetting.setActive(true);

        return gstSettingRepository.save(gstSetting);
    }

    @Transactional
    public GstSetting deactivate(Long id) {

        GstSetting gstSetting = getById(id);

        gstSetting.setActive(false);

        return gstSettingRepository.save(gstSetting);
    }

    @Transactional
    public void delete(Long id) {

        GstSetting gstSetting = getById(id);

        gstSettingRepository.delete(gstSetting);
    }

    private void deactivateAll() {

        List<GstSetting> activeSettings =
                gstSettingRepository.findByActiveTrueOrderByCreatedAtDesc();

        for (GstSetting setting : activeSettings) {
            setting.setActive(false);
        }

        if (!activeSettings.isEmpty()) {
            gstSettingRepository.saveAll(activeSettings);
        }
    }

    private void validateGstPercentage(BigDecimal gstPercentage) {

        if (gstPercentage == null) {
            throw new IllegalArgumentException(
                    "GST percentage is required."
            );
        }

        if (gstPercentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "GST percentage cannot be negative."
            );
        }

        if (gstPercentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(
                    "GST percentage cannot be greater than 100."
            );
        }
    }

    private String cleanText(String value) {

        if (value == null) {
            return null;
        }

        String cleaned = value.trim();

        return cleaned.isEmpty() ? null : cleaned;
    }
}