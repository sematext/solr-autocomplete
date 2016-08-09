/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.autocomplete.solr;

/**
 * Represents data for one value of grouping field.
 *
 * @author sematext, http://www.sematext.com/
 */
public class AcGroupingFieldValue {
  private String fieldValue;
  private int requestedCountOfSuggestions;
  
  public String getFieldValue() {
    return fieldValue;
  }
  public void setFieldValue(String fieldValue) {
    this.fieldValue = fieldValue;
  }
  public int getRequestedCountOfSuggestions() {
    return requestedCountOfSuggestions;
  }
  public void setRequestedCountOfSuggestions(int requestedCountOfSuggestions) {
    this.requestedCountOfSuggestions = requestedCountOfSuggestions;
  }
}