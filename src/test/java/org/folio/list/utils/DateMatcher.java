package org.folio.list.utils;


import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateMatcher extends BaseMatcher<Date> {
  private static final DateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
  private final Date firstDate;
  private Throwable error;

  public DateMatcher(Date date) {
    this.firstDate = date;
  }

  @Override
  public boolean matches(Object o) {
    try {
      Date secondDate = FORMATTER.parse((String) o);
      return secondDate.equals(firstDate);
    } catch (Exception e) {
      this.error = e;
      return false;
    }
  }

  @Override
  public void describeTo(Description description) {
    if (error!=null) {
      description.appendText(error.getMessage());
    }
  }
}
