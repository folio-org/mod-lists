package org.folio.list.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.folio.list.domain.ListEntity;
import org.folio.list.repository.ListRepository;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Log4j2
@Primary
@Service
public class CustomTenantService extends TenantService {

  protected final PrepareSystemUserService prepareSystemUserService;
  private final ListRepository listRepository;
  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  public CustomTenantService(
    JdbcTemplate jdbcTemplate,
    FolioExecutionContext context,
    FolioSpringLiquibase folioSpringLiquibase,
    PrepareSystemUserService prepareSystemUserService,
    ListRepository listRepository
  ) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.prepareSystemUserService = prepareSystemUserService;
    this.listRepository = listRepository;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    log.info("Initializing system user");
    prepareSystemUserService.setupSystemUser();
    Collection<ListEntity> lists = (Collection<ListEntity>) listRepository.findAll();
    for (ListEntity list : lists) {
      System.out.println(list.getEntityTypeId());
      if (list.getEntityTypeId().equals(UUID.fromString("0069cf6f-2833-46db-8a51-8934769b8289"))) {
        list.setEntityTypeId(UUID.fromString("ddc93926-d15a-4a45-9d9c-93eadc3d9bbf"));
        try {
          updateFqlQueryForUsers(list);
          updateFieldNamesForUsers(list);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
      listRepository.save(list);
    }
  }

  public void updateFqlQueryForUsers(ListEntity list) throws Exception {
    Map<String, String> keyMappings = getStringStringMap();
    String currentQuery = list.getFqlQuery();
    Map<String, Object> map = objectMapper.readValue(currentQuery, new TypeReference<HashMap<String, Object>>() {});

    Map<String, Object> result = new HashMap<>();

    for (Map.Entry<String, Object> entry : map.entrySet()) {
      String key = entry.getKey();
      System.out.println("print key" +key);
      if (keyMappings.containsKey(key)) {
        String newKey = keyMappings.get(key);
        result.put(newKey, entry.getValue());
      } else {
        result.put(key, entry.getValue());
      }
    }
    String resultStr = objectMapper.writeValueAsString(result);
    System.out.print("new migrated query" +resultStr);
    list.setFqlQuery(resultStr);
  }


  public void updateFieldNamesForUsers(ListEntity list){
    Map<String, String> keyMappings = getStringStringMap();
    List<String> listFields = list.getFields();
    List<String> updatedFields = new ArrayList<>();
    for (String field : listFields) {
      // If no mapping is found, retain the original field name
      updatedFields.add(keyMappings.getOrDefault(field, field));
    }
    list.setFields(updatedFields); // Update the fields in ListEntity with the translated names

  }

  @NotNull
  private static Map<String, String> getStringStringMap() {
    Map<String, String> keyMappings = new HashMap<>();
    keyMappings.put("user_active", "users.active");
    keyMappings.put("user_barcode", "users.barcode");
    keyMappings.put("username", "users.username");
    keyMappings.put("user_updated_date", "users.updated_date");
    keyMappings.put("user_preferred_first_name", "users.preferred_first_name");
    keyMappings.put("user_middle_name", "users.middle_name");
    keyMappings.put("user_patron_group_id", "users.patron_group_id");
    keyMappings.put("user_patron_group", "users.patron_group");
    keyMappings.put("user_last_name", "users.last_name");
    keyMappings.put("id", "users.id");
    keyMappings.put("user_first_name", "users.first_name");
    keyMappings.put("user_external_system_id", "users.external_system_id");
    keyMappings.put("user_expiration_date", "users.enrollment_date");
    keyMappings.put("user_enrollment_date", "users.enrollment_date");
    keyMappings.put("user_email", "users.email");
    keyMappings.put("user_created_date", "users.created_date");
    keyMappings.put("user_department_ids", "users.department_ids");
    keyMappings.put("user_department_names", "users.departments");
    keyMappings.put("user_mobile_phone", "users.mobile_phone");
    keyMappings.put("user_phone", "users.phone");
    keyMappings.put("users_departments", "users.departments");
    keyMappings.put("users_addresses", "users.addresses");
    keyMappings.put("user_address_line1", "address_line1");
    keyMappings.put("user_address_line2", "address_line2");
    keyMappings.put("user_cities", "city");
    keyMappings.put("user_country_ids", "country_id");
    keyMappings.put("user_postal_codes", "postal_code");
    keyMappings.put("user_primary_address", "primary_address");
    keyMappings.put("user_regions", "region");
    keyMappings.put("user_address_ids", "address_id");
    ///////////////////////////////
    //keyMappings.put("user_address_ids", "address_id");
    //keyMappings.put("user_address_type_names", "users.updated_date");

    return keyMappings;
  }
}
