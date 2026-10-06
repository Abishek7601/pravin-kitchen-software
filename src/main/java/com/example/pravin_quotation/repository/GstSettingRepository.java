package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.GstSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GstSettingRepository extends JpaRepository<GstSetting, Long> {

    List<GstSetting> findAllByOrderByCreatedAtDesc();

    List<GstSetting> findByActiveTrueOrderByCreatedAtDesc();

    Optional<GstSetting> findFirstByActiveTrueOrderByCreatedAtDesc();
}