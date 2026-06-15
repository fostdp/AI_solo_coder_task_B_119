package com.metrology.balance.repository;

import com.metrology.balance.entity.VirtualWeighingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VirtualWeighingItemRepository extends JpaRepository<VirtualWeighingItem, Integer> {

    Optional<VirtualWeighingItem> findByItemCode(String itemCode);

    List<VirtualWeighingItem> findByIsActiveTrueOrderByDisplayOrderAsc();

    List<VirtualWeighingItem> findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(String category);

    List<VirtualWeighingItem> findByCivilizationAndIsActiveTrueOrderByDisplayOrderAsc(String civilization);

    List<VirtualWeighingItem> findByCategoryOrderByDisplayOrderAsc(String category);
}
