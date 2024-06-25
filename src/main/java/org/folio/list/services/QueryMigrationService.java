package org.folio.list.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.list.domain.ListEntity;
import org.folio.list.repository.ListRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Stream;

@Log4j2
@Service
@RequiredArgsConstructor
public class QueryMigrationService {

  @Autowired
  private ObjectMapper objectMapper;

  private static ResourcePatternResolver resourceResolver;

  private final ListRepository listRepository;

  public void migratingQueries() {
    Iterable<ListEntity> lists = listRepository.findAll();
    List<ListEntity> listsToUpdate = new ArrayList<>();
    for (ListEntity list : lists) {
      if (list.getEntityTypeId().equals(UUID.fromString("0069cf6f-2833-46db-8a51-8934769b8289"))) {
        try {
          list.setEntityTypeId(UUID.fromString("ddc93926-d15a-4a45-9d9c-93eadc3d9bbf"));
          list.setFqlQuery(updateFqlQueryForUsers(list));
          list.setFields(updateFieldNamesForUsers(list));
        } catch (Exception e) {
          log.error("Exception:", e);
          throw new RuntimeException(e);
        }
      }
      //listRepository.save(list);
    }
    listRepository.saveAll(listsToUpdate);
  }

  public String updateFqlQueryForUsers(ListEntity list) throws Exception {
    Map<String, String> keyMappings = getStringStringMap();
    String currentQuery = list.getFqlQuery();
    if (currentQuery == null || currentQuery.isEmpty()) {
      return "";
    }
    Map<String, Object> map = objectMapper.readValue(currentQuery, new TypeReference<HashMap<String, Object>>() {
    });

    Map<String, Object> result = new HashMap<>();

    for (Map.Entry<String, Object> entry : map.entrySet()) {
      String key = entry.getKey();
      //System.out.println("print key" +key);
      if (keyMappings.containsKey(key)) {
        //System.out.println("printing new key" +keyMappings.get(key));
        String newKey = keyMappings.get(key);
        //System.out.println("printing value" +entry.getValue());
        result.put(newKey, entry.getValue());
      } else {
        result.put(key, entry.getValue());
      }
    }
    //System.out.print("new migrated query" +objectMapper.writeValueAsString(result));
    return objectMapper.writeValueAsString(result);
    //
    //list.setFqlQuery(resultStr);
  }


  public List<String> updateFieldNamesForUsers(ListEntity list) throws IOException {

    Map<String, String> keyMappings = getStringStringMap();
    List<String> listFields = list.getFields();
    if (listFields == null || listFields.isEmpty()) {
      return Collections.emptyList();
    }
    List<String> updatedFields = new ArrayList<>();
    for (String field : listFields) {
      // If no mapping is found, retain the original field name
      updatedFields.add(keyMappings.getOrDefault(field, field));
    }
    //list.setFields(updatedFields); // Update the fields in ListEntity with the translated names
    return updatedFields;
  }

  @NotNull
  private static Map<String, String> getStringStringMap() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, String> keyMappings = new HashMap<>();
    ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
    List<Map<String, String>> mapList = Stream
      .of(resourceResolver.getResources("classpath:/mappings/keymappings.json5"))
      .filter(Resource::isReadable)
      .map(resource -> {
        try {
          return objectMapper.readValue(resource.getInputStream(), new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
          log.error("Unable to read values from resource: {}", resource.getDescription(), e);
          throw new UncheckedIOException(e);
        }
      })
      .toList();

    for (Map<String, String> map : mapList) {
      keyMappings.putAll(map);
    }

    return keyMappings;
  }
}
