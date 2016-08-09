/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.autocomplete.solr.group;

import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;

import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

import com.sematext.autocomplete.solr.AcGroupResult;
import com.sematext.querysegmenter.GenericSegmentDictionaryMemImpl;
import com.sematext.querysegmenter.QuerySegmenter;
import com.sematext.querysegmenter.QuerySegmenterDefaultImpl;
import com.sematext.querysegmenter.TypedSegment;

public class DictionaryGroupingSort extends GroupingSort {

  private QuerySegmenter segmenter = new QuerySegmenterDefaultImpl();

  public DictionaryGroupingSort(SolrParams additionalComponentParams) {
    super(additionalComponentParams);
    NamedList<Object> all = additionalComponentParams.toNamedList();
    for (Entry<String, Object> entry : all) {
      if (entry.getKey().equals("class")) {
        continue;
      }
      // The dictionary name is the group name
      segmenter.addFileDictionary(entry.getKey(), entry.getValue().toString(), GenericSegmentDictionaryMemImpl.class);
    }
  }

  @Override
  public void sort(ResponseBuilder rb, List<AcGroupResult> allGroupsResults) {

    // Look if some words of the query is in a dictionary
    String q = rb.req.getParams().get(CommonParams.Q);
    List<TypedSegment> segments = segmenter.segment(q);

    // No words from the query in any dictionary. Just keep the original ordering of groups.
    if (segments.isEmpty()) {
      return;
    }

    // TODO for now, we only elevate the first group that matches, but we might need to do the same for the others.
    TypedSegment typedSegment = segments.get(0);

    // The dictionary name is the group name
    String groupNameToElevate = typedSegment.getDictionaryName();

    // Check if a group with the same name is there. If it's the case, remove it from list (temporarily).
    AcGroupResult groupToElavate = removeMatchingGroup(allGroupsResults, groupNameToElevate);

    // Put back group at the top of the list
    if (groupToElavate != null) {
      allGroupsResults.add(0, groupToElavate);
    }
  }

  /**
   * Retrieve the group from the list that matches the group name to elevate.
   * 
   * @param groups
   *          all groups
   * @param groupNameToElevate
   *          group name to elevate
   * @return matching group
   */
  private AcGroupResult removeMatchingGroup(List<AcGroupResult> groups, String groupNameToElevate) {
    AcGroupResult groupToElavate = null;
    ListIterator<AcGroupResult> listIterator = groups.listIterator();
    while (listIterator.hasNext()) {
      AcGroupResult group = listIterator.next();
      String groupName = group.getAcGroupingFieldValue().getFieldValue();
      if (groupName.equals(groupNameToElevate)) {
        listIterator.remove();
        groupToElavate = group;
        break;
      }
    }
    return groupToElavate;
  }

}
