package org.folio.list.services;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.list.domain.ListEntity;
import org.folio.list.domain.dto.RelatedUser;
import org.folio.list.domain.dto.RelatedUserCollection;
import org.folio.list.repository.ListRepository;
import org.folio.list.rest.UsersClient;
import org.folio.list.rest.UsersClient.User;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserService {
  private final ListRepository listRepository;
  private final UsersClient usersClient;

  public RelatedUserCollection getRelatedUsersByRole(String role) {
    return switch (role) {
      case "create" -> getRelatedUsers(ListEntity::getCreatedBy);
      case "update" -> getRelatedUsers(ListEntity::getUpdatedBy);
      default -> new RelatedUserCollection();
    };
  }

  private RelatedUserCollection getRelatedUsers(Function<ListEntity, UUID> function) {
    var users = StreamSupport.stream(listRepository.findAll().spliterator(), false)
      .map(function)
      .filter(Objects::nonNull)
      .distinct()
      .map(usersClient::getUser)
      .filter(user -> user.getFullName().isPresent())
      .map(user -> new RelatedUser()
        .id(user.id().toString())
        .fullName(user.getFullName().get()))
      .toList();
    return new RelatedUserCollection().relatedUsers(users).totalRecords(users.size());
  }
}
