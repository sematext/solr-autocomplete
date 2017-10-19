package com.sematext.autocomplete.urp;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;
import org.apache.solr.util.plugin.SolrCoreAware;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class AutocompleteUpdateRequestProcessorFactory extends UpdateRequestProcessorFactory implements SolrCoreAware {

    private String solrAC;
    private SolrClient solrACServer;
    private String separator;
    private SolrCore core;

    private List<String> fields = new ArrayList<String>();
    private List<String> copyAsIsFields = new ArrayList<String>();

    @Override
    @SuppressWarnings("rawtypes")
    public void init(NamedList args) {
        super.init(args);

        solrAC = (String) args.get("solrAC");

        if (solrAC.startsWith("http:")) {

            // Used when AC core is deployed on separate Solr
            this.solrACServer = new HttpSolrClient.Builder(solrAC).build();
        }

        this.separator = (String) args.get("separator");

        String fieldsStr = (String) args.get("fields");
        String copyAsIsFieldsStr = (String) args.get("copyAsIsFields");

        if (fieldsStr == null) {
            throw new RuntimeException(
                    "Can't initialize AutocompleteUpdateRequestProcessor unless fields are specified");
        }

        StringTokenizer tok = new StringTokenizer(fieldsStr, ",");
        while (tok.hasMoreTokens()) {
            fields.add(tok.nextToken().trim());
        }
        
        if (copyAsIsFieldsStr != null) {
          String [] fs = copyAsIsFieldsStr.split(",");
          for (String f : fs) {
            copyAsIsFields.add(f.trim());
          }
        }
    }

    @Override
    public UpdateRequestProcessor getInstance(SolrQueryRequest req, SolrQueryResponse rsp,
            UpdateRequestProcessor nextURP) {
        if (this.solrACServer == null) {
            // Used with embedded Solr AC core; when AC core is deployed on same Solr and 'main index'
            this.solrACServer = new EmbeddedSolrServer(core.getCoreContainer(), solrAC);
        }

        return new AutocompleteUpdateRequestProcessor(solrACServer, fields, copyAsIsFields, separator, nextURP);
    }

    @Override
    public void inform(SolrCore core) {
        this.core = core;
    }
}
