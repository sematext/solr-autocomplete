package com.sematext.autocomplete.solr;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;

import com.sematext.autocomplete.tst.AutoCompleteService;
import com.sematext.autocomplete.tst.DoublyLinkedList.DLLIterator;

public class AutoCompleteRequestHandler extends RequestHandlerBase {

    private static AutoCompleteService service;

    @Override
    @SuppressWarnings("rawtypes")
    public void init(NamedList arg0) {
        super.init(arg0);
        service = new AutoCompleteService("etc/hr-wiki-titles.txt");

    }

    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void handleRequestBody(SolrQueryRequest request, SolrQueryResponse response) throws Exception {

        String searchString = request.getParams().get("searchString");

        searchString = searchString.trim();

        try {
            DLLIterator it = service.matchPrefix(searchString).iterator();

            List<NamedList<String>> matches = new ArrayList<NamedList<String>>();

            while (it.hasNext()) {
                String word = (String) it.next();

                NamedList<String> node = new SimpleOrderedMap<String>();
                node.add("Title", word);

                matches.add(node);
            }

            if (matches.size() > 0) {

                response.add("ResultSet", matches);
            }
        }

        catch (RuntimeException e) {
            response.add("status", "failed");
        }

    }

}
