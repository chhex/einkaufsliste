package ch.chris.einkaufsliste.domain.repository;

import ch.chris.einkaufsliste.domain.entity.ShoppingList;
import ch.chris.einkaufsliste.domain.enums.ListStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {

    List<ShoppingList> findByOwnerId(Long ownerId);

    List<ShoppingList> findByStatus(ListStatus status);

    /**
     * Alle Listen, auf die ein User Zugriff hat - als Owner ODER als Member.
     */
    @Query("""
            SELECT l FROM ShoppingList l
            WHERE l.owner.id = :userId
               OR l.id IN (SELECT m.id.listId FROM ListMember m WHERE m.id.userId = :userId)
            """)
    List<ShoppingList> findAccessibleByUserId(@Param("userId") Long userId);

}
