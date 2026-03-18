package org.folio.list.util;

import java.time.OffsetDateTime;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class DateMatcher extends BaseMatcher<OffsetDateTime> {

  private final OffsetDateTime firstDate;
  private Throwable error;

  public DateMatcher(OffsetDateTime date) {
    this.firstDate = date;
  }

  @Override
  public boolean matches(Object o) {
    try {
      OffsetDateTime secondDate = OffsetDateTime.parse((String) o);
      return secondDate.equals(firstDate);
    } catch (Exception e) {
      this.error = e;
      return false;
    }
  }

  @Override
  public void describeTo(Description description) {
    if (error != null) {
      description.appendText(error.getMessage());
    }
  }
}
