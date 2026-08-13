package ch.chris.einkaufsliste.domain.repository;

import ch.chris.einkaufsliste.domain.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByListIdOrderByKategorieAscBezeichnungAsc(Long listId);

    List<Item> findByListIdOrderByBezeichnungAsc(Long listId);

    List<Item> findByListIdOrderByEinheitAscBezeichnungAsc(Long listId);

    List<Item> findByListIdAndAbgehakt(Long listId, boolean abgehakt);

    long countByListIdAndAbgehakt(Long listId, boolean abgehakt);

}
