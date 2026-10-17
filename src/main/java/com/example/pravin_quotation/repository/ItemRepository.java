package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("SELECT i FROM Item i ORDER BY i.name ASC")
    List<Item> findAllByOrderByDisplayOrderAsc();

    @Query("SELECT i FROM Item i WHERE i.active = true ORDER BY i.name ASC")
    List<Item> findByActiveTrueOrderByDisplayOrderAsc();

    @Query("SELECT i FROM Item i WHERE i.division.id = :divisionId ORDER BY i.name ASC")
    List<Item> findByDivisionIdOrderByDisplayOrderAsc(
            @Param("divisionId") Long divisionId
    );

    @Query("SELECT i FROM Item i WHERE i.division.id = :divisionId AND i.active = true ORDER BY i.name ASC")
    List<Item> findByDivisionIdAndActiveTrueOrderByDisplayOrderAsc(
            @Param("divisionId") Long divisionId
    );
}