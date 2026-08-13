package ch.chris.einkaufsliste.domain.repository;

import ch.chris.einkaufsliste.domain.entity.ListMember;
import ch.chris.einkaufsliste.domain.entity.ListMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListMemberRepository extends JpaRepository<ListMember, ListMemberId> {

    List<ListMember> findByIdListId(Long listId);

    List<ListMember> findByIdUserId(Long userId);

}
