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
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.params.SpatialParams;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.search.DocList;
import org.apache.solr.search.DocListAndSet;

public class LocationGroupingHandler extends GroupingHandler {

  private final String fq;
  private final String distance;
  private final String sfield;

  public LocationGroupingHandler(String fieldName, String groupName, SolrParams additionalComponentParams) {
    super(fieldName, groupName, additionalComponentParams);
    fq = getAdditionalComponentParams().get(CommonParams.FQ);
    distance = getAdditionalComponentParams().get(SpatialParams.DISTANCE);
    sfield = getAdditionalComponentParams().get(SpatialParams.FIELD);
  }

  @Override
  public void prepareGroupQueryParams(ModifiableSolrParams params) {

    String pt = params.get(SpatialParams.POINT);
    if (pt == null) {
      return;
    }

    params.add(SpatialParams.POINT, pt);
    params.add(CommonParams.FQ, fq);
    params.add(SpatialParams.FIELD, sfield);

    // Use distance from config if not available in the request
    String d = params.get(SpatialParams.DISTANCE);
    if (d == null) {
      params.add(SpatialParams.DISTANCE, distance);
    }
  }

  @Override
  public DocListAndSet postProcessResult(ResponseBuilder rb, DocList originalResult) {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public DocListAndSet postProcessDistributedResult(ResponseBuilder rb, SolrDocumentList originalResult) {
    // TODO Auto-generated method stub
    return null;
  }

}
