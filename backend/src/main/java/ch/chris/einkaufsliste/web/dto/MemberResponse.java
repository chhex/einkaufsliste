package ch.chris.einkaufsliste.web.dto;

import ch.chris.einkaufsliste.domain.entity.ListMember;

public record MemberResponse(
        Long userId,
        String name,
        String email,
        String sortierung
) {
    public static MemberResponse from(ListMember member) {
        return new MemberResponse(
                member.getUser().getId(),
                member.getUser().getName(),
                member.getUser().getEmail(),
                member.getSortierung() == null ? null : member.getSortierung().name()
        );
    }
}
