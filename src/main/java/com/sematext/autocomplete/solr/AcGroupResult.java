/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.autocomplete.solr;

import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.search.DocList;

public class AcGroupResult {
  private AcGroupingFieldValue acGroupingFieldValue;
  private DocList resultingDocs;
  private SolrDocumentList distributedResultingDocs;
  
  public DocList getResultingDocs() {
    return resultingDocs;
  }
  public void setResultingDocs(DocList resultingDocs) {
    this.resultingDocs = resultingDocs;
  }
  public SolrDocumentList getDistributedResultingDocs() {
    return distributedResultingDocs;
  }
  public void setDistributedResultingDocs(SolrDocumentList distributedResultingDocs) {
    this.distributedResultingDocs = distributedResultingDocs;
  }
  public AcGroupingFieldValue getAcGroupingFieldValue() {
    return acGroupingFieldValue;
  }
  public void setAcGroupingFieldValue(AcGroupingFieldValue acGroupingFieldValue) {
    this.acGroupingFieldValue = acGroupingFieldValue;
  }
}
