/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.autocomplete.solr;

import org.apache.lucene.search.Query;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.ShardParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocListAndSet;
import org.apache.solr.search.DocSlice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sematext.autocomplete.solr.group.GroupingHandler;
import com.sematext.autocomplete.solr.group.GroupingSort;
import com.sematext.solr.handler.component.AbstractReSearcherComponent;
import com.sematext.solr.handler.component.ReSearcherRequestContext;

/**
 * AutoCompleteSearchComponent handles grouping logic of “custom data”. Clients will have to provide 
 * field name (ac_grouping_field) on which grouping will be made and parameter which describes which values 
 * from that group are interesting, how many suggestions for each value is wanted and order in which values 
 * are sorted in resulting list. Such parameter is not mandatory, since it can be defined in configuration 
 * for each possible ac_grouping_field. For instance, configuration could look like this:

  &lt;lst name=”ac_grouping_fields_definition”&gt;
    &lt;str name=”type”&gt;book:5 dvd:3 cd:2&lt;/str&gt;
    &lt;str name=”is_sposored”&gt;false:9 true:1&lt;/str&gt;
  &lt;/lst&gt;

 * When sending AC query, client can send parameters like:

   ac_grouping_field=type
   ac_grouping_field_description=book:5 dvd:3 cd:2
   
 * Old functionality of filtering results by fq can be achieved either by not using ACSC (for instance, without
 * sending ac_grouping_field parameter) or by using ACSC where fq isn’t done on the same field which is 
 * ac_grouping_field of that query.
 * 
 * In order for this component to work correctly, values for fields in AC index that can be used as 
 * ac_grouping_field will have to be filled for all documents, empty values wouldn’t be allowed 
 * (in fact, they would, but clients would miss some suggestions they would otherwise get).
 *
 * @author sematext, http://www.sematext.com/
 */
public final class AutoCompleteSearchComponent extends AbstractReSearcherComponent {
  private static final Logger LOG = LoggerFactory.getLogger(AutoCompleteSearchComponent.class);
  private static final Pattern PATTERN = Pattern.compile(":\\d+( |$)");
  
  public static final String COMPONENT_NAME = "autoComplete";
  public static final String AC_GROUPING_FIELD_PARAM_NAME = "ac_grouping_field";
  public static final String AC_GROUPING_FIELD_DEFINITION_PARAM_NAME = "ac_grouping_field_def";
  public static final String AC_GROUPING_SORT_PARAM_NAME = "ac_grouping_sort";
  
  public static final String AC_SPELLCHECKING_PARAM_NAME = "ac_spellcheck";
  public static final String AC_MATCH_FULL_WORDS_PARAM_NAME = "ac_matchFullWords";
  public static final String AC_MATCH_CORRECT_WORD_ORDERING_PARAM_NAME = "ac_matchWordOrder";
  public static final String AC_PURE_COMPLETION_PARAM_NAME = "ac_pureCompletion";
  
  private static final String AC_SCHEMA_FIELD_NAME_LEGACY = "prefixTok";
  private static final String AC_SCHEMA_FIELD_NAME_PURE_COMPLETION = "pureCompletion";
  private static final String AC_SCHEMA_FIELD_NAME_PHRASE_SPELLCHECKING = "phraseSpellchecking";
  private static final String AC_SCHEMA_FIELD_NAME_MATCH_FULL_WORDS = "matchFullWords";
  private static final String AC_SCHEMA_FIELD_NAME_MATCH_WORD_ORDER = "matchWordOrder";
  
  //private static final String AC_SEARCH_HANDLER = "searchHandler";
  
  private SolrParams groupingFieldDefinitions;
  private List<GroupingHandler> groupingHandlers = new ArrayList<GroupingHandler>();
  private Map<String, GroupingSort> groupSorts = new HashMap<String, GroupingSort>();
  private String searchHandler;
  
  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void init(NamedList args) {
    super.init(args);
    
    Object grDef = args.get("acGroupingFieldDefinitions");
    Object grHandlers = args.get("acGroupingHandlerDefinitions");
    Object grSorts = args.get("acGroupingSortDefinitions");
    Object grSearchHandler = args.get("searchHandler");
    if (grSearchHandler instanceof String) {
      this.searchHandler = (String)grSearchHandler;
    }

    if (grDef != null && grDef instanceof NamedList) {
      groupingFieldDefinitions = SolrParams.toSolrParams((NamedList) grDef);
    }
    else {
      groupingFieldDefinitions = new MapSolrParams(new HashMap<String, String>());
    }
    
    if (grHandlers != null && grHandlers instanceof NamedList) {
      NamedList grHandlersNamedList = (NamedList) grHandlers;
      for (int i = 0; i < grHandlersNamedList.size(); i++) {
        String name = grHandlersNamedList.getName(i);
        String [] parsedName = name.split(":");
        
        NamedList values = (NamedList) grHandlersNamedList.getVal(i);

        String className = (String) values.get("class");
        
        try {
          Class handlerClass = this.getClass().getClassLoader().loadClass(className);
          Class [] proto = new Class[3];
          proto[0] = String.class;
          proto[1] = String.class;
          proto[2] = SolrParams.class;
          Object [] params = new Object[3];
          params[0] = parsedName[0];
          params[1] = parsedName[1];
          params[2] = SolrParams.toSolrParams(values);

          Constructor<GroupingHandler> ct = handlerClass.getConstructor(proto);
          groupingHandlers.add(ct.newInstance(params));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
    
    if (grSorts != null && grSorts instanceof NamedList) {
      NamedList grSortsNamedList = (NamedList) grSorts;
      for (int i = 0; i < grSortsNamedList.size(); i++) {
        String name = grSortsNamedList.getName(i);
        
        NamedList values = (NamedList) grSortsNamedList.getVal(i);

        String className = (String) values.get("class");
        
        try {
          Class handlerClass = this.getClass().getClassLoader().loadClass(className);
          Class [] proto = new Class[1];
          proto[0] = SolrParams.class;
          Object [] params = new Object[1];
          params[0] = SolrParams.toSolrParams(values);

          Constructor<GroupingSort> ct = handlerClass.getConstructor(proto);
          groupSorts.put(name, ct.newInstance(params));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }    
    }
  }
  
  @Override
  public int distributedProcess(ResponseBuilder rb) throws IOException {
    if (searchHandler != null && checkComponentShouldProcess(rb)) {
      doProcess(rb);
    }
    
    return ResponseBuilder.STAGE_DONE;
  }
  
  @Override
  public void finishStage(ResponseBuilder rb) {
    if (searchHandler == null) {
      super.finishStage(rb);
    }
  }
  
  @Override
  protected boolean checkComponentShouldProcess(ResponseBuilder rb) {
    SolrParams params = rb.req.getParams();
    return params.getBool(COMPONENT_NAME, true);
  }
  
  public void doProcess(ResponseBuilder rb) {
    ReSearcherRequestContext ctx = new ReSearcherRequestContext(rb);
    ctx.setCore(getCore());
    ctx.setQueryOnlyComponents(getQueryOnlyComponents());
    ctx.setShardHandlerFactory(getShardHandlerFactory());
    
    try {
      doProcess(ctx, rb);
    } catch (Exception e) {
      String msg = "ReSearcher error";
      LOG.error(msg, e);
      throw new RuntimeException(msg, e);
    }

    ctx.restoreContext(rb);
  }
  
  @Override
  protected void doProcess(ReSearcherRequestContext ctx, ResponseBuilder rb) throws Exception {
    String groupingFieldName = rb.req.getParams().get(AC_GROUPING_FIELD_PARAM_NAME);
    
    if (groupingFieldName == null) {
      boolean spellcheck = rb.req.getParams().getBool(AC_SPELLCHECKING_PARAM_NAME, false);
      boolean matchFullWords = rb.req.getParams().getBool(AC_MATCH_FULL_WORDS_PARAM_NAME, false);
      boolean matchCorrectWordOrdering = rb.req.getParams().getBool(AC_MATCH_CORRECT_WORD_ORDERING_PARAM_NAME, false);
      boolean pureCompletion = rb.req.getParams().getBool(AC_PURE_COMPLETION_PARAM_NAME, false);

      // TODO perform the check to allow spellchecking, matching of full words and correct word ordering only with dismax
      // handler
      
      if (pureCompletion && spellcheck) {
        throw new IllegalArgumentException("ac_pureCompletion and ac_spellcheck parameters can't be true at the same time!");
      }
      
      // correctly prepare parameters
      // construct qf
      String qf;
      String bq = null;
      
      // main search field first
      if (pureCompletion) {
        qf = AC_SCHEMA_FIELD_NAME_PURE_COMPLETION;
      } else if (spellcheck) {
        qf = AC_SCHEMA_FIELD_NAME_PHRASE_SPELLCHECKING;
      } else {
        // default, legacy field
        qf = AC_SCHEMA_FIELD_NAME_LEGACY;
      }
      
      // now boosts
      if (matchFullWords) {
        // TODO move boost to configuration?
        qf = qf + " " + AC_SCHEMA_FIELD_NAME_MATCH_FULL_WORDS + "^10";
      }
      
      if (matchCorrectWordOrdering) {
        // since matchWordOrder field is based on shingles, it has to be treated differently (dismax parser would not
        // parse it correctly) - by using bq parameter
        // TODO move boost to configuration?
        bq = "(" + AC_SCHEMA_FIELD_NAME_MATCH_WORD_ORDER + ":\"" + rb.req.getParams().get(CommonParams.Q) + "\")^10";
      }
      
      ModifiableSolrParams params = ctx.getParams();
      params.set(DisMaxParams.QF, qf);
      if (bq != null) {
        params.add(DisMaxParams.BQ, bq);
      }
      if (searchHandler != null) {
        params.set(CommonParams.QT, searchHandler);
        params.set(ShardParams.SHARDS_QT, searchHandler);
      }
      
      ctx.getHandler().handleSuggestionResponseRequest(ctx, params, getComponentName(), rb);
    } else {
      // perform grouping logic instead of sending simple request
      String groupingFieldDef = rb.req.getParams().get(AC_GROUPING_FIELD_DEFINITION_PARAM_NAME, 
          groupingFieldDefinitions.get(groupingFieldName));
      
      // if client didn't provide grouping field definition and there is nothing configured in solrconfig.xml for
      // that field, we have to throw exception
      if (groupingFieldDef == null) {
        throw new IllegalArgumentException("There is no definition for grouping field " + groupingFieldName +
            " in query or in solrconfig.xml, the query parameters were : " + rb.req.getParamString());
      }

      List<AcGroupingFieldValue> groupingValues = parseGroupingFieldDef(groupingFieldDef);
      
      List<AcGroupResult> resultsOfAllQueries = new ArrayList<AcGroupResult>();
      
      // for each value from definition of grouping field perform another query, take just N results and add them to
      // resulting list
      boolean isDistributed = false;
      for (AcGroupingFieldValue value:groupingValues) {
        if (value.getRequestedCountOfSuggestions() == 0) {
          // no need to execute such query
          continue;
        }
        
        ModifiableSolrParams params = new ModifiableSolrParams(ctx.getParams());
        params.add(CommonParams.FQ, groupingFieldName + ":" + value.getFieldValue());
        params.set("rows", value.getRequestedCountOfSuggestions());
        if (searchHandler != null) {
          params.set(CommonParams.QT, searchHandler);
          params.set(ShardParams.SHARDS_QT, searchHandler);
        }
        // apply GroupingHandlers here
        for (GroupingHandler gh : groupingHandlers) {
          if (gh.accepts(groupingFieldName, value.getFieldValue())) {
            gh.prepareGroupQueryParams(params);
          }
        }
        
        // execute next query
        SolrQueryResponse rsp = ctx.getHandler().handleSuggestionResponseRequest(ctx, params, getComponentName(), ctx.getQueryOnlyComponents());
        
        Object response = rsp.getValues().remove("response");
        // local search
        if (response instanceof ResultContext) {
          // now remove results of this query from response
          ResultContext queryResults = (ResultContext) response;
          
          if (queryResults != null) {
            // apply GroupingHandlers postprocessing here
            for (GroupingHandler gh : groupingHandlers) {
              if (gh.accepts(groupingFieldName, value.getFieldValue())) {
                gh.postProcessResult(rb, queryResults.getDocList());
              }
            }
  
            // and add results to the list where we collect groups results
            AcGroupResult groupResult = new AcGroupResult();
            groupResult.setResultingDocs(queryResults.getDocList());
            groupResult.setAcGroupingFieldValue(value);
            
            resultsOfAllQueries.add(groupResult);
          }
          
          isDistributed = false;
        } else {
          // distributed search
          SolrDocumentList docList = (SolrDocumentList) response;
          if (docList != null) {
          // apply GroupingHandlers postprocessing here
            for (GroupingHandler gh : groupingHandlers) {
              if (gh.accepts(groupingFieldName, value.getFieldValue())) {
                gh.postProcessDistributedResult(rb, docList);
              }
            }
  
            // and add results to the list where we collect groups results
            AcGroupResult groupResult = new AcGroupResult();
            groupResult.setDistributedResultingDocs(docList);
            groupResult.setAcGroupingFieldValue(value);
            
            resultsOfAllQueries.add(groupResult);
          }
          
          isDistributed = true;
        }
      }

      if (isDistributed) {
        // merge results into single list and set it into result
        mergeDistributedResultsIntSingleResponse(rb, resultsOfAllQueries, "response");
      } else {
        // merge results into single list and set it into result
        mergeResultsIntSingleResponse(rb, resultsOfAllQueries, "response");
      }
    }
  }

  private void mergeDistributedResultsIntSingleResponse(ResponseBuilder rb, List<AcGroupResult> resultsToMerge, String responseTagName) {
    int docs = 0;
    float maxScore = 0.0f;
    
    // if groups have to be displayed in some custom order, other than the order specified in 
    // ac_grouping_field_def
    String groupingSort = rb.req.getParams().get(AC_GROUPING_SORT_PARAM_NAME);
    if (groupSorts.containsKey(groupingSort)) {
      groupSorts.get(groupingSort).sort(rb, resultsToMerge);
    }
    
    SolrDocumentList docList = new SolrDocumentList();
    // first find count of documents
    for (AcGroupResult acGroupResult : resultsToMerge) {
      // if slice contains more results than requested, take requested count; if it contains less results than
      // requested, we have to take what we got, not more
      docs += ((acGroupResult.getDistributedResultingDocs().size() > acGroupResult.getAcGroupingFieldValue().getRequestedCountOfSuggestions() == true) ? 
          acGroupResult.getAcGroupingFieldValue().getRequestedCountOfSuggestions() : acGroupResult.getDistributedResultingDocs().size());
      
      if (acGroupResult.getDistributedResultingDocs().getMaxScore() > maxScore) {
        maxScore = acGroupResult.getDistributedResultingDocs().getMaxScore();
      }
      
      docList.addAll(acGroupResult.getDistributedResultingDocs());
    }
    
    docList.setStart(0);
    docList.setNumFound(docs);

    rb.rsp.add(responseTagName, docList);
  }

  /**
   * Merges results of multiple queries into single result and places it into Solr response under name given
   * by parameter responseTagName
   */
  private void mergeResultsIntSingleResponse(ResponseBuilder rb, List<AcGroupResult> resultsToMerge, String responseTagName) 
  {
    int docs = 0;
    float maxScore = 0.0f;
    
    // if groups have to be displayed in some custom order, other than the order specified in 
    // ac_grouping_field_def
    String groupingSort = rb.req.getParams().get(AC_GROUPING_SORT_PARAM_NAME);
    if (groupSorts.containsKey(groupingSort)) {
      groupSorts.get(groupingSort).sort(rb, resultsToMerge);
    }
    
    // first find count of documents
    for (AcGroupResult acGroupResult : resultsToMerge) {
      // if slice contains more results than requested, take requested count; if it contains less results than
      // requested, we have to take what we got, not more
      docs += ((acGroupResult.getResultingDocs().size() > acGroupResult.getAcGroupingFieldValue().getRequestedCountOfSuggestions() == true) ? 
          acGroupResult.getAcGroupingFieldValue().getRequestedCountOfSuggestions() : acGroupResult.getResultingDocs().size());
      
      if (acGroupResult.getResultingDocs().maxScore() > maxScore) {
        maxScore = acGroupResult.getResultingDocs().maxScore();
      }
    }
    
    int [] luceneIds = new int[docs];
    int index = 0;
    
    // now merge luceneIds
    for (AcGroupResult acGroupResult : resultsToMerge) {
      DocIterator docIter = acGroupResult.getResultingDocs().iterator();
      
      int countAddedDocFromCurrentDocSlice = 0;
      while (docIter != null && docIter.hasNext()) {
        if (countAddedDocFromCurrentDocSlice >= acGroupResult.getAcGroupingFieldValue().getRequestedCountOfSuggestions()) {
          // if query returned more results than requested, we have to break this loop, so we don't take
          // to many results for some group
          break;
        }
          
        luceneIds[index++] = docIter.nextDoc();
        countAddedDocFromCurrentDocSlice++;
      }
    }
    
    // create results
    DocListAndSet res = new DocListAndSet();
    res.docList = new DocSlice(0, docs, luceneIds, null, docs, maxScore);
    if (rb.isNeedDocSet()) {
      List<Query> queries = new ArrayList<Query>();
      queries.add(rb.getQuery());
      List<Query> filters = rb.getFilters();
      if (filters != null)
        queries.addAll(filters);
      // TODO is this really needed?
      try {
        res.docSet = rb.req.getSearcher().getDocSet(queries);
      } catch (IOException e) {
        throw new RuntimeException("Error while creating docSet in ACSC!", e);
      }
    }
    
    rb.setResults(res);
    rb.rsp.add(responseTagName, rb.getResults().docList);
  }

  private List<AcGroupingFieldValue> parseGroupingFieldDef(String groupingFieldDef) {
    List<AcGroupingFieldValue> values = new ArrayList<AcGroupingFieldValue>();
    
    Matcher m = PATTERN.matcher(groupingFieldDef);
    int count = 0;
    int prevEnd = count;
    while (m.find()) {
      int start = m.start();
      int end = m.end();
      
      AcGroupingFieldValue value = new AcGroupingFieldValue();
      value.setFieldValue(groupingFieldDef.substring(prevEnd, start));
      int indexOfSeparator = groupingFieldDef.indexOf(':', start);
      if (end == groupingFieldDef.length()) {
        value.setRequestedCountOfSuggestions(Integer.parseInt(groupingFieldDef.substring(indexOfSeparator + 1, end)));
      } else {
        value.setRequestedCountOfSuggestions(Integer.parseInt(groupingFieldDef.substring(indexOfSeparator + 1, end - 1)));
      }
      
      if (value.getRequestedCountOfSuggestions() > 0) {
        values.add(value);
      }
      
      prevEnd = end;
      count++;
    }

    return values;
  }

  @Override
  protected String getBestSuggestionResultsTagName() {
    return "ac_response";
  }

  @Override
  protected String getSuggestionsTagName() {
    return "ac_suggestions";
  }

  @Override
  protected String getComponentName() {
    return COMPONENT_NAME;
  }

  @Override
  public String getDescription() {
    return "AutoCompleteSearchComponent";
  }

}