package ch.chris.einkaufsliste.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ListMemberId implements Serializable {

    @Column(name = "list_id")
    private Long listId;

    @Column(name = "user_id")
    private Long userId;

    protected ListMemberId() {
        // fuer JPA
    }

    public ListMemberId(Long listId, Long userId) {
        this.listId = listId;
        this.userId = userId;
    }

    public Long getListId() {
        return listId;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ListMemberId that)) return false;
        return Objects.equals(listId, that.listId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(listId, userId);
    }

}
