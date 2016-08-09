/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.autocomplete.solr.group;

import java.util.List;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.component.ResponseBuilder;

import com.sematext.autocomplete.solr.AcGroupResult;

public abstract class GroupingSort {
  private SolrParams additionalComponentParams;
  
  public GroupingSort(SolrParams additionalComponentParams) {
    this.additionalComponentParams = additionalComponentParams;
  }

  /**
   * Used to define the new order in which AC result groups will be returned in the result set.
   * 
   * @param allGroupsResults
   */
  public abstract void sort(ResponseBuilder rb, List<AcGroupResult> allGroupsResults);

  public SolrParams getAdditionalComponentParams() {
    return additionalComponentParams;
  }

  public void setAdditionalComponentParams(SolrParams additionalComponentParams) {
    this.additionalComponentParams = additionalComponentParams;
  }
}
