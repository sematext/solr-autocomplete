/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.autocomplete.solr;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.CommonParams;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

public class AutoCompleteSearchComponentTest extends SolrTestCaseJ4 {
  
  @BeforeClass
  public static void beforeTests() throws Exception {
    // to run from IDE:
    // initCore("example/solr/collection1/conf/solrconfig.xml", "example/solr/collection1/conf/schema-function-query-ordering.xml", "example/solr");
    
    // to build with maven
    initCore("solrconfig.xml", "schema.xml", "solr");

    assertU(adoc("phrase", "elvis presley", "is_sponsored", "true", "type", "dvd"));
    assertU(adoc("phrase", "bob marley", "is_sponsored", "true", "type", "cd"));
    assertU(adoc("phrase", "bob dylan", "is_sponsored", "false", "type", "book", "resalePrice", "15.99"));
    assertU(adoc("phrase", "the doors", "is_sponsored", "false", "type", "dvd"));
    assertU(adoc("phrase", "bob marley & the wailers", "is_sponsored", "true", "type", "dvd"));
    assertU(adoc("phrase", "bono", "is_sponsored", "true", "type", "book"));
    assertU(adoc("phrase", "bob marley & the wailers 2", "is_sponsored", "false", "type", "dvd", "resalePrice", "5.00"));
    assertU(adoc("phrase", "bob marley & the wailers 3", "is_sponsored", "true", "type", "book", "resalePrice", "5.00"));
    assertU(adoc("phrase", "bono and bob marley 1", "is_sponsored", "false", "type", "cd", "resalePrice", "3.18"));
    assertU(adoc("phrase", "bono and bob marley 2", "is_sponsored", "true", "type", "book"));
    assertU(adoc("phrase", "bono and bob marley 3", "is_sponsored", "false", "type", "dvd", "resalePrice", "7.22"));
    assertU(adoc("phrase", "new york city", "is_sponsored", "true", "type", "book"));
    assertU(adoc("phrase", "newton newton", "is_sponsored", "false", "type", "dvd"));
    assertU(adoc("phrase", "the washington times article", "is_sponsored", "true", "type", "book"));
    assertU(adoc("phrase", "times in washington", "is_sponsored", "true", "type", "book"));
    
    assertU("commit", commit());
  }
  
  @AfterClass
  public static void afterClass() throws IOException {
    //h.getCore().getSearcher().get().close();
  }

  @Test
  public void testAcQuery() {
    assertQ(req(CommonParams.QT, "dismax_ac", 
        CommonParams.Q, "bo",
        AutoCompleteSearchComponent.AC_GROUPING_FIELD_PARAM_NAME, "is_sponsored",
        AutoCompleteSearchComponent.COMPONENT_NAME, "true")
        ,"//result[@name='response'][@numFound='4']"
        ,"//result[@name='response']/doc[1]/bool[@name='is_sponsored'][.='true']"
        ,"//result[@name='response']/doc[2]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[3]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[4]/bool[@name='is_sponsored'][.='false']"
    );
  }
  
  @Test
  public void testAcQueryReversedOrder() {
    assertQ(req(CommonParams.QT, "dismax_ac", 
        CommonParams.Q, "bo",
        AutoCompleteSearchComponent.AC_GROUPING_FIELD_PARAM_NAME, "is_sponsored",
        AutoCompleteSearchComponent.AC_GROUPING_FIELD_DEFINITION_PARAM_NAME, "false:3 true:1",
        AutoCompleteSearchComponent.COMPONENT_NAME, "true")
        ,"//result[@name='response'][@numFound='4']"
        ,"//result[@name='response']/doc[1]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[2]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[3]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[4]/bool[@name='is_sponsored'][.='true']"
    );
  }
  
  @Test
  public void testAcQueryWithNonExistantValue() {
    assertQ(req(CommonParams.QT, "dismax_ac", 
        CommonParams.Q, "bo",
        AutoCompleteSearchComponent.AC_GROUPING_FIELD_PARAM_NAME, "type",
        AutoCompleteSearchComponent.AC_GROUPING_FIELD_DEFINITION_PARAM_NAME, "dvd:3 book:1 pc:4",
        AutoCompleteSearchComponent.COMPONENT_NAME, "true")
        ,"//result[@name='response'][@numFound='4']"
        ,"//result[@name='response']/doc[1]/str[@name='type'][.='dvd']"
        ,"//result[@name='response']/doc[2]/str[@name='type'][.='dvd']"
        ,"//result[@name='response']/doc[3]/str[@name='type'][.='dvd']"
        ,"//result[@name='response']/doc[4]/str[@name='type'][.='book']"
    );
  }
  
  @Test
  public void testAcQueryWithZeroRequestedCount() {
    assertQ(req(CommonParams.QT, "dismax_ac", 
        CommonParams.Q, "bo",
        AutoCompleteSearchComponent.AC_GROUPING_FIELD_PARAM_NAME, "is_sponsored",
        AutoCompleteSearchComponent.AC_GROUPING_FIELD_DEFINITION_PARAM_NAME, "false:0 true:1",
        AutoCompleteSearchComponent.COMPONENT_NAME, "true")
        ,"//result[@name='response'][@numFound='1']"
        ,"//result[@name='response']/doc[1]/bool[@name='is_sponsored'][.='true']"
    );
  }
  
  @Test
  public void testAcQuerySomeValueExistsMultipleTimes() {
    assertQ(req(CommonParams.QT, "dismax_ac", 
        CommonParams.Q, "bo",
        AutoCompleteSearchComponent.AC_GROUPING_FIELD_PARAM_NAME, "is_sponsored",
        AutoCompleteSearchComponent.AC_GROUPING_FIELD_DEFINITION_PARAM_NAME, "false:3 true:1 false:2",
        AutoCompleteSearchComponent.COMPONENT_NAME, "true")
        ,"//result[@name='response'][@numFound='6']"
        ,"//result[@name='response']/doc[1]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[2]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[3]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[4]/bool[@name='is_sponsored'][.='true']"
        ,"//result[@name='response']/doc[5]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[6]/bool[@name='is_sponsored'][.='false']"
    );
  }
  
  @Test
  public void testAcBoostFullWords() {
    assertQ(req(CommonParams.QT, "dismax_ac", 
        CommonParams.Q, "new",
        AutoCompleteSearchComponent.COMPONENT_NAME, "true")
        ,"//result[@name='response'][@numFound='2']"
        ,"//result[@name='response']/doc[1]/str[@name='phrase'][.='newton newton']"
        ,"//result[@name='response']/doc[2]/str[@name='phrase'][.='new york city']"
    );
    
    assertQ(req(CommonParams.QT, "dismax_ac", 
        CommonParams.Q, "new",
        CommonParams.DEBUG_QUERY, "true",
        AutoCompleteSearchComponent.AC_MATCH_FULL_WORDS_PARAM_NAME, "true",
        AutoCompleteSearchComponent.COMPONENT_NAME, "true")
        ,"//result[@name='response'][@numFound='2']"
        ,"//result[@name='response']/doc[1]/str[@name='phrase'][.='new york city']"
        ,"//result[@name='response']/doc[2]/str[@name='phrase'][.='newton newton']"
    );
  }

  @Test
  public void testAcBoostCorrectWordOrdering() {
    assertQ(req(CommonParams.QT, "dismax_ac", 
        CommonParams.Q, "washington tim",
        AutoCompleteSearchComponent.COMPONENT_NAME, "true")
        ,"//result[@name='response'][@numFound='2']"
        ,"//result[@name='response']/doc[1]/str[@name='phrase'][.='the washington times article']"
        ,"//result[@name='response']/doc[2]/str[@name='phrase'][.='times in washington']"
    );
    assertQ(req(CommonParams.QT, "dismax_ac", 
        CommonParams.Q, "washington tim",
        AutoCompleteSearchComponent.AC_MATCH_CORRECT_WORD_ORDERING_PARAM_NAME, "true",
        AutoCompleteSearchComponent.COMPONENT_NAME, "true")
        ,"//result[@name='response'][@numFound='2']"
        ,"//result[@name='response']/doc[1]/str[@name='phrase'][.='the washington times article']"
        ,"//result[@name='response']/doc[2]/str[@name='phrase'][.='times in washington']"
    );
  }

  @Test
  public void testAcSpellchecking() {
    assertQ(req(CommonParams.QT, "dismax_ac", 
        CommonParams.Q, "washington tim",
        AutoCompleteSearchComponent.COMPONENT_NAME, "true")
        ,"//result[@name='response'][@numFound='2']"
        ,"//result[@name='response']/doc[1]/str[@name='phrase'][.='the washington times article']"
        ,"//result[@name='response']/doc[2]/str[@name='phrase'][.='times in washington']"
    );
  }

  @Test
  public void testAcSpellchecking2() {
    assertQ(req(CommonParams.QT, "dismax_ac", 
        CommonParams.Q, "washEngton tim",
        AutoCompleteSearchComponent.AC_SPELLCHECKING_PARAM_NAME, "true", 
        AutoCompleteSearchComponent.COMPONENT_NAME, "true")
        ,"//result[@name='response'][@numFound='2']"
        ,"//result[@name='response']/doc[1]/str[@name='phrase'][.='the washington times article']"
        ,"//result[@name='response']/doc[2]/str[@name='phrase'][.='times in washington']"
    );
  }

  @Test
  public void testAcBoostCorrectWordOrdering_noMlatches() {
    assertQ(req(CommonParams.QT, "dismax_ac", 
        CommonParams.Q, "washeng",
        AutoCompleteSearchComponent.AC_MATCH_CORRECT_WORD_ORDERING_PARAM_NAME, "true", 
        AutoCompleteSearchComponent.COMPONENT_NAME, "true")
        ,"//result[@name='response'][@numFound='0']"
    );
  }
  
  @Test
  public void testAcGroupingWithHandler() {
    assertQ(req(CommonParams.QT, "dismax_ac", 
        CommonParams.Q, "bo",
        AutoCompleteSearchComponent.AC_GROUPING_FIELD_PARAM_NAME, "is_sponsored",
        AutoCompleteSearchComponent.AC_GROUPING_FIELD_DEFINITION_PARAM_NAME, "false:3 true:1 false:2",
        AutoCompleteSearchComponent.COMPONENT_NAME, "true")
        ,"//result[@name='response'][@numFound='6']"
        ,"//result[@name='response']/doc[1]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[2]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[3]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[4]/bool[@name='is_sponsored'][.='true']"
        ,"//result[@name='response']/doc[5]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[6]/bool[@name='is_sponsored'][.='false']"
        
        ,"//result[@name='response']/doc[1]/str[@name='phrase'][.='bono and bob marley 1']"
        ,"//result[@name='response']/doc[2]/str[@name='phrase'][.='bono and bob marley 3']"
        ,"//result[@name='response']/doc[3]/str[@name='phrase'][.='bob dylan']"
    );
    
    assertQ(req(CommonParams.QT, "dismax_ac_groupingHandlers", 
        CommonParams.Q, "bo",
        AutoCompleteSearchComponent.AC_GROUPING_FIELD_PARAM_NAME, "is_sponsored",
        AutoCompleteSearchComponent.AC_GROUPING_FIELD_DEFINITION_PARAM_NAME, "false:3 true:1 false:2",
        AutoCompleteSearchComponent.COMPONENT_NAME, "true")
        ,"//result[@name='response'][@numFound='6']"
        ,"//result[@name='response']/doc[1]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[2]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[3]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[4]/bool[@name='is_sponsored'][.='true']"
        ,"//result[@name='response']/doc[5]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[6]/bool[@name='is_sponsored'][.='false']"
        
        ,"//result[@name='response']/doc[1]/str[@name='phrase'][.='bono and bob marley 1']"
        ,"//result[@name='response']/doc[2]/str[@name='phrase'][.='bono and bob marley 3']"
        ,"//result[@name='response']/doc[3]/str[@name='phrase'][.='bob marley & the wailers 2']"
    );
  }
  
  @Test
  public void testAcGroupingWithSort() {
    assertQ(req(CommonParams.QT, "dismax_ac", 
        CommonParams.Q, "bo",
        AutoCompleteSearchComponent.AC_GROUPING_FIELD_PARAM_NAME, "is_sponsored",
        AutoCompleteSearchComponent.AC_GROUPING_FIELD_DEFINITION_PARAM_NAME, "false:3 true:1 false:2",
        AutoCompleteSearchComponent.COMPONENT_NAME, "true")
        ,"//result[@name='response'][@numFound='6']"
        ,"//result[@name='response']/doc[1]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[2]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[3]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[4]/bool[@name='is_sponsored'][.='true']"
        ,"//result[@name='response']/doc[5]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[6]/bool[@name='is_sponsored'][.='false']"
        
        ,"//result[@name='response']/doc[1]/str[@name='phrase'][.='bono and bob marley 1']"
        ,"//result[@name='response']/doc[2]/str[@name='phrase'][.='bono and bob marley 3']"
        ,"//result[@name='response']/doc[3]/str[@name='phrase'][.='bob dylan']"
    );
    
    assertQ(req(CommonParams.QT, "dismax_ac_groupingHandlers", 
        CommonParams.Q, "bo",
        AutoCompleteSearchComponent.AC_GROUPING_FIELD_PARAM_NAME, "is_sponsored",
        AutoCompleteSearchComponent.AC_GROUPING_FIELD_DEFINITION_PARAM_NAME, "false:3 true:1 false:2",
        AutoCompleteSearchComponent.AC_GROUPING_SORT_PARAM_NAME, "is_sponsored:X_at_the_top",
        AutoCompleteSearchComponent.COMPONENT_NAME, "true")
        ,"//result[@name='response'][@numFound='6']"
        ,"//result[@name='response']/doc[1]/bool[@name='is_sponsored'][.='true']"
        ,"//result[@name='response']/doc[2]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[3]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[4]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[5]/bool[@name='is_sponsored'][.='false']"
        ,"//result[@name='response']/doc[6]/bool[@name='is_sponsored'][.='false']"
    );
  }
}