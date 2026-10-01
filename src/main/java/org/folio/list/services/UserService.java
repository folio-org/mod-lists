package org.folio.list.services;

import static java.lang.String.format;
import static java.lang.String.join;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.list.domain.ListEntity;
import org.folio.list.domain.dto.RelatedUser;
import org.folio.list.domain.dto.RelatedUserCollection;
import org.folio.list.repository.ListRepository;
import org.folio.list.rest.UsersClient;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserService {
  private static final String QUERY = "id==(%s)";
  private static final String OR_DELIMETER = " or ";
  private static final int CHUNK_SIZE = 25;

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
    var ids = StreamSupport.stream(listRepository.findAll().spliterator(), false)
      .map(function)
      .filter(Objects::nonNull)
      .map(UUID::toString)
      .distinct()
      .toList();
    var chunks =
      IntStream.range(0, (ids.size() + CHUNK_SIZE - 1) / CHUNK_SIZE)
        .mapToObj(i -> ids.subList(i * CHUNK_SIZE, Math.min((i + 1) * CHUNK_SIZE, ids.size())))
        .toList();
    var users =
      chunks.stream()
        .flatMap(idList -> usersClient
          .getByQuery(format(QUERY, join(OR_DELIMETER, idList)), idList.size()).users().stream())
        .filter(user -> user.getFullName().isPresent())
        .map(user -> new RelatedUser()
          .id(user.id().toString())
          .fullName(user.getFullName().get()))
        .toList();
    return new RelatedUserCollection().relatedUsers(users).totalRecords(users.size());
  }
}
