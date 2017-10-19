package com.sematext.autocomplete;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;

public class LoadPerformaceTester {

    /**
     * @param args
     * @throws MalformedURLException
     */
    public static void main(String[] args) throws MalformedURLException {

        if (args.length < 2) {
            System.out.println("Usage: LoadPerformanceTester <inputFile> <solrUrl>");
            System.exit(0);
        }

        FileReader input;
        SolrClient solr = new HttpSolrClient.Builder(args[1]).build();

        try {
            input = new FileReader(args[0]);
            BufferedReader bufRead = new BufferedReader(input);

            String line = bufRead.readLine();
            while (line != null) {

                SolrInputDocument simpleDocument = new SolrInputDocument();
                simpleDocument.addField("phrase", line.toLowerCase());

                try {
                    solr.add(simpleDocument);
                } catch (SolrException se) {
                    se.printStackTrace();
                } catch (SolrServerException e) {
                    e.printStackTrace();
                }

                line = bufRead.readLine();
            }

            solr.commit();
            solr.optimize();

            bufRead.close();
            input.close();
            solr.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SolrServerException e) {
            e.printStackTrace();
        }
    }
}
