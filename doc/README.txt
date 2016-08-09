==AutoComplete Solr Setup==

1. put AC schema.xml in Solr's conf dir
2. start servlet container with Solr
3. Load AC data into AC
4. TODO: Tomo, what else is needed?

==Loading AC Suggestions==

AC can be defined on a field in the "main" index or in a separate AC
index.

If AC lives in the "main" index, data is loaded into a special AC
field as documents are sent to the main index for indexing.

If AC lives as a separate AC index, this index needs to be populated
with data from some data source: with a file (or CSV) loader, index
loader if the data is in field in another index, or DataImportHandler
if data is in a database.

To load AC with data from an existing index, the "main" index field
used for AC must be stored (not just indexed).  The IndexLoader tool
is used load data into the AC index.

To load phrases from a text file into AC, use FileLoader.

AC can also be loaded from a custom UpdateRequestProcessor (URP).  The
custom URP intercepts documents sent to the main index and posts their
fields used for AC to the AC index.

==Custom Suggestion Ordering==

Possible approaches:
1. Index-time document boosting
2. Function query
   2.1. ExternalFileField
3. Sorting

## Using Index-time Document Boosting

Index-time Document Boosting is good when you have a numeric value
associated with every document and you want it to affect the ranking
somewhat, but not fully override it.  To use Index-time Document
Boosting, use a numeric value such popularity as index-time boost
factor.  The higher the boost the higher the rank.  For example:

<add>
  <doc boost="1"><field name="phrase">A Beautiful Mind</field></doc>
  <doc boost="2"><field name="phrase">A Believer Sings the Truth</field></doc>
  <doc boost="3"><field name="phrase">A Broken Frame</field></doc>
  <doc boost="4"><field name="phrase">A Che</field></doc>
  <doc boost="5"><field name="phrase">A Clockwork Orange</field></doc>
  <doc boost="6"><field name="phrase">A Clue For Scooby Doo</field></doc>
  <doc boost="7"><field name="phrase">A Concert Behind Prison Walls</field></doc>
  <doc boost="8"><field name="phrase">A Day at the Races (album)</field></doc>
  <doc boost="9"><field name="phrase">A Farewell to Arms</field></doc>
  <doc boost="10"><field name="phrase">A Kind Of Magic</field></doc>
</add> 

If data is fed using another mechanism, such as SolrJ, the equivalent
method for setting Document boost should be used.

The final ordering will depend on the relevance score, which will be
*somewhat* affected by the boost.  There are other factors that are
included in relevance score computation, and a custom Similarity
should be used to control ranking more precisely than a simple boost
factor can provide.  For instance, a custom Similarity implementation
may want to override the computeNorm and lengthNorm methods and
eliminate the score dependency on the number of terms in the phrase
field.  The DefaultSimilarity class favours shorter terms and punishes
long terms, which is why with DefaultSimilarity matching phrases with
equal boost will always be returned in shorter-phrases-first,
longer-phrases-second, with the longest phrases last order.  If this
is not acceptable or desirable, a custom Similarity is needed.  A more
drastic measure could simply have all Similarity methods return a
constant, other than the method that involves the boost factor, thus
eliminating any scoring and letting the boost have absolute control
over the order. (TODO: This can also be achieved by sorting on a
numeric field, right Tomo?)  If a custom Similarty is used, it must be
set in schema.xml and AC data needs to be indexed with it.


## FunctionQuery Boosting

If AC is embeded in the main index then one can't use document
boosting, as it would affect hit scoring as well.  However, we can use
a Function Query to change suggestion ordering as follows.  We set the
popularity of the phrase in a separate document field
(e.g. "popularity") like this:

<add>
  <doc><field name="phrase">A Beautiful Mind</field><field name="popularity">10</field></doc>
  <doc><field name="phrase">A Believer Sings the Truth</field><field name="popularity">20</field></doc>
  <doc><field name="phrase">A Broken Frame</field><field name="popularity">30</field></doc>
  <doc><field name="phrase">A Che</field><field name="popularity">40</field></doc>
  <doc><field name="phrase">A Clockwork Orange</field><field name="popularity">50</field></doc>
  <doc><field name="phrase">A Clue For Scooby Doo</field><field name="popularity">60</field></doc>
  <doc><field name="phrase">A Concert Behind Prison Walls</field><field name="popularity">70</field></doc>
  <doc><field name="phrase">A Day at the Races (album)</field><field name="popularity">80</field></doc>
  <doc><field name="phrase">A Farewell to Arms</field><field name="popularity">90</field></doc>
  <doc><field name="phrase">A Kind Of Magic</field><field name="popularity">100</field></doc>
</add>

We then change the client query and add _val_:"ord(popularity)" to the
query string.  An example of a YUI page using such a function query is
in web/auto-complete-function-query-order.html file.

If ordering is expected to change frequently, one should consider this
alternative approach that keeps numerical values (like "popularity"),
one for each document, in an external file that can be more frequently
regenerated without requiring reindexing of the AC data:

http://lucene.apache.org/solr/api/org/apache/solr/schema/ExternalFileField.html


## Sorting

Sorting is a simple solution for absolute ordering. If we have
popularity field set we can simply add "&sort=popularity desc".
AutoComplete YUI using sort is configured in
web/auto-complete-sort.html


==UI Customization==

Look and feel of UI (colors and font styles) can be customized by
editing css/autocomplete.css, also many functional parameters can be
defined (customized) in autocomplete.js:

## Minimum Query Length

By default, as soon as a user starts typing characters into the input
element, the AutoComplete control starts batching characters for a
query to the DataSource.

You may increase how many characters the user must type before
triggering this batching, which can help reduce load on a server,
especially if the first few characters of the input string will not
produce meaningful query results.

	// Require user to type at least 1 characters before triggering a query 
	YAHOO.widget.AutoComplete.prototype.minQueryLength = 1; 

## Maximum Results Displayed

You can define maximum number of results to display in results
container:

	// Display up to 10 results in the container 
	YAHOO.widget.AutoComplete.prototype.maxResultsDisplayed = 10; 

	
## Animation

If you include the YUI Animation utility on your web page, you can
enable animation on the transitions of the AutoComplete container
element using the following code:
	
	YAHOO.widget.AutoComplete.prototype.animVert = true;
	 
	YAHOO.widget.AutoComplete.prototype.animHoriz = false; 
	 
	YAHOO.widget.AutoComplete.prototype.animSpeed = 0.3;
	
By default, if the Animation utility is present, the container will
animate vertically, but not horizontally, over 0.3 seconds.

## Query Delay

By default, AutoComplete batches user input and sends queries 0.1
seconds from the last key input event.  You may adjust this delay for
optimum user experience and/or server load. Keep in mind that this
value only reflects the delay before sending queries, and any delays
in receiving query results that may be caused by server or
computational latency will not be reflected in this value.  This value
must be a Number greater than 0.

	YAHOO.widget.AutoComplete.prototype.queryDelay = 0.2;
	
## Auto-highlight

By default, when the container populates with query results, the first
item in the list will be automatically highlighted for the user. Use
the following code to disable this feature:

	YAHOO.widget.AutoComplete.prototype.autoHighlight = false;

## Use Shadow

If you would like the container element to have a drop-shadow, be sure
to define a class yui-ac-shadow and enable the feature with the
following code

	YAHOO.widget.AutoComplete.prototype.useShadow = true;

## Auto-highlight

By default, when the container populates with query results, the first
item in the list will be automatically highlighted for the user. Use
the following code to disable this feature:

	YAHOO.widget.AutoComplete.prototype.autoHighlight = false; 
	
For full list of common configurations go here http://developer.yahoo.com/yui/autocomplete/#configs

==Wikipedia Demo SetUp==

First download all titles wikipedia dump for languages, for example:
http://download.wikimedia.org/hrwiki/20080303/hrwiki-latest-all-titles-in-ns0.gz
http://download.wikimedia.org/enwiki/20081008/enwiki-latest-all-titles-in-ns0.gz
http://download.wikimedia.org/eswiki/20081126/eswiki-latest-all-titles-in-ns0.gz

Create folders for language cores and copy config files from /conf in
core/conf. Edit multicore feature in example/multicore/solr.xml to
point to core dirs. Start Solr:
    
    java -Dsolr.solr.home=multicore -jar start.jar

Load wikipedia all-titles dump for specific language to Solr using
FileLoader (in language specific core), for example:

$ for lang in hr es en; do echo $lang; cat wiki/${lang}wiki-latest-all-titles-in-ns0  | sed -e 's/_/ /g' | java -cp ac.jar com.sematext.autocomplete.loader.FileLoader http://localhost:8080/solr/coreAC-$lang; done
