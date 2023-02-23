# LOB  - a CLI tool to download and update LOB in Oracle table

## Usage

### Windows
`
LOB.exe -config [config file]
`
### Linux/Unix
`
java -jar lob-1.5.jar -config [config file]
`
- Default config file is `lob-config.json` under the same folder, if `-config` is not specified.

> The package contains both `LOB.exe` and `lob-1.5.jar`. Choose one which is fit your needs.

## Prerequisite
- JRE 8 or higher.  `LOB.exe` is a Java executable program.

## Sample config file

<pre>
{
  "jdbcUrl"  : "jdbc:oracle:thin:@server:1521:SID",
	"user"     : "user",
	"password" : "password",
	"action"   : "SELECT",
	"lobType"  : "CLOB",
	"column"   : "CLOB_FILE",
	"table"    : "LOB_TEST",
	"where"    : "ID=1",
	"lobFile"  : "myClob.xml"
}
</pre>

## Parameters

|Parameter | Descrption |
|----------|------------|
|jdbcUrl   |standard Oracle JDBC URL|
|user   |username|
|password   |password|
|action   |SELECT or UPDATE|
|lobType   |CLOB or BLOB. Used when action=UPDATE|
|column   |LOB column name|
|table   |Table or View name|
|where   |where clause which identifies your target LOB colum|
|lobFile   |full path or current folder of the download LOB or the UPDATE LOB file|

## CLI arguments

<pre>
usage: LOB
 -action &lt;arg&gt;     SELECT or UPDATE
 -column &lt;arg&gt;     LOB column name
 -config &lt;arg&gt;     user-specific config file. If not specified, lob-config.json is used
 -help             show help
 -jdbcUrl &lt;arg&gt;    Oracle JDBC URL
 -lobFile &lt;arg&gt;    full file path of the output file for SELECT stmt; input file for UPDATE stmt
 -lobType &lt;arg&gt;    CLOB or BLOB. It is used for UPDATE statement only
 -password &lt;arg&gt;   DB password
 -table &lt;arg&gt;      target table or view name
 -user &lt;arg&gt;       DB user
 -where &lt;arg&gt;      where clause to identify this LOB value
</pre>

## How to Use
- Choose `action` as `SELECT` or `UPDATE`
- Specify `column`, `table`, and `where` parameter to identify which LOB value you want to download or update
- To make it more script-friendly, one can provide a template config file with `jdbcUrl`, `user`, `password`, `action`, `column` and `table`, and pass the argument `where` and `lobFile` to override the values in this template, e.g.

    <pre>
    LOB.exe -config template.json -where "ID=101" -lobFile 101.xml
    LOB.exe -config template.json -where "ID=102" -lobFile 102.xml
    </pre>
- This program only run on the first selected LOB column value, and then exit. So if the where clause gives multiple rows, only the first one will be used.