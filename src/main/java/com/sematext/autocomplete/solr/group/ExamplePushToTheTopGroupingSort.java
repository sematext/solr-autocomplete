/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.autocomplete.solr.group;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.component.ResponseBuilder;

import com.sematext.autocomplete.solr.AcGroupResult;

public class ExamplePushToTheTopGroupingSort extends GroupingSort {
  private String valueToPushToTop;
  
  public ExamplePushToTheTopGroupingSort(SolrParams additionalComponentParams) {
    super(additionalComponentParams);
    
    valueToPushToTop = additionalComponentParams.get("X");
  }

  @Override
  public void sort(ResponseBuilder rb, List<AcGroupResult> allGroupsResults) {
    Collections.sort(allGroupsResults, new PushToTopComparator(valueToPushToTop));
  }
}

class PushToTopComparator implements Comparator<AcGroupResult> {
  private String x;
  
  public PushToTopComparator(String x) {
    this.x = x;
  }
  @Override
  public int compare(AcGroupResult o1, AcGroupResult o2) {
    if (o1.getAcGroupingFieldValue().getFieldValue().equals(o2.getAcGroupingFieldValue().getFieldValue())) {
      return 0;
    }
    
    if (o1.getAcGroupingFieldValue().getFieldValue().equalsIgnoreCase(x)) {
      return -1;
    }
    
    return 1;
  }
}