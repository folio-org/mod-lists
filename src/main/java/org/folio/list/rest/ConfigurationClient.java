package org.folio.list.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import java.time.DateTimeException;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

@Log4j2
@Service
@FeignClient(name = "configurations")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public abstract class ConfigurationClient {

  private final ObjectMapper objectMapper;

  @GetMapping("/entries?query=(module==ORG and configName==localeSettings)")
  public abstract String getLocaleSettings();

  public ZoneId getTenantTimezone() {
    try {
      String localeSettingsResponse = this.getLocaleSettings();
      JsonNode localeSettingsNode = objectMapper.readTree(localeSettingsResponse);
      String valueString = localeSettingsNode.path("configs").get(0).path("value").asText();
      JsonNode valueNode = objectMapper.readTree(valueString);
      return ZoneId.of(valueNode.path("timezone").asText());
    } catch (JsonProcessingException | FeignException.Unauthorized | NullPointerException | DateTimeException e) {
      log.error("Failed to retrieve timezone information from mod-configuration. Defaulting to UTC.", e);
      return ZoneId.of("UTC");
    }
  }
}
