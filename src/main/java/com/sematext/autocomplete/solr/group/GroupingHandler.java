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

/**
 * Adds ability to define custom parameters and custom response processing for each "AC group" query.
 *
 * @author sematext, http://www.sematext.com/
 */
public abstract class GroupingHandler {
  private String fieldName;
  private String groupName;
  private SolrParams additionalComponentParams;
  
  public GroupingHandler(String fieldName, String groupName, SolrParams additionalComponentParams) {
    this.fieldName = fieldName;
    this.groupName = groupName;
    this.additionalComponentParams = additionalComponentParams;
  }
  
  /**
   * This method should populate parameters Maps and List which will be applied to solr query params before executing a query 
   * for particular AC group. Keys in the maps should be solr parameter name, while values
   * in the List for each parameter name should contain all values which will be applied for this parameter.
   * 
   * @param requestParams parameters of original request. They should not be modified in this method!!
   * 
   * 
   * @return .
   */
  public abstract void prepareGroupQueryParams(ModifiableSolrParams requestParams);
  
  /**
   * Can be used to adjust resulting set after the query for particular group was executed. If no changes are done,
   * the method should return null.
   * 
   * @param rb .
   * @param originalResult .
   * @return new DocSlice result or null if no changes
   */
  public abstract DocListAndSet postProcessResult(ResponseBuilder rb, DocList originalResult);
  
  public abstract DocListAndSet postProcessDistributedResult(ResponseBuilder rb, SolrDocumentList originalResult);
  
  /**
   * Checks if particular GroupHandler should be used to additionally pre-process request or post-process result set
   * of some group. Group is defined by : 
   *   - fieldName by which group is created
   *   - groupName - the value in the fieldName field
   *   
   * For instance, consider this grouping definition:
   *   <str name=”type”>book:5 dvd:3 cd:2</str>
   *   
   * This method would be invoked 3 times (since there are 3 different group queries):
   *   - fieldName = "type", groupName = "book"
   *   - fieldName = "type", groupName = "dvd"
   *   - fieldName = "type", groupName = "cd"
   *   
   * If some GroupHandler is supposed to pre-process/post-process only the query of one particular group (say, type=book),
   * this method will return true only when grouping fieldName is "type" and value by which the group is formed is "book".
   * 
   * @param groupName .
   * @return .
   */
  public final boolean accepts(String fieldName, String groupName) {
    if (this.fieldName.equalsIgnoreCase(fieldName) && this.groupName.equals(groupName)) {
      return true;
    }
    
    return false;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public SolrParams getAdditionalComponentParams() {
    return additionalComponentParams;
  }

  public void setAdditionalComponentParams(SolrParams additionalComponentParams) {
    this.additionalComponentParams = additionalComponentParams;
  }
}
