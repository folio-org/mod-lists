package org.folio.list.domain;

import org.folio.list.domain.dto.ListUpdateRequestDTO;
import org.folio.list.exception.AbstractListException;
import org.folio.list.exception.MaxListSizeExceededException;
import org.folio.list.rest.UsersClient;
import org.folio.list.util.TaskTimer;
import org.folio.list.utils.TestDataFixture;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ListEntityTest {

  @Test
  void shouldCompleteRefresh_listRefreshedFirstTime() {
    int recordsCount = 5;
    ListEntity refreshingFirstTime = TestDataFixture.getListEntityWithInProgressRefresh();

    refreshingFirstTime.refreshCompleted(recordsCount, new TaskTimer());

    assertThat(refreshingFirstTime.getInProgressRefresh()).isNull();
    assertThat(refreshingFirstTime.getFailedRefresh()).isNull();
    assertThat(refreshingFirstTime.getSuccessRefresh().getStatus()).isEqualTo(AsyncProcessStatus.SUCCESS);
    assertThat(refreshingFirstTime.getSuccessRefresh().getRefreshEndDate()).isNotNull();
    assertThat(refreshingFirstTime.getSuccessRefresh().getRecordsCount()).isEqualTo(recordsCount);
    assertThat(refreshingFirstTime.getSuccessRefresh().getContentVersion()).isEqualTo(1);
  }

  @Test
  void shouldCompleteRefresh_refreshingAlreadyRefreshedList() {
    int recordsCount = 5;
    ListEntity alreadyRefreshedList = TestDataFixture.getListEntityWithInProgressRefresh();
    ListRefreshDetails successRefresh = TestDataFixture.getListEntityWithSuccessRefresh().getSuccessRefresh();
    alreadyRefreshedList.setSuccessRefresh(successRefresh);


    alreadyRefreshedList.refreshCompleted(recordsCount, new TaskTimer());

    assertThat(alreadyRefreshedList.getInProgressRefresh()).isNull();
    assertThat(alreadyRefreshedList.getFailedRefresh()).isNull();
    assertThat(alreadyRefreshedList.getSuccessRefresh().getStatus()).isEqualTo(AsyncProcessStatus.SUCCESS);
    assertThat(alreadyRefreshedList.getSuccessRefresh().getRefreshEndDate()).isNotNull();
    assertThat(alreadyRefreshedList.getSuccessRefresh().getRecordsCount()).isEqualTo(recordsCount);
    assertThat(alreadyRefreshedList.getSuccessRefresh().getContentVersion()).isEqualTo(successRefresh.getContentVersion() + 1);
  }

  @Test
  void testGetContentVersion() {
    ListEntity refreshingList = TestDataFixture.getListEntityWithInProgressRefresh();
    ListRefreshDetails successRefresh = TestDataFixture.getListEntityWithSuccessRefresh().getSuccessRefresh();
    refreshingList.setSuccessRefresh(successRefresh);
    assertThat(refreshingList.getSuccessRefresh().getContentVersion()).isEqualTo(successRefresh.getContentVersion());
  }

  @Test
  void testRefreshFailed() {
    ListEntity entity = TestDataFixture.getListEntityWithInProgressRefresh();
    String expectedErrorCode = "unexpected.error";
    Throwable failureReason = new RuntimeException("Something Went Wrong");

    entity.refreshFailed(failureReason, new TaskTimer());

    assertThat(entity.getInProgressRefresh()).isNull();
    assertThat(entity.getFailedRefresh().getStatus()).isEqualTo(AsyncProcessStatus.FAILED);
    assertThat(entity.getFailedRefresh().getErrorCode()).isEqualTo(expectedErrorCode);
    assertThat(entity.getFailedRefresh().getErrorMessage()).isEqualTo(failureReason.getMessage());
    assertThat(entity.getFailedRefresh().getRefreshEndDate()).isNotNull();
  }

  @Test
  void testRefreshFailedWithErrorCode() {
    ListEntity entity = TestDataFixture.getListEntityWithInProgressRefresh();
    String expectedErrorCode = "refresh-max.list.size.exceeded";
    AbstractListException exception = new MaxListSizeExceededException(entity, 1000);

    entity.refreshFailed(exception, new TaskTimer());

    assertThat(entity.getInProgressRefresh()).isNull();
    assertThat(entity.getFailedRefresh().getStatus()).isEqualTo(AsyncProcessStatus.FAILED);
    assertThat(entity.getFailedRefresh().getErrorCode()).isEqualTo(expectedErrorCode);
    assertThat(entity.getFailedRefresh().getErrorMessage()).isEqualTo(exception.getMessage());
    assertThat(entity.getFailedRefresh().getRefreshEndDate()).isNotNull();
  }

  @Test
  void isFailedRefreshedShouldReturnTrue() {
    ListEntity refreshedList = TestDataFixture.getListEntityFailedRefresh();
    assertTrue(refreshedList.refreshFailed());
  }

  @Test
  void isRefreshingShouldReturnTrue() {
    ListEntity refreshingList = TestDataFixture.getListEntityWithInProgressRefresh();
    assertThat(refreshingList.isRefreshing()).isTrue();
  }

  @Test
  void isRefreshingShouldReturnFalse() {
    ListEntity nonRefreshingList = new ListEntity();
    assertThat(nonRefreshingList.isRefreshing()).isFalse();
    nonRefreshingList = TestDataFixture.getListEntityWithSuccessRefresh();
    assertThat(nonRefreshingList.isRefreshing()).isFalse();
    nonRefreshingList = TestDataFixture.getListEntityFailedRefresh();
    assertThat(nonRefreshingList.isRefreshing()).isFalse();
  }

  @Test
  void isRefreshedShouldReturnTrue() {
    ListEntity refreshedList = TestDataFixture.getListEntityWithSuccessRefresh();
    assertThat(refreshedList.isRefreshed()).isTrue();
  }

  @Test
  void isRefreshedShouldReturnFalse() {
    ListEntity nonRefreshedList = new ListEntity();
    assertThat(nonRefreshedList.isRefreshed()).isFalse();
    nonRefreshedList = TestDataFixture.getListEntityWithInProgressRefresh();
    assertThat(nonRefreshedList.isRefreshed()).isFalse();
    nonRefreshedList = TestDataFixture.getListEntityFailedRefresh();
    assertThat(nonRefreshedList.isRefreshed()).isFalse();
  }

  @Test
  void testGetRecordsCount() {
    ListEntity refreshedList = TestDataFixture.getListEntityWithSuccessRefresh();
    assertThat(refreshedList.getRecordsCount()).isEqualTo(2);
    ListEntity nonRefreshedList = TestDataFixture.getListEntityFailedRefresh();
    assertThat(nonRefreshedList.getRecordsCount()).isZero();
  }

  @Test
  void testGetInProgressRefreshId() {
    ListEntity refreshingList = TestDataFixture.getListEntityWithInProgressRefresh();
    assertThat(refreshingList.getInProgressRefreshId()).isNotEmpty();
    ListEntity refreshedList = TestDataFixture.getListEntityWithSuccessRefresh();
    assertThat(refreshedList.getInProgressRefreshId()).isEmpty();
    ListEntity failedRefreshList = TestDataFixture.getListEntityFailedRefresh();
    assertThat(failedRefreshList.getInProgressRefreshId()).isEmpty();
  }

  @Test
  void shouldCancelRefresh() {
    UUID userId = UUID.randomUUID();
    ListEntity refreshingList = TestDataFixture.getListEntityWithInProgressRefresh();
    ListRefreshDetails refreshDetails = refreshingList.getInProgressRefresh();
    refreshingList.refreshCancelled(userId);
    assertEquals(AsyncProcessStatus.CANCELLED, refreshDetails.getStatus());
    assertNull(refreshingList.getInProgressRefresh());
  }

  @Test
  void shouldUpdateList() {
    UsersClient.User user = new UsersClient.User(UUID.randomUUID(), Optional.of(new UsersClient.Personal("Test", "User")));
    ListEntity list = TestDataFixture.getInactiveListEntity();
    ListUpdateRequestDTO listUpdateRequestDTO = TestDataFixture.getListUpdateRequestDTO();
    list.update(listUpdateRequestDTO, user, "item_status = 'missing'");
    assertEquals(list.getName(), listUpdateRequestDTO.getName());
    assertEquals(list.getDescription(), listUpdateRequestDTO.getDescription());
    assertEquals(list.getFqlQuery(), listUpdateRequestDTO.getFqlQuery());
    assertEquals(list.getFields(), listUpdateRequestDTO.getFields());
    assertEquals(list.getIsActive(), listUpdateRequestDTO.getIsActive());
    assertEquals(list.getIsPrivate(), listUpdateRequestDTO.getIsPrivate());
    assertEquals(list.getUpdatedBy(), user.id());
  }
}
