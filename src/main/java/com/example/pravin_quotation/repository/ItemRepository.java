package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Optional<Item> findByNameIgnoreCaseAndDivisionId(
            String name,
            Long divisionId
    );

    boolean existsByNameIgnoreCaseAndDivisionId(
            String name,
            Long divisionId
    );

    boolean existsByNameIgnoreCaseAndDivisionIdAndIdNot(
            String name,
            Long divisionId,
            Long id
    );

    List<Item> findAllByOrderByDisplayOrderAsc();

    List<Item> findByActiveTrueOrderByDisplayOrderAsc();

    List<Item> findByDivisionIdOrderByDisplayOrderAsc(
            Long divisionId
    );

    List<Item> findByDivisionIdAndActiveTrueOrderByDisplayOrderAsc(
            Long divisionId
    );
}