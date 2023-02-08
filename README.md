# LOB  - a CLI tool to download and update LOB in Oracle table

## Usage
`
LOB.exe -config [config file]
`

Default config file is `lob-config.json` under the same folder, if `-config` is not specified.

## Prerequisite
- JRE 8 or higher.  `LOB` is a Java executable program.

## Sample config file

<pre/>
{
  "jdbcUrl"  : "jdbc:oracle:thin:@server:1521:SID",
  "user"     :"apps",
  "password" : "apps",
  "lobType"  : "BLOB",
  "sqlStmt"  : "UPDATE LOB_TEST SET CLOB_FILE=? WHERE ID=1",
  "lobFile"  : "C:/WORK/abc.xml"
}
</pre>

## Parameters

|Parameter | Descrption |
|----------|------------|
|jdbcUrl   |standard Oracle JDBC URL|
|user   |username|
|password   |password|
|lobType   |CLOB or BLOB. Used when the sqlStmt is a UPDATE statement|
|sqlStmt   |A SELECT statement to download LOB, UPDATE statement to update a LOB|
|lobFile   |full file path of the download LOB of the UPDATE LOB|

## CLI arguments

<pre>
usage: LOB
 -config &lt;arg&gt;     user-specific config file. If not specified, lob-config.json is used
 -help             show help
 -jdbcUrl &lt;arg&gt;    Oracle JDBC URL
 -lobFile &lt;arg&gt;    full file path of the output file for SELECT stmt; input file for UPDATE stmt
 -lobType &lt;arg&gt;    CLOB or BLOB. It is used for UPDATE statement only
 -password &lt;arg&gt;   DB password
 -sqlStmt &lt;arg&gt;    SELECT or UPDATE statement of a particular LOB value
 -user <&lt;arg&gt;       DB user
</pre>

## How to Use
- The SQL statement `sqlStmt` must only select or update the target LOB column only, and the `WHERE` clause to control only *ONE* row is selected in the query
- To make it more script-friendly, one can create provide a template config file with `jdbcUrl`, `user`, `password`, and pass the argument `sqlStmt` and `lobFile` to override the values in this template, e.g.

    <pre>
    LOB.exe -config template.json -sqlStmt "SELECT LOB_VALUE FROM TABLE_A WHERE ID=101" -lobFile 101.xml
    LOB.exe -config template.json -sqlStmt "SELECT LOB_VALUE FROM TABLE_A WHERE ID=102" -lobFile 102.xml
    </pre>
