/*
 *    Copyright (c) 2007-2009 Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any actual or intended
 *    publication of such source code.
 */
package com.sematext.autocomplete.loader;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;

/**
 * Reads specified fields from each document in the given index and sends each field as a separate AC
 * document/suggestion to the AutoComplete backend. If field is marked as array field (surrounded by []) it will be
 * tokenized on separator and each token will be indexed as separate AC document/suggestion
 *
 * Usage: <code>
 * java -Dfile.encoding=UTF8 -Dclient.encoding.override=UTF-8 -Xmx256m -Xms256m -server com.sematext.autocomplete.loader.CustomIndexLoader
 * /path/to/index AutoCompleteSolrUrl indexField1,acField1 indexField2,acField2 ...
 * </code>
 *
 * @author sematext, http://www.sematext.com/
 */
public class CustomIndexLoader {
    static final String PHRASE = "phrase";
    static final String TYPE = "type";

    static int countDistinctNewDocs = 0;
    static int countIgnoredOriginalDocs = 0;
    static int countCreatedAcDocs = 0;

    static boolean mergingWithOldDocsEnabled = true;
    
    static long totalSearchTime = 0;
    
    @SuppressWarnings("unused")
    public static void main(String[] args) throws CorruptIndexException, IOException, SolrServerException {

        if (args.length < 3) {
            System.err.println("Usage: java -Dfile.encoding=UTF8 -Dclient.encoding.override=UTF-8 -Xmx256m -Xms256m -server " + CustomIndexLoader.class.getName()
                    + " </path/to/index> <AutoCompleteSolrUrl> <PhraseFieldName1,[PhraseFieldName2],..>"
                    + " optional:-C:<copyField1,copyField2,copyField3:acField1...>"
                    + " optional:-I:<ignoreField1:value1,value2;ignoreField2:value3,value4,value5...>"
                    + " optional:-S:<array field separator>"
                    + " optional:-DM (disables merging of docs)");
            System.exit(0);
        }

        System.out.println("CustomIndexLoader starting");
        DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(args[0])));
        int docs = reader.maxDoc();
        SolrClient solr = new ConcurrentUpdateSolrClient.Builder(args[1]).withQueueSize(2000).withThreadCount(2).build();
        Set<SolrInputDocument> batch = new HashSet<SolrInputDocument>(1000);

        String[] fieldNames = args[2].split(",");
        
        String[] copyAsIsFields = new String [0];
        Map<String, String []> copyFields = new HashMap<String, String []>();
        Map<String, String []> ignoreFields = new HashMap<String, String []>();
        String separator = "";
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 3; i < args.length; i++) {
          if (args[i].startsWith("-C:")) {
            copyAsIsFields = args[i].substring(3).split(",");
          } else if (args[i].startsWith("-S:")) {
            separator = args[i].substring(3);
          } else if (args[i].startsWith("-I:")) {
            String [] iDefs = args[i].substring(3).split(";");
            
            for (String def : iDefs) {
              String [] tmp = def.split(":");
              ignoreFields.put(tmp[0], tmp[1].split(","));
            }
          } else if (args[i].startsWith("-DM")) {
            mergingWithOldDocsEnabled = false;
          }
        }

        System.out.println("CustomIndexLoader found " + docs + " documents in main index");
        
        Bits liveDocs = MultiFields.getLiveDocs(reader);
        
        // go through all docs in the index
        for (int i = 0; i < docs; i++) {
            printCurrentStats(i, startTime, docs);
          
            // process doc only if not deleted
            if (liveDocs == null || liveDocs.get(i)) {
                copyFields.clear();
                // first extract all fields which should be copied as-is
                for (String fieldName : copyAsIsFields) {
                  // if there is : separator, then use only part related to main index fieldName
                  String oldFieldName = fieldName.split(":")[0];
                  String [] values = reader.document(i).getValues(oldFieldName);
                  copyFields.put(fieldName, values);
                }
                
                boolean excludeDoc = false;
                String excludeFieldName = null;
                String excludeFieldValue = null;
                for (String fieldName : ignoreFields.keySet()) {
                  String [] values = reader.document(i).getValues(fieldName);
                  
                  for (String val : values) {
                    for (String valIgnore : ignoreFields.get(fieldName)) {
                      if (val.equals(valIgnore)) {
                        excludeDoc = true;
                        excludeFieldName = fieldName;
                        excludeFieldValue = val;
                        break;
                      }
                    }
                  }
                }
                
                if (excludeDoc) {
                  countIgnoredOriginalDocs++;
                  // System.out.println("Excluding doc based on field '" + excludeFieldName + "' with value '" + excludeFieldValue + "'");
                  continue;
                }
              
                for (String fieldName : fieldNames) {

                    boolean arrayField = fieldName.startsWith("[") && fieldName.endsWith("]");

                    boolean tokenizeField = fieldName.startsWith("{") && fieldName.endsWith("}");

                    IndexableField field = null;
                    if (arrayField || tokenizeField) {
                        field = reader.document(i).getField(fieldName.substring(1, fieldName.length() - 1));
                    } else {
                        field = reader.document(i).getField(fieldName);
                    }

                    if (field != null) {

                        String phrase = field.stringValue();

                        if (arrayField || tokenizeField) {

                            String[] phrases = null;

                            if (arrayField) {
                                phrases = phrase.split(separator);
                            } else {
                                phrases = phrase.split(" ");
                            }

                            for (String value : phrases) {
                                SolrInputDocument doc = fetchExistingOrCreateNewSolrDoc(solr, value.trim());
                                addField(doc, PHRASE, value.trim());
                                addField(doc, "type", fieldName.substring(1, fieldName.length() - 1));
                                addCopyAsIsFields(doc, copyFields);
                                solr.add(doc);
                            }

                        } else {
                            // System.out.println("Adding doc for phrase : " + phrase);
                            SolrInputDocument doc = fetchExistingOrCreateNewSolrDoc(solr, phrase);
                            addField(doc, PHRASE, phrase);
                            addField(doc, TYPE, fieldName);
                            addCopyAsIsFields(doc, copyFields);
                            solr.add(doc);
                        }
                    }

                }
            }
        }
        if (!batch.isEmpty())
            solr.add(batch);
        reader.close();
        System.out.println("Optimizing...");
        solr.optimize();
        System.out.println("Done...");
    }

    private static void printCurrentStats(int i, long startTime, int totalDocCount) {
      if (i < 1000) {
        if (i % 100 == 0) {
          if (i == 0) {
            System.out.println(getHeaderMessage(i, totalDocCount));
          }
          else {
            System.out.println(getHeaderMessage(i, totalDocCount) + " - " + getProcessedStats() + " - " + getTimeStats(startTime, totalDocCount, i));
          }
        }
      } else {
        if (i % 1000 == 0) {
          System.out.println(getHeaderMessage(i, totalDocCount) + " - " + getProcessedStats() + " - " + getTimeStats(startTime, totalDocCount, i));
        }
      }/* else {
        if (i % 10000 == 0) {
          System.out.println(getHeaderMessage(i, totalDocCount) + " - " + getProcessedStats() + " - " + getTimeStats(startTime, totalDocCount, i));
        }
      }*/
    }

    private static String getProcessedStats() {
      return "ignored original docs = " + countIgnoredOriginalDocs + ", new AC docs prepared = " + countCreatedAcDocs + 
             ", new first-time docs inserted into AC = " + countDistinctNewDocs;
    }

    private static String getTimeStats(long startTime, long totalDocCount, long currentDoc) {
      return "current duration = " + formatTime(System.currentTimeMillis() - startTime) + ", time spent in search = " + formatTime(totalSearchTime) + ", ETA = " + formatTime(estimateRemaining(System.currentTimeMillis() - startTime, totalDocCount, currentDoc));
    }

    private static long lastCurrentDoc = 0;
    private static long lastCurrentDuration = -1;
    
    private static long estimateRemaining(long currentDuration, long totalDocCount, long currentDoc) {
      double lastPeriodDocsPerMiliSec = ((double) currentDoc - (double) lastCurrentDoc) / ((double) currentDuration - (double) lastCurrentDuration);
      double remainingDocs = totalDocCount - currentDoc;
      
      lastCurrentDoc = currentDoc;
      lastCurrentDuration = currentDuration;
      
      return (long) (remainingDocs / lastPeriodDocsPerMiliSec);
    }

    private static String formatTime(long l) {
      String res = "";
      // convert to seconds
      l = l / 1000;
      
      if (l >= 60 * 60) {
        res += String.valueOf((l / (60 * 60))) + " hrs, ";
        l = l % (60 * 60);
      }
      if (l >= 60) {
        res += String.valueOf((l / (60))) + " min, ";
        l = l % (60);
      }
      res += l + " sec";
      
      return res;
    }

    private static String getHeaderMessage(int i, long totalDocCount) {
      return "CustomIndexLoader processing original doc " + (i + 1) + "/" + totalDocCount;
    }

    private static void addField(SolrInputDocument doc, String name, String value) {
      // find if such field already exists
      if (doc.get(name) == null) {
        // System.out.println("Adding field " + name + " without previous values");
        doc.addField(name, value);
      } else {
        // for some fields we can't allow multiple values, like ID field phrase, so we have to perform this check
        SolrInputField f = doc.get(name);
        for (Object val : f.getValues()) {
          // fix for boolean values
          if ((value.equalsIgnoreCase("t") && val.toString().equalsIgnoreCase("true")) ||
              (value.equalsIgnoreCase("f") && val.toString().equalsIgnoreCase("false"))) {
                return;
          }
          if (value.equals(val.toString())) {
            // if we find such value in the doc, we will not add it again
            // System.out.println("Field " + name + " already contains value " + value);
            return;
          }
        }
        // System.out.println("Adding field " + name + " without new value " + value);
        f.addValue(value);
      }
    }

    private static void addField(SolrInputDocument doc, String name, String [] values) {
      // find if such field already exists
      if (doc.get(name) == null) {
        doc.addField(name, values);
      } else {
        // for some fields we can't allow multiple values, like ID field phrase, so we have to perform this check
        SolrInputField f = doc.get(name);
        for (String v : values) {
          boolean valueAlreadyIn = false;
          for (Object val : f.getValues()) {
            if (v.equals(val.toString())) {
              // if we find such value in the doc, we will not add it again
              valueAlreadyIn = true;
              break;
            }
            // fix for boolean values
            if ((v.equalsIgnoreCase("t") && val.toString().equalsIgnoreCase("true")) ||
                (v.equalsIgnoreCase("f") && val.toString().equalsIgnoreCase("false"))) {
              valueAlreadyIn = true;
              break;
            }
          }
          
          if (!valueAlreadyIn) {
            f.addValue(v);
          }
        }
      }
    }

    private static SolrInputDocument fetchExistingOrCreateNewSolrDoc(SolrClient solr, String id) throws SolrServerException, IOException {
      countCreatedAcDocs++;

      if (!mergingWithOldDocsEnabled) {
        // if disabled, always use fresh document and override older docs with the same phrase
        return new SolrInputDocument();
      }
      if (id.equals("")) {
        return new SolrInputDocument();
      }
      
      Map<String, String> p = new HashMap<String, String>();
      p.put("q", PHRASE + ":\"" + ClientUtils.escapeQueryChars(id) + "\"");
     
      long t1 = System.currentTimeMillis();
      
      SolrParams params = new MapSolrParams(p);
      QueryResponse res = solr.query(params);
      
      totalSearchTime += (System.currentTimeMillis() - t1);
      
      if (res.getResults().size() == 0) {
        // System.out.println("Document for phrase " + id + " NOT FOUND");
        countDistinctNewDocs++;
        return new SolrInputDocument();
      } else if (res.getResults().size() == 1) {
        SolrDocument doc = res.getResults().get(0);
        SolrInputDocument tmp = new SolrInputDocument();
        
        // System.out.println("Document for phrase " + id + " found");
        
        for (String fieldName : doc.getFieldNames()) {
          tmp.addField(fieldName, doc.getFieldValue(fieldName));
        }
        return tmp;
      } else {
        throw new IllegalStateException("Query with params : " + p + " returned more than 1 hit!");
      }
    }

    private static void addCopyAsIsFields(SolrInputDocument doc, Map<String, String []> copyFields) {
      for (String fName : copyFields.keySet()) {
        String [] fNames = fName.split(":");
        // if field has a different name in AC index than in main index, use that name, otherwise use main index name
        addField(doc, fNames[fNames.length - 1], copyFields.get(fName));
      }
    }
}
