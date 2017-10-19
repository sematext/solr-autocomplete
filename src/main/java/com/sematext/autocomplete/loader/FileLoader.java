/*
 *    Copyright (c) 2007-2009 Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any actual or intended
 *    publication of such source code.
 */
package com.sematext.autocomplete.loader;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.common.SolrInputDocument;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;

/**
 * Reads AutoComplete items from standard input and sends them to the AutoComplete backend.
 * The input should be line-oriented.  Values should be prefixed with field names that match the
 * AutoComplete backend configuration.  Multiple fields should be tab-separated.  For example:
 * <pre>
 *   phrase:First Item Here       url:http://example.com/First
 *   phrase:Second Item Here      url:http://example.com/Second
 * </pre>
 * Lines starting with the <code>#</code> character are skipped.
 *
 * @author sematext, http://www.sematext.com/
 */
public class FileLoader {
    protected SolrClient solr;
    protected int docs = 0;

    public static void main(String[] args) throws IOException, SolrServerException {
        if (args.length < 1) {
            System.out.println("Usage: cat <InputFile> | java -Dfile.encoding=UTF8 -Dclient.encoding.override=UTF-8 -Xmx256m -Xms256m -server " + FileLoader.class.getName() + " <AutoCompleteSolrUrl>");
            System.exit(0);
        }
        FileLoader loader = new FileLoader(args[0]);
        loader.run();
    }

    public FileLoader(String solrURL) throws MalformedURLException {
        solr = new ConcurrentUpdateSolrClient.Builder(solrURL).withQueueSize(10000).withThreadCount(2).build();
    }

    protected void run() throws SolrServerException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line = reader.readLine();
        while (line != null) {
            if (!line.startsWith("#")) {
                docs++;
                solr.add(makeDoc(line));
                if (docs % 1000 == 0) {
                  System.out.println("Docs: " + docs);
                }
            }
            line = reader.readLine();
        }
        reader.close();
        System.out.println("Optimizing...");
        solr.optimize();
    }

    /**
     * Creates a {@link SolrInputDocument} out of the input.
     * @param line the input to use for constructing the {@link SolrInputDocument}
     * @return the populated {@link SolrInputDocument}
     */
    protected SolrInputDocument makeDoc(String line) {
        SolrInputDocument doc = new SolrInputDocument();
        String[] pairs = line.split("\t");
        for (String pair : pairs) {
            String[] nv = pair.split(":", 2);
            doc.addField(nv[0], nv[1]);
        }
        return doc;
    }
}
