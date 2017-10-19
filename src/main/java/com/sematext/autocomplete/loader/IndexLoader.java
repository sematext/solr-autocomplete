/*
 *    Copyright (c) 2007-2009 Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any actual or intended
 *    publication of such source code.
 */
package com.sematext.autocomplete.loader;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Reads AutoComplete items from specified fields in the given index and sends them to the AutoComplete backend.
 * Usage:
 * <code>
 * java -Dfile.encoding=UTF8 -Dclient.encoding.override=UTF-8 -Xmx256m -Xms256m -server com.sematext.autocomplete.loader.IndexLoader
 * /path/to/index AutoCompleteSolrUrl indexField1,acField1 indexField2,acField2 ...
 * </code>
 * @author sematext, http://www.sematext.com/
 */
public class IndexLoader {

    public static void main(String[] args) throws CorruptIndexException, IOException, SolrServerException {

        if (args.length < 3) {
            System.err.println("Usage: java -Dfile.encoding=UTF8 -Dclient.encoding.override=UTF-8 -Xmx256m -Xms256m -server " + IndexLoader.class.getName()
                    + " </path/to/index> <AutoCompleteSolrUrl> <indexField1,acField1> [indexField2,acField2 ... ]");
            System.exit(0);
        }
        Map<String,String> fieldMap = getFieldMapping(args, 2);
        DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(args[0])));
        int docs = reader.maxDoc();
        SolrClient solr = new ConcurrentUpdateSolrClient.Builder(args[1]).withQueueSize(10000).withThreadCount(2).build();
        Set<SolrInputDocument> batch = new HashSet<SolrInputDocument>(1000);
        
        Bits liveDocs = MultiFields.getLiveDocs(reader);
        
        // go through all docs in the index
        for (int i = 0; i < docs; i++) {
            // process doc only if not deleted
            if (liveDocs == null || liveDocs.get(i)) {
                // loop through all fields to be looked at
                SolrInputDocument doc = new SolrInputDocument();
                Iterator<String> iter = fieldMap.keySet().iterator();
                
                boolean phraseFieldEmpty = false;
                
                while (iter.hasNext()) {
                    String indexField = iter.next();
                    String acField = fieldMap.get(indexField);
                    IndexableField field = reader.document(i).getField(indexField);
                    String value = field != null ? reader.document(i).getField(indexField).stringValue() : null;
                    
                    if (field != null && value != null && !value.isEmpty()) {
                      doc.addField(acField, value);
                    } else {
                      // not very relevant piece of info
                      // System.err.println("Field is null or empty, skipping: " + indexField);
                      
                      if (acField.equalsIgnoreCase("phrase")) {
                        System.err.println("Since AC phrase field would be null, this doc will not be created: " + reader.document(i));
                        phraseFieldEmpty = true;
                        break;
                      }
                    }
                }

                if (!phraseFieldEmpty) {
                  solr.add(doc);
                  if (docs % 1000 == 0) {
                    System.out.println("Docs: " + docs);
                  }
                }
            }
        }
        if (!batch.isEmpty())
            solr.add(batch);
        reader.close();
        System.out.println("Optimizing...");
        solr.optimize();
        solr.close();
    }

    private static Map<String,String> getFieldMapping(String[] pairs, int offset) {
        HashMap<String,String> map = new HashMap<String,String>();
        for (int i=offset; i<pairs.length; i++) {
            String[] fields = pairs[i].split(",");
            map.put(fields[0], fields[1]);
        }
        return map;
    }
}
