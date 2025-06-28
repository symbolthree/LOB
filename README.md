# LOB  - a CLI tool to download and update LOB in Oracle table

## Usage

### Windows
`LOB.exe -config [config file]`
### Linux
`java -jar lob-2.0.jar -config [config file]`

### Note
- Default config file is `lob-config.json` under the same folder, if `-config` is not specified.

- The zip archive contains `LOB.exe` and `lob-2.0.jar`. File `LOB.exe` is Windows program launcher only.

## Prerequisite
- JRE 8 or higher

## Sample config file 
> `SELECT CLOB_FILE FROM LOB_TEST WHERE ID=1`
- FIle is downloaded as `c:\output\myClob.xml`
<pre>
{
  "jdbcUrl"      : "jdbc:oracle:thin:@server:1521:SID",
  "user"         : "user",
  "password"     : "password",
  "action"       : "SELECT",
  "table"        : "LOB_TEST",	
  "column"       : "CLOB_FILE",
  "where"        : "ID=1",
  "lobFilePath"  : "C:\output"
  "lobFileName"  : "myClob.xml"
}
</pre>

> `UPDATE LOB_TEST SET CLOB_FILE=[C:\input\myClon.xml] WHERE ID=1`
- File `C:\input\myClon.xml` is updated to that LOB column

<pre>
{
  "jdbcUrl"      : "jdbc:oracle:thin:@server:1521:SID",
  "user"         : "user",
  "password"     : "password",
  "action"       : "UPDATE",
  "table"        : "LOB_TEST",	
  "column"       : "CLOB_FILE",
  "where"        : "ID=1",
  "lobFilePath"  : "C:\input"
  "lobFileName"  : "myClob.xml"
}
</pre>

## Parameters

|Parameter | Descrption |
|----------|------------|
|jdbcUrl   |standard Oracle JDBC URL|
|user   |username|
|password   |password|
|table   |Table or View name|
|action   |SELECT or UPDATE|
|column   |LOB column name|
|where   |where clause which identifies your target LOB column|
|lobFilePath |full path of the download LOB file or the LOB file to be updated to|
|lobFileName |filename of the download LOB file or the LOB file to be updated to|

* Current running folder is used if `lobFilePath` is empty 

## CLI arguments

<pre>
usage: LOB
 -action &lt;arg&gt;     SELECT or UPDATE
 -column &lt;arg&gt;     LOB column name
 -config &lt;arg&gt;     user-specific config file. Default is lob-config.json
 -help             show help
 -jdbcUrl &lt;arg&gt;    Oracle JDBC URL
 -lobFile &lt;arg&gt;    Obseleted. Use lobFilePath and lobFileName
 -lobType &lt;arg&gt;    Obseleted. The LOB type is found internally
 -password &lt;arg&gt;   DB password
 -table &lt;arg&gt;      target table or view name
 -user &lt;arg&gt;       DB user
 -where &lt;arg&gt;      where clause to identify this LOB value
</pre>

## How to Use
- Choose `action` as `SELECT` or `UPDATE`
- Specify `column`, `table`, and `where` parameters to identify which LOB value you want to download or update
- To make it more script-friendly, one can provide a template config file with `jdbcUrl`, `user`, `password`, `action`, `column` and `table`, and pass the argument `where` and `lobFileName` to override the values in this template, e.g.

  <pre>
  LOB.exe -config template.json -where "ID=101" -lobName 101.xml
  LOB.exe -config template.json -where "ID=102" -lobName 102.xml
  </pre>

- This program only run on the first selected LOB column value, and then exit. So if the where clause gives multiple rows, only the first one will be processed.

## New in 2.0

- The file name can be constructed dynamically by using the existing parameters and date variable in the config file, e.g.

  - Original SQL Select statement is `SELECT LOB_COLUMN FROM LOB_TABLE WHERE ID=123`
  - `lobFileName` in the config file is `${table}-${column}-${where}-${date:yyyy-MM-dd}.xml`
 
 - Config file is  
    <pre>
    {
      "jdbcUrl"      : "jdbc:oracle:thin:@server:1521:SID",
      "user"         : "user",
      "password"     : "password",
      "action"       : "SELECT",
      "table"        : "LOB_TABLE",	
      "column"       : "LOB_COLUMN",
      "where"        : "ID=123",
      "lobFilePath"  : "${table}-${column}-${where}-${date:yyyy-MM-dd}.xml"
      "lobFileName"  : "myClob.xml"
    }
    </pre>
  
  - Output Filename becomes `LOB_TABLE-LOB_COLUMN-ID=123-2025-07-01.xml`
  
  - Special characters will be removed for filename syntax restriction.
 - Substitution rule is borrowed from Apache common `StringSubstitutor`.
   > https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringSubstitutor.html 


