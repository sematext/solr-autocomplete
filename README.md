[![Build Status](https://travis-ci.org/sematext/solr-autocomplete.svg?branch=master)](https://travis-ci.org/sematext/solr-autocomplete)

# Solr AutoComplete

The `AutoComplete` (`AC`) enhances the search experience through suggest-as-you-type and auto-complete functionality built into the search form. As one starts to enter search terms, the module detects a pause in typing and offers a list of suggested searches. One can then easily pick one of the suggestions or continue refining the suggestions by typing in more of the query. For example, if you type `"bass"` `AC` might offer suggestions that include `"bass fishing"` or `"bass guitar"`, and even `"sea bass"` (note how `"bass"` is not necessarily the first word)

## Contents
- [Support Matrix](#support-matrix)
- [Build](#build)
- [Release Notes](#release-notes)
- [Advanced Functionality](#advanced-functionality)
- [How it Works](#how-it-works)
- [Installing AC into Your Application Server](#installing-ac-into-your-application-server)
  - [Deploying AC Back-end](#deploying-ac-back-end)
  - [Feeding AC Back-end from Command-line](#feeding-ac-back-end-from-command-line)
    - [FileLoader](#fileloader)
    - [IndexLoader](#indexloader)
  - [Feeding AC with DataImportHandler](#feeding-ac-with-dataimporthandler)
  - [Updating AC in Real-time](#updating-ac-in-real-time)
  - [Adjusting AC schema.xml](#adjusting-ac-schema-xml)
  - [Testing AC Backend](#testing-ac-backend)
  - [Integrating AC into Search Form](#integrating-ac-into-search-form)
  - [UI Customization](#ui-customization)
    - [Minimum Query Length](#minimum-query-length)
    - [Maximum Results Displayed](#maximum-results-displayed)
    - [Animation](#animation)
    - [Query Delay](#query-delay)
    - [Auto-highlight](#auto-highlight)
    - [Use Shadow](#use-shadow)
  - [Using Advanced Functionality](#using-advanced-functionality)
    - [Misspelling Correction](#misspelling-correction)
    - [Suggestions Grouping](#suggestions-grouping)
    - [Full Word Match Boosting](#full-word-match-boosting)
    - [Matching Word Ordering Boost](#matching-word-ordering-boost)
    - [Custom Data in AC GUI Component](#custom-data-in-ac-gui-component)
    - [Parameter Matrix](#parameter-matrix)
    - [AutoComplete in Distributed Environment](#autocomplete-in-distributed-environment)
    - [HTTPS](#https)
    - [Performance Considerations](#performance-considerations)
  - [License](#license)
- [Contact](#contact)

## Support Matrix
<table>
  <tr>
    <th>solr-autocomplete</th>
    <th>solr</th>
  </tr>
  <tr>
    <td>1.6.5.2.0</td>
    <td>5.2.0</td>
  </tr>
  <tr>
    <td>1.6.6.0.1</td>
    <td>6.0.1</td>
  </tr>
  <tr>
    <td>1.6.6.3.0</td>
    <td>6.3.0</td>
  </tr>
</table>

## Build
You need maven and JDK 8:

```
$ mvn clean package
```

## Release Notes
#### 1.6.6.3.0 (2016-12-09)
- Support for Solr 6.3.0

#### 1.6.6.0.1 (2016-06-13)
- Support for Solr 6.0.1

## Advanced Functionality
* __Automatic misspelling correction__ – if you type `“washeng”` or `“vashin”`, both incomplete misspellings of `“washington”`, `AC` will offer suggestions like `“washington”` or `“washington times”`.
* __Suggestions grouping by a field__ – for instance, the first 2 suggestions could be sponsored, and the rest unsponsored; or the first 5 suggestions could be book titles, followed by 4 dvd titles, with 1 cd title at the end, assuming you have these 3 types of items and their titles loaded into `AC`.
* __Full word match boosting__ pushes to the top suggestions which contain the exact words from partially entered queries . For instance, if a person enters `“new”`, the phrase `“new york”` would be pushed above `“newton”`.
* __Matching “word ordering” boost__ pushes to the top those suggestions that match the order of query terms. For instance, if user enters `“washington tim”`, `AC` will push suggestion `“the washington times article”` above `“times in washington”`.
* __Custom data in AC GUI component__ – GUI component can display not just the text of `AC` suggestions, but also other data, such as images, URL links, “sponsored link” message, description, price, etc., as demonstrated in the example GUI component that comes with `AC`.

All advanced functionality is optional and one can combine individual pieces of functionality as one needs it. The example `solrconfig.xml` shows how to configure one simple dismax handler to provide all these functionalities. Different combinations are achieved by sending different sets of parameters to your dismax handler (more about the parameters in later sections). At the same time, core functionality is available out-of-the-box, too -- just send your queries to `dismax_ac` handler without advanced parameters.

## How it Works
`AC` consists of a Solr-powered back-end, the AJAX/JavaScript front end, and command-line tools. The back-end consists of a special Solr schema suitable for fast retrieval of partial words and phrases. The AJAX attaches to any existing HTML search form. It detects pauses in typing and issues queries to the back-end as the user keeps typing. The returned suggestions are displayed as a list attached to the search field. The command-line tools are used for loading suggestion items into the `AC` back-end.

## Installing AC into Your Application Server
To install `AC` into solr place the following libraries from the package into your `$SOLR_HOME/server/solr-webapp/webapp/WEB-INF/lib/` directory:
* st-AutoComplete-xxx.jar
* st-ReSearcher-core-xxxjar

### Deploying AC Back-end
Deployment to an existing, but fresh non-multi-core Solr:

1. Copy `solrconfig.xml` and `schema.xm`l to the Solr `$SOLR_HOME/server/solr/collection1/conf/` directory
2. Optionally adjust the names of fields in `schema.xml` (describer later in the document)
3. Start the solr

At this point the `AC` back-end should be running, but will be empty and thus have no data to suggest.

### Feeding AC Back-end from Command-line

To feed `AC` back-end with data, use one of the two command-line loaders:

* FileLoader
* IndexLoader

As their names imply, `FileLoader` gets data to push into `AC` back-end from a file, while `IndexLoader` gets data from the index.  Because `IndexLoader` reads directly from the local Lucene index given a path to index directory, this `IndexLoader` should either be used when the index is not sharded or it should be run against all index shards.

To satisfy their run-time dependencies, make sure the `CLASSPATH` is set correctly. You can do that by making use of the included `bin/prep-classpath.sh` script:

```
$ export CLASSPATH=$CLASSPATH:`./bin/prep-classpath.sh`
```

#### FileLoader

Alternatively, you can use it with the `java -cp` command-line argument as shown below. The `FileLoader` reads data from standard input, which makes it easy to load the contents of example files in `example/exampledocs` directory using the following UNIX command:

```
cat example/exampledocs/just-phrases.txt | java -cp `./bin/prep-classpath.sh` com.sematext.autocomplete.loader.FileLoader http://localhost:8983/solr/collection1
```

This will read each line from the standard input (from `just-phrases.txt`) and send it to the `AC back-end` running at `http://localhost:8983/solr/collection1`. Look at `just-phrases.txt` to learn about the format of the file/input.

#### IndexLoader

If you have an existing Solr or Lucene index on the local server and you’d like to load one of its stored fields into the `AC` back-end at `http://localhost:8983/solr/collection1`, use the following command to invoke `IndexLoader`:

```
$ java -cp `./bin/prep-classpath.sh` com.sematext.autocomplete.loader.IndexLoader /path/to/index http://localhost:8983/solr/collection1 productName,phrase
```

Note how the main index field name `“productName”` was mapped to the `AC` field name `“phrase”` defined in `schema.xml`. It is also important to note that any index fields specified must be stored in the index.
To load multiple index fields into the `AC` back-end simply list additional field pairs on the command-line. The following example loads fields `id`, `bookTitle` and `bookURL` into `AC` fields `id`, `title` and `url`, respectively:

```
$ java -cp `./bin/prep-classpath.sh` com.sematext.autocomplete.loader.IndexLoader /path/to/index http://localhost:8983/solr/collection1 id,id bookTitle,title bookURL,url
```

Note that this assumes you have correctly added the `“id”`, `“title”` and `“url”` fields to the `AC` `schema.xml`.

#### Feeding AC with DataImportHandler

The `AC` index can also be populated with data from a Solr index using the `DataImportHandler (DIH)` `SolrEntityProcessor` [http://wiki.apache.org/solr/DataImportHandler#SolrEntityProcessor]
To use this one must enable `DIH` in `solrconfig.xml` by registering the `DIH` handler

```xml
<requestHandler name="/dataimport" class="org.apache.solr.handler.dataimport.DataImportHandler">
    <lst name="defaults">
      <str name="config">data-config.xml</str>
    </lst>
</requestHandler>
```

The configuration is provided in two places:

1. `solrconfig.xml` -- data config file location is added here 
2. `data-config.xml` -- configures the data source and mapping from the source index to the `AC` core

```xml
<dataConfig>
  <document>
    <entity name="sep"
    processor="SolrEntityProcessor"
    url="http://localhost:8080/solr/main-index"
    query="*:*"
    fl="id,project,title,type,url"
    rows="1000"
    transformer="TemplateTransformer"
    format="javabin">
      <field column="id" name="id" />
      <field column="project" name="project" />
      <field column="title" name="phrase" />
      <field column="type" name="type_desc" />
      <field column="url" name="link" />
      <field column="sponsored" template="false" />
    </entity>
  </document>
</dataConfig>
```

To invoke this handler and popular `AC` call the following URL:

```
http://localhost:8080/solr/autocomplete/dataimport?command=full-import.
```

#### Updating AC in Real-time

Instead of periodically loading suggestions into `AC` as described earlier, one can keep `AC` updated with suggestions in real-time. This is useful when suggestions are based on fields in documents being added to the main index and there is a need to keep them in sync with the main index. For example, if the main index contains documents with book data and suggestions provided by `AC` are book titles, then one can use `AutocompleteUpdateRequestProcessor` to insert a book title into `AC` whenever a new book document is indexed. The configuration attribute `'fields'` defines a list of fields whose values will used to create `AC` suggestions. The `AutoComplete` component should be deployed to `/lib` directory of your main index and configured in your main index `solrconfig.xml` as follows:

```xml
<updateRequestProcessorChain>
  <processor class="com.sematext.autocomplete.urp.AutocompleteUpdateRequestProcessorFactory">
    <str name="separator">,</str>

    <!-- Use only when AC core is deployed in a separate Solr instance
    <str name="solrAC">http://localhost:8080/solr/autocomplete</str>
    -->

    <!--Use with embedded Solr AC core; when AC core is deployed in same Solr and 'main
        index' -->
    <str name="solrAC">autocomplete</str>

    <!--Fields which will be added, [ ] fields will be treated as array fields which will 
        be tokenized by 'separator' -->
    <str name="fields">title,[attribution],{tags},category</str>
    <str name="copyAsIsFields">type,genre</str>
  </processor>
  <processor class="solr.RunUpdateProcessorFactory" />
  <processor class="solr.LogUpdateProcessorFactory" />
</updateRequestProcessorChain>
```

Each document added to main solr core for indexing will be processed by custom `AC` update request processor and values of fields specified in the `“fields"` parameter will be extracted, processed, and added to `AC` as suggestions.

The `copyAsIsFields` attribute defines a list of comma-separated field names that should be copied to `AC` as is. Both their names and values are copied directly without any modification.. Such fields cannot be used as suggestions. Instead, they can optionally be used for suggestion filtering using `fq` parameter. For instance, if field `“type”` is copied to `AC`, then `AC` queries could have an additional parameter `“&fq=type:book”` in order to restrict suggestions to those that originated from documents of type book The `AC` update request processor can be used in two modes: with an `AC` core running in the same Solr instance, or with an `AC` core running in a separate Solr instance. If the `AC` core is in the same Solr instance as the main index, then only core name needs to be set in solr `AC` parameter, like `“autocomplete”` in the previous config example. If the `AC` core is deployed in a separate Solr instance, then the URL to `AC` Solr core needs to be set in the solr `AC` parameter, like `http://localhost:8080/solr/autocomplete`.

The advantage of the embedded `AC` core is that there is no HTTP overhead when indexing new documents into `AC`.

`AC` `UpdateRequestProcessor` doesn’t commit after each new document (since version 1.2.2). This means that users can define their own commit scheme, as it suits their needs. For instance, starting point can be configuration like the following:

```xml
<!-- AC Update Request Processor doesn't call commit after adding a new doc so users can specify their own commit policy through usage of autoCommit and autoSoftCommit settings. We suggest using autoSoftCommit with period of 5 sec and hard commit with period of 10 min -->
    <autoCommit>
      <maxTime>600000</maxTime>  <!-- hard commit every 10 minutes -->
      <openSearcher>false</openSearcher>
    </autoCommit>
    <autoSoftCommit>
      <maxTime>60000</maxTime>  <!-- soft commit every 60 seconds -->
    </autoSoftCommit>
```

##### Customizing phrase created by AC UpdateRequestProcessor

It is possible to adjust phrase field value before the document is added to `AC` index. `AC` `URP` provides a convenient method which can be overriden by creating a subclass of Sematext’s `AutocompleteUpdateRequestProcessor` class (and a factory class for your subclass):

```java
protected String decoratePhrase(String phraseFieldValue, SolrInputDocument mainIndexDoc)
```

In you decide to use this approach, the only difference in setup and configuration is to use the name of your `AC` `URP` subclass in `updateRequestProcessorChain` in your `solrconfig.xml`.

### Adjusting AC schema.xml

The example `schema.xml` assumes `AC` Solr instance will be fed documents with `“phrase”` fields. For deployments where `AC` needs to offer only one type of suggestion (e.g. only product names) this `schema.xml` will suffice. When a single `AC` instance needs to offer different types of suggestions or simply be fed different types of data, the example `schema.xml` will need to be adjusted.

For instance, if we feed `AC` with book titles and DVD titles, and we want to be able to get `AC` to offer either only book titles or only DVD titles as suggestions, predefined field `“type”` can be used for storing either `“dvd”` or `“book”` value, depending on whether some suggestion is valid only for books or only for dvds.

However, in case you need to add publisher's name to each suggestion, you'd just have to add new field to the schema:

```xml
<field name="publisher" type="string" indexed="true" stored="true"/> 
```

In such cases, you'll be able to extract publisher of some product from the data returned by `AC`.

However, note that the content of the field `“publisher”` will not be used to match `AC` suggestions. If you wanted that too, you'd have to fill such value again into field `“phrase”` when importing data into `AC` index.

### Testing AC Backend

Once you’ve started the `AC` Solr backend and fed it data to use as `AC` suggestions, you can quickly test it by opening URLs such as the ones below:

```
http://localhost:8080/solr/select/?q=PartialPhrase&qt=dismax_ac
http://localhost:8080/solr/select/?q=PartialBookTitle&qt=dismax_ac
http://localhost:8080/solr/select/?q=PartialDVDTitle&qt=dismax_ac&ac_spellcheck=true&ac_matchFullWords=true
```

When even partial queries (in other words, queries without fully spelled out key words) match documents fed into the `AC` back-end and results are returned as XML responses, it means your `AC` back-end is set up correctly.

### Integrating AC into Search Form

Once `AC` has been populated, you should look at the content of auto-complete.html and edit it to set the correct URL that points to your `AC` back-end, whether to Solr itself or via Apache. Similarly, edit the included `autocomplete.js` file by looking for the line that contains the CHANGE.TO.YOUR.SERVER.HERE string end editing that URL to fit your environment. This is the URL that will be called when the user selects one of the `AC` suggestions.

Once these adjustments have been made, open the adjusted auto-complete.html in your browser and start typing the text that matches the content of `just-phrases.txt` or any other data that’s been fed into the `AC` back-end.

Once this is working, mimic what’s in the `auto-complete.html` source and integrate the needed HTML and JavaScript into your own search form.

### UI Customization

Look and feel of UI (colors and font styles) can be customized by editing `css/autocomplete.css`, also many functional parameters can be defined (customized) in `autocomplete.js`:

#### Minimum Query Length

By default, as soon as a user starts typing characters into the input element, the `AutoComplete` control starts batching characters for a query to the `DataSource`.

You may increase how many characters the user must type before triggering this batching, which can help reduce load on a server, especially if the first few characters of the input string will not produce meaningful query results.

```
// Require user to type at least 1 characters before triggering a query 
YAHOO.widget.AutoComplete.prototype.minQueryLength = 1; 
```
#### Maximum Results Displayed

You can define maximum number of results to display in results container:

```
// Display up to 10 results in the container 
YAHOO.widget.AutoComplete.prototype.maxResultsDisplayed = 10;
```
#### Animation

If you include the YUI Animation utility on your web page, you can enable animation on the transitions of the `AutoComplete` container element using the following code:

```
YAHOO.widget.AutoComplete.prototype.animVert = true;
YAHOO.widget.AutoComplete.prototype.animHoriz = false; 
YAHOO.widget.AutoComplete.prototype.animSpeed = 0.3;
```

By default, if the Animation utility is present, the container will animate vertically, but not horizontally, over 0.3 seconds.

#### Query Delay

By default, `AutoComplete` batches user input and sends queries 0.1 seconds from the last key input event.  You may adjust this delay for optimum user experience and/or server load. Keep in mind that this value only reflects the delay before sending queries, and any delays in receiving query results that may be caused by server or computational latency will not be reflected in this value.  This value must be a number greater than 0.

```
YAHOO.widget.AutoComplete.prototype.queryDelay = 0.2;
```

#### Auto-highlight

By default, when the container populates with query results, the first item in the list will be automatically highlighted for the user. Use the following code to disable this feature:

```
YAHOO.widget.AutoComplete.prototype.autoHighlight = false;
```

#### Use Shadow

If you would like the container element to have a drop-shadow, be sure to define a class yui-ac-shadow and enable the feature with the following code

```
YAHOO.widget.AutoComplete.prototype.useShadow = true;
```

For a full list of common configurations see [http://developer.yahoo.com/yui/autocomplete/#configs]

### Using Advanced Functionality

Everything needed to use advanced capabilities is already configured in the example `solrconfig.xml` and `schema.xml`. Depending on which capabilities you want to use, you may need to tweak some settings and delete or comment out parts which are not related to them, to keep `AC` index from growing unnecessarily large. All advanced functionalities (except `Suggestions Grouping`) require the usage of `dismax` handler (`dismax_ac` from sample `solrconfig.xml` is a great starting point).

#### Misspelling Correction

Would you like `AC` to suggest, for example, `“washington”` or `“washington times”` even though a person misspelled the first word when he partially entered `“washeng”` or `“vashin”`? If so, you should enable misspelling autocorrection. Note, however, that enabling this does come at the price of somewhat larger `AC` index and more complex `AC` queries.

To achieve this functionality, just send parameter `ac_spellcheck=”true”`. Also, field `phraseSpellchecking` in `schema.xml` must be uncommented, together with related `copyField` command for it. If you don't need spellchecking in your `AC` setup, comment out these two definitions from your `schema.xml` to save on `AC` index space. Of course, if you decide to enable this functionality at some point after you’ve already created the `AC` index, you will have to uncomment these fields and rebuild your `AC` index.

To get spellchecked suggestions, you can use predefined `dismax_ac` handler and send it parameter `“ac_spellcheck=true”`, it is as simple as that. Queries sent to this handler might look like this:

```
http://localhost:8080/solr/select/?q="washington%20tim"&qt=dismax_ac_&ac_spellcheck=true
```

**IMPORTANT NOTE**: when using `Solr 4.0` and higher, `dismax` search handler used for `AC` spellechking has to be configured with setting:

```xml
<str name="mm">100%</str>
```

Older versions of Solr don't need this setting, although it can't hurt.

#### Suggestions Grouping

This advanced functionality enables you to achieve things like:

* the first 2 suggestions should be sponsored, the rest unsponsored suggestions (handy way to embed advertising directly into `AC`)
* the first 5 suggestions should be books, then 4 dvds and 1 cd at the end (handy way to include multiple types of items into a single `AC` suggestion list, but specifying the order and quantity for each type)

To enable this functionality 3 things need to be done:

1. Query requests   have to be sent to a request handler with `acComponent` defined in its components chain. In the example `solrconfig.xml`, you can use `dismax_ac`.
2. Query requests   have to contain the name of the field to be used as criterion for grouping. The URL parameter's name is `ac_grouping_field`. There can be only one such parameter in the request. Also, when query request is sent to the search handler with `acComponent` in its chain, but this parameter is not defined in the request, `acComponent` will not do anything. Here is one example of a request whose results would be grouped by `is_sponsored` field:

```
http://localhost:8080/solr/select/?q="wash"&qt=dismax_ac&ac_grouping_field=is_sponsored
```

3. Grouping definition for field specified by the `ac_grouping_field` must be present. The definition specifies the order groups should appear in, and how many items each group should have. For instance, to achieve:

```
“the first 2 suggestions should be sponsored, the rest unsponsored”
```

the `ac_grouping_field` value would have to be `“is_sponsored”`, while grouping definition would have to be:

```
true:2  false:8
```
or, to achieve:

```
“the first 5 suggestions should be books, then 4 dvds and 1 cd at the end”
```

the `ac_grouping_field` value would have to be `“type”`, while grouping definition would have to be:

```
book:5  dvd:4 cd:1
```

There are two ways one can define grouping definition. One way is by specifying it in the `solrconfig.xml`, in the definition of `acComponent`. Under the list `acGroupingFieldDefinitions`, you can define grouping rules for any field you intend to group on, like this:

```xml
<searchComponent name="acComponent"  class="com.sematext.autocomplete.solr.AutoCompleteSearchComponent">    
  <str name="searchHandler">dismax_ac_query</str> <!-- optional -->
  <lst name="acGroupingFieldDefinitions">   
    <str name="is_sponsored">true:2 false:8</str>   
    <str name="type">book:5 dvd:4 cd:1</str>  
  </lst>    
</searchComponent>
```

`searchHandler` is the `requestHandler` use to get number of search result for autocomplete search, it should be contains only query component. This param is optional, if we don’t use this feature, the request handler that contains this component must also contains query component.

The second way to define grouping definition is to send it with a query request. For the grouping field specified by `ac_grouping_field`, you can also specify grouping definition in the parameter `ac_grouping_field_def`, like this (example URL is not URL-encoded for readability):

```
http://localhost:8080/solr/select/?q="wash"&qt=dismax_ac&ac_grouping_field=is_sponsored&ac_grouping_field_def=true:2 false:8
```

In case the parameter `ac_grouping_field_def` is provided in the request, definitions from `solrconfig.xml` are ignored. In other words, this is a way to override the value from the configuration.

There are a couple of things to keep in mind while using `“suggestions grouping”` functionality:

1. In case you want to use `“suggestions grouping”` while at the same time filter the `AC` suggestions by using Solr's `fq` parameter, you can do that only if you don't filter by the same field which is used for `“suggestions grouping”`,
2. Since `“suggestions grouping”` groups each suggestion into one of the specified groups, all phrases in the `AC` index need to have some value for the fields you intend to group by. For instance, in case of field `is_sponsored`, if you set the field value to true for all sponsored suggestions, but don't set it to false for absolutely all unsponsored ones, then not all unsponsored suggestions will show in grouped suggestions.

#### Full Word Match Boosting

This functionality pushes to the top suggestions which contain the exact words from the queries. For instance, if a person enters `“new”`, the phrase `“new york”` would be pushed above `“newton”`.

To get this functionality, use `dismax_ac handler` and send a parameter `ac_matchFullWords=true`. Also, field `matchFullWords` must be defined in `schema.xml`. In case you don't find this functionality useful, it is best to comment out the definition of this field in `schema.xml` together with related copyField command.

An example of request which uses this functionality is here:

```
http://localhost:8080/solr/select/?q="new"&qt=dismax_ac&ac_grouping_field=is_sponsored&ac_grouping_field_def=true:2 false:8&ac_matchFullWords=true
```

This example also shows that you can combine various advanced functionalities at the same time, like suggestions grouping and full word match boosting.

#### Matching Word Ordering Boost

This functionality pushes to the top suggestions matching the order of query terms. For instance, if a person enters `“washington tim”`, `AC` will push suggestion `“the washington times article”` above `“times in washington”`, even though the latter may be considered a closer match otherwise.

To get this functionality, use `dismax_ac` handler and send a parameter `ac_matchWordOrder=true`. Also, field `matchCorrectWordOrdering` must be defined in `schema.xml`. In case you don't find this functionality useful, it is best to comment out the definition of this field in `schema.xml` together with related `copyField` command.

An example of request which uses this functionality is here:

```
http://localhost:8080/solr/select/?q="washintgon tim"&qt=dismax_ac&ac_matchFullWords=true&ac_matchWordOrder=true
```

#### Custom Data in AC GUI Component

You can add as many custom fields to the schema.xml as you want and display them on your GUI as you wish. The example `AC` GUI component shows how to achieve this, as the screenshot below shows.

##### Parameter Matrix

Not all parameters can be sent together. The following table summarizes relations between various `AC` parameters.

`ac_grouping_field`|`ac_grouping_field_def`|`ac_spellcheck`|`ac_matchFullWords`|`ac_matchWordOrder`|`ac_pureCompletion`
----|----|----|----|----|----|
`ac_grouping_field`||Y|Y|Y|Y|Y
`ac_grouping_field_def`|Y||Y|Y|Y|Y
`ac_spellcheck`|Y|Y||Y|Y|N
`ac_matchFullWords`|Y|Y|Y||Y|Y
`ac_matchWordOrder`|Y|Y|Y|Y||Y
`ac_pureCompletion`|Y|Y|N|Y|Y|

As you can see, almost all combinations are allowed. The only exception is usage of `ac_pureCompletion` and `ac_spellcheck`, since the functionalities they provide are actually incompatible (`ac_pureCompletion` searches for a way to simply finish exact phrase which was entered by user, while `ac_spellcheck` considers possibility of a misspelling).

Another thing to note is that by not sending both `ac_pureCompetion` and `ac_spellcheck`, you would get pre 1.1 functionality based on `prefixTok`.

#### AutoComplete in Distributed Environment

`AutoComplete` works both in non-distributed (single node or simple master-slave) and distributed setup. If you are using `SolrCloud`, everything will work out-of-the-box automatically. There are no special parameters which should tell whether the setup is distributed or not. Also, there is only one version of `AutoComplete` jar which knows how to work in all kinds of setups.

If you are using `“manually”` configured distributed search (where you manually define `“shards”` parameter in your requests or in `solrconfig.xml`), a few things may have to be adjusted:

* Make sure you properly use `shards.qt` parameter - if non-default search handler is used, you should mention its name with this parameter. If you already had a functional distributed setup before using `AutoComplete`, chances are you are already using `shards.qt` parameter correctly, so you should adjust `AutoComplete` request handler in a similar way - to hit `AutoComplete` handler on other shards.
* If you plan to use standard request handler (where `AutoComplete` is added) you shouldn’t have `“shards”` parameter defined in solrconfig (this is true regardless of `AutoComplete` itself, because it can cause infinite recursion inside of Solr). If you are not using standard handler, you are most likely fine. If not, that means you can do one of the following:
  1. specify `“shards”` parameter in request sent to Solr (which is not practical in all cases)
  2. define a separate request handler in solrconfig for `AutoComplete`. This separate request handler can be a copy of your original query handler in everything but `“shards”` parameter. Your client application would still send requests to your original request handler, but you would have to add `shards.qt` parameter whose value should match the name of the request handler `“copy”`.

There is an exception to this case: if you have one aggregator shard sitting in front of N other shards, where aggregator is the only one receiving requests and the only one having `“shards”` parameter in its config, you can ignore this step. In that case, you need AutoComplete configuration only on your aggregator shard, other sub-shards don’t need it.

#### HTTPS

If Solr is exposed through SSL, `shardHandlerFactory` configuration should be added to `AutoComplete` component:

```xml
<searchComponent name="acComponent"  class="com.sematext.autocomplete.solr.AutoCompleteSearchComponent">    
  <lst name="acGroupingFieldDefinitions">   
    <str name="is_sponsored">true:2 false:8</str>   
    <str name="type">book:5 dvd:4 cd:1</str>  
  </lst>    
  <shardHandlerFactory class="HttpShardHandlerFactory">
    <str name="urlScheme">https://</str>
    <int name="socketTimeOut">1000</int>
    <int name="connTimeOut">5000</int>
  </shardHandlerFactory>
</searchComponent>
```

#### Performance Considerations

Although the example `schema.xml` and `solrconfig.xml` have all possible fields enabled by default, that is not the best choice for most setups. Here are some things that need to be considered to achieve optimal performance:

##### Which fields are really needed for desired set of functionalities?

One should check the table in the previous section and identify which parameters will be sent to `AC`. For each parameter which will not be used, its field and accompanying copyField instruction should be commented out of `schema.xml`, since their presence increases the size of the index. `Solrconfig.xml` in this case doesn't have to be changed.

##### Using standard or dismax handlers?

In all cases standard search handlers will work faster than dismax ones. However, for the ease of use, dismax handlers are a better choice. In case your `AC` index isn't very large (for instance, 100000 `AC` phrases isn't considered large), you might want to use pre-defined dismax search handler as you will hardly notice any difference in speed between dismax and standard handler. That is also our recommendation: try the performance of your `AC` setup with dismax handler first.  

In case you notice sub-optimal performance, you can easily switch to using standard search handlers. However, advanced features (except suggestions grouping) are provided only by `dismax_ac` handler.

##### Introducing separate handler for AC grouping

Default configurations of `AC` request handlers have `“query”` component in their component chains. It is required to provide full `AC` functionality, but in some cases it brings a small overhead as it executes one extra query. If you are looking to maximally optimize `AC` performance, you can do the following:

* define a separate request handler with just **query** component. Let’s call it `acRequestHandler`.
* remove **query** component from the request handler that contains the `AC` component.  This is the request handler that your client side is hitting directly.
* adjust `AC` component definition by adding `<str name="searchHandler">acRequestHandler</str>`

In that case, your `AC` component definition could look like this:

```xml
<searchComponent name="acComponent" class="com.sematext.autocomplete.solr.AutoCompleteSearchComponent">
  <str name="searchHandler">acRequestHandler</str>
  <lst name="acGroupingFieldDefinitions">
    <str name="is_sponsored">true:1 false:3</str>
  </lst>
</searchComponent>
```

Note that performance gain will not be large in most cases, so unless you really need to tune `AC` performance, we suggest sticking with the simpler default configuration.

## License

Query Autocomplete is released under Apache License, Version 2.0

## Contact

For any questions ping [@sematext](http://www.twitter.com/sematext)
