package org.folio.list.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWithIgnoringCase;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class LatestMigratedVersionRepositoryTest {

  @Mock
  EntityManager entityManager;

  @Mock
  PlatformTransactionManager platformTransactionManager;

  @InjectMocks
  LatestMigratedVersionRepository latestMigratedVersionRepository;

  @Test
  void testGetLatestVersionWhenAlreadyPresent() {
    Query query = mock(Query.class);
    when(entityManager.createNativeQuery(anyString())).thenReturn(query);
    when(query.getSingleResult()).thenReturn("current");

    assertThat(latestMigratedVersionRepository.getLatestMigratedVersion(), is("current"));

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(entityManager, times(1)).createNativeQuery(captor.capture());
    assertThat(captor.getValue(), startsWithIgnoringCase("select"));
    verifyNoMoreInteractions(entityManager);
  }

  @Test
  void testGetLatestVersionWhenDatabaseEmpty() {
    Query query = mock(Query.class);
    when(entityManager.createNativeQuery(anyString())).thenReturn(query);
    when(query.getSingleResult()).thenThrow(new NoResultException());

    assertThat(latestMigratedVersionRepository.getLatestMigratedVersion(), is("-1"));

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(entityManager, times(2)).createNativeQuery(captor.capture());
    assertThat(captor.getAllValues(), contains(startsWithIgnoringCase("select"), startsWithIgnoringCase("insert")));
    verifyNoMoreInteractions(entityManager);
  }

  @Test
  void testUpdateVersionWhenAlreadyCurrent() {
    Query query = mock(Query.class);
    when(entityManager.createNativeQuery(anyString())).thenReturn(query);
    when(query.getSingleResult()).thenReturn("current");

    latestMigratedVersionRepository.setLatestMigratedVersion("current");

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(entityManager, times(1)).createNativeQuery(captor.capture());
    assertThat(captor.getValue(), startsWithIgnoringCase("select"));
    verifyNoMoreInteractions(entityManager);
  }

  @Test
  void testUpdateVersionWhenUpdateNeededAndDatabaseHasCurrentVersion() {
    Query query = mock(Query.class);
    when(entityManager.createNativeQuery(anyString())).thenReturn(query);
    when(query.setParameter(anyString(), eq("new"))).thenReturn(query);
    when(query.getSingleResult()).thenReturn("old");

    latestMigratedVersionRepository.setLatestMigratedVersion("new");

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(entityManager, times(2)).createNativeQuery(captor.capture());
    assertThat(captor.getAllValues(), contains(startsWithIgnoringCase("select"), startsWithIgnoringCase("update")));
    verifyNoMoreInteractions(entityManager);
  }

  @Test
  void testUpdateVersionWhenUpdateNeededAndDatabaseIsEmpty() {
    Query query = mock(Query.class);
    when(entityManager.createNativeQuery(anyString())).thenReturn(query);
    when(query.setParameter(anyString(), eq("new"))).thenReturn(query);
    when(query.getSingleResult()).thenThrow(new NoResultException());

    latestMigratedVersionRepository.setLatestMigratedVersion("new");

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(entityManager, times(3)).createNativeQuery(captor.capture());
    assertThat(
      captor.getAllValues(),
      contains(startsWithIgnoringCase("select"), startsWithIgnoringCase("insert"), startsWithIgnoringCase("update"))
    );
    verifyNoMoreInteractions(entityManager);
  }
}
