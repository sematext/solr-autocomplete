/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.autocomplete.solr.group;

import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.search.DocList;
import org.apache.solr.search.DocListAndSet;

public class ExampleResalePriceGroupingHandler extends GroupingHandler {
  private String resalePrice;

  public ExampleResalePriceGroupingHandler(String fieldName, String groupName, SolrParams additionalComponentParams) {
    super(fieldName, groupName, additionalComponentParams);
    resalePrice = getAdditionalComponentParams().get("resalePrice");
  }

  @Override
  public DocListAndSet postProcessResult(ResponseBuilder rb, DocList originalResult) {
    // no post processing needed
    return null;
  }
  
  @Override
  public DocListAndSet postProcessDistributedResult(ResponseBuilder rb, SolrDocumentList originalResult) {
 // no post processing needed
    return null;
  }

  @Override
  public void prepareGroupQueryParams(ModifiableSolrParams params) {
    // filtering out anything with price greater than config param "price"
    params.add("fq", "resalePrice:[* TO " + resalePrice + "]");
  }
}
