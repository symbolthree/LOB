package symbolthree.oracle.lob;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.stream.MalformedJsonException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.apache.commons.text.StringSubstitutor;

public class LOB {
	
	private static String defaultConfig = "lob-config.json";
	//private String sqlStmt;
	private String lobFile="";
	private String lobFileName="";
	private String lobFilePath="";
	private String jdbcUrl="";
	private String user="";
	private String password="";
	private String lobType="";
	private String action="";
	private String column="";
	private String table="";
	private String where="";
	
	Options options = new Options();	
	private Connection conn;
	private int LOB_READ_CHUNK_SIZE = 2048;
	
	public static void main(String[] args) {
		
		String banner = "\r\n***** L O B *****\r\n";
		
		System.out.println(banner);
		
		try {
			DriverManager.registerDriver (new oracle.jdbc.driver.OracleDriver());
		} catch (SQLException e) {
			System.out.println("Unable to find Oracle driver");
			System.exit(1);
		}
		LOB lob = new LOB();

		try {
			lob.programOptions(args);
		} catch (ParseException e1) {
			System.out.println("Error of processing arguments");
			lob.showHelp();
			System.exit(1);
		} catch (MalformedJsonException mje) {
			System.out.println("Config is not valid JSON file");
			System.exit(1);
		} catch (IOException ioe) {
			System.out.println("Unable to read config file");
			System.exit(1);
		}
		
		if (!lob.checkParameters()) {
			System.exit(1);
		}
		
		try {
			Instant start = Instant.now();
			lob.execute();
			Instant finish = Instant.now();
			float time = (Duration.between(start, finish).toMillis())/1000f;
	        System.out.println(String.format("Operation time elapsed : %.2f s", time));
			
		} catch (SQLException sqle) {
			System.out.println(sqle.getMessage());
			System.exit(1);
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());
		}
	}

	private void execute() throws SQLException, IOException {
		if (action.equalsIgnoreCase("SELECT")) {
			System.out.println("Download LOB from column " + column + " of table " + table + " ...");
			executeSelect();
		} else if (action.equalsIgnoreCase("UPDATE")) {
			System.out.println("Updating LOB of column " + column + " of table " + table + " ...");			
			executeUpdate();
		} else {
			System.out.println("Action must be SELECT or UPDATE");			
		}
		
	}
	
	private void readConfigFile(String _config) throws IOException, MalformedJsonException {
		File configFile = null;
		if (_config != null) {
			configFile = new File(_config); 
	    } else {
	    	configFile = new File(System.getProperty("user.dir"), defaultConfig);	    	
        }
		
		if (!configFile.exists()) {
			System.out.println("Unable to find " + configFile.getAbsolutePath());
			System.exit(1);
		}
		
		System.out.println("Reading config " + configFile.getAbsolutePath());			
        Gson gson = new Gson();
		BufferedReader br = new BufferedReader(new FileReader(configFile));
	    Map<?, ?> map = gson.fromJson(br, Map.class);
	    
	    jdbcUrl  = (String)map.get("jdbcUrl");
	    user     = (String)map.get("user");
	    password = (String)map.get("password");
	    //lobType  = (String)map.get("lobType");
	    action   = (String)map.get("action");
	    column   = (String)map.get("column");
	    table    = (String)map.get("table");
	    where    = (String)map.get("where");
	    lobFilePath = (String)map.get("lobFilePath");
	    lobFileName = (String)map.get("lobFileName");
	    
	    br.close();
	}


	private void executeSelect() throws SQLException, IOException {
		conn = DriverManager.getConnection(jdbcUrl, user, password);
		
		String newSQL = "SELECT " + column + ", DBMS_LOB.GETLENGTH(" + column + ") FROM " + table + " WHERE " + where;
		
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(newSQL);
		ResultSetMetaData md = rs.getMetaData();
		
		int colType = md.getColumnType(1);
		
		String colTypeName = JDBCType.valueOf(colType).getName();
		System.out.println("\tlobType is " + colTypeName);
		System.out.println("\tlobFilePath is " + lobFilePath);
		System.out.println("\tlobFile is " + lobFileName);

        int sizeWritten = 0;
        int precentageShown = 0;
        
		if (rs.next()) {
			int filesize =  rs.getInt(2);
			System.out.println("\tFile size = " + String.format("%,d bytes", filesize));
		
			if (colTypeName.equals("CLOB")) {
				Clob clob = rs.getClob(1);
				if (clob != null) {
		          File file = new File(lobFilePath, lobFileName);
		          if (file.exists()) file.delete();

				  Reader reader = clob.getCharacterStream();
		          FileWriter writer = new FileWriter(file);
		          char[] buffer = new char[LOB_READ_CHUNK_SIZE];
		          int chunkSize = 0;
		          while ((chunkSize = reader.read(buffer)) != -1) {
		        	 char[] chunk = new char[chunkSize];
		        	 chunk = Arrays.copyOf(buffer, chunkSize);
		             writer.write(chunk);
		             buffer = new char[chunkSize];		             
		             sizeWritten = sizeWritten + chunkSize;
		             
		             int precentage = Math.toIntExact(sizeWritten*100 / filesize); 
		             if (precentage%20 == 0 && precentage > precentageShown) {
		            	 System.out.println("\tData Transferred = " + precentage + "%");
		            	 precentageShown = precentage;
		             }
		          }
		          writer.flush();
		          writer.close();
				  System.out.println("CLOB File " + file.getAbsolutePath() + " created");		          
			   }
			}
			
			if (colTypeName.equals("BLOB")) {
				Blob blob = rs.getBlob(1);
				if (blob != null) {
				  InputStream is = blob.getBinaryStream();
		          File file = new File(lobFilePath, lobFileName);
		          if (file.exists()) file.delete();
		          
		          FileOutputStream  fos = new FileOutputStream (file);
		          byte[] buffer = new byte[LOB_READ_CHUNK_SIZE];
		          while (is.read(buffer) > 0) {
		        	  fos.write(buffer);
			          sizeWritten = sizeWritten + LOB_READ_CHUNK_SIZE;
			          
		             int precentage = Math.toIntExact(sizeWritten*100 / filesize); 
		             if (precentage%20 == 0 && precentage > precentageShown) {
		            	 System.out.println("\tData Transferred = " + precentage + "%");
		            	 precentageShown = precentage;
		             }
		          }

		          fos.flush();
		          fos.close();
				  System.out.println("BLOB file " + file.getAbsolutePath() + " created");		          
			   }
			}
		} else {
			System.out.println("No LOB data is selected");
		}
		rs.close();
		conn.close();
	}
	
	private void executeUpdate() throws SQLException, IOException {
		conn = DriverManager.getConnection(jdbcUrl, user, password);
		
		String newSQL = "SELECT " + column + " FROM " + table + " WHERE " + where;
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(newSQL);
		ResultSetMetaData md = rs.getMetaData();
		
		int colType = md.getColumnType(1);
		String colTypeName = JDBCType.valueOf(colType).getName();
		rs.close();
		
		String sqlStmt = "UPDATE " + table + " SET " + column + "=? WHERE " + where;
		
		PreparedStatement ps = conn.prepareStatement(sqlStmt);
		System.out.println("\tlobType is " + colTypeName);
		System.out.println("\tlobFilePath is " + lobFilePath);
		System.out.println("\tlobFile is " + lobFileName);
		lobFile = (new File(lobFilePath, lobFileName)).getAbsolutePath();
		System.out.println("\tlobFile length is " + String.format("%,d bytes", new File(lobFile).length()));
		
		if (colTypeName.equals("CLOB")) {
			BufferedReader reader = new BufferedReader(new FileReader(lobFile));
			ps.setClob(1, reader);
			ps.executeUpdate();
			reader.close();
			ps.close();
			conn.close();
		}
		
		if (colTypeName.equals("BLOB")) {
			BufferedInputStream reader = new BufferedInputStream(new FileInputStream(lobFile));
			ps.setBlob(1, reader);
			ps.executeUpdate();
			reader.close();
			ps.close();
			conn.close();
		}
		System.out.println("The LOB file updated successfully");
	}
	
	private void programOptions(String[] args) throws ParseException, IOException {

		options.addOption(Option.builder("config").hasArg().required(false)
				.desc("user-specific config file. If not specified, " + defaultConfig + " is used").build());
		options.addOption(Option.builder("jdbcUrl").hasArg().required(false)
				.desc("Oracle JDBC URL").build());
		options.addOption(Option.builder("user").hasArg().required(false)
				.desc("DB user").build());
		options.addOption(Option.builder("password").hasArg().required(false)
				.desc("DB password").build());
		options.addOption(Option.builder("action").hasArg().required(false)
				.desc("SELECT or UPDATE").build());
		options.addOption(Option.builder("lobType").hasArg().required(false)
				.desc("Obseleted.").build());
		options.addOption(Option.builder("column").hasArg().required(false)
				.desc("LOB column name").build());
		options.addOption(Option.builder("table").hasArg().required(false)
				.desc("target table or view name").build());
		options.addOption(Option.builder("where").hasArg().required(false)
				.desc("where clause to identify this LOB value").build());
		options.addOption(Option.builder("lobFile").hasArg().required(false)
				.desc("Obseleted. Use lobFilePath and lobFileName").build());
		options.addOption(Option.builder("lobFilePath").hasArg().required(false)
				.desc("file path of the output file for SELECT stmt; input file for UPDATE stmt").build());
		options.addOption(Option.builder("lobFileName").hasArg().required(false)
				.desc("file name of the output file for SELECT stmt; input file for UPDATE stmt").build());
				
		options.addOption("help", false, "show help");

		if (args != null && args.length > 0 && ! args[0].startsWith("-")) {
			System.out.println("Unknown argument " + Arrays.toString(args));
			showHelp();
			System.exit(0);			
		}
		
		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = parser.parse(options, args);
		
		String config = null;
		if (cmd.hasOption("config")) {
			config = cmd.getOptionValue("config");
		}
		readConfigFile(config);		
		
		if (cmd.hasOption("jdbcUrl"))  jdbcUrl  = cmd.getOptionValue("jdbcUrl").trim();
		if (cmd.hasOption("user"))     user     = cmd.getOptionValue("user").trim();
		if (cmd.hasOption("password")) password = cmd.getOptionValue("password").trim();
		if (cmd.hasOption("action"))   action   = cmd.getOptionValue("action").trim();		
		if (cmd.hasOption("lobType"))  lobType  = cmd.getOptionValue("lobType").trim();
		
		if (cmd.hasOption("column"))   column   = cmd.getOptionValue("column").trim();
		if (cmd.hasOption("table"))    table    = cmd.getOptionValue("table").trim();
		if (cmd.hasOption("where"))    where    = cmd.getOptionValue("where").trim();
		
		// Obsoleted, backward support only
		if (cmd.hasOption("lobFile"))  lobFile  = cmd.getOptionValue("lobFile").trim();
		
		// 2.0
	    if (cmd.hasOption("lobFilePath"))  lobFilePath  = cmd.getOptionValue("lobFilePath").trim();
		if (lobFilePath.isEmpty()) lobFilePath = System.getProperty("user.dir");

		if (cmd.hasOption("lobFileName"))  lobFileName  = cmd.getOptionValue("lobFileName").trim();
		lobFileName = resolveFileName(lobFileName);

		if (cmd.hasOption("help")) {
			showHelp();
			System.exit(0);
		}
	}
	
    private boolean checkParameters() {
    	if (jdbcUrl == null  || jdbcUrl.isEmpty()   || 
			user == null     || user.isEmpty()      ||
			password == null ||  password.isEmpty() ||
			action == null   || action.isEmpty()    ||
			//lobFile == null  || lobFile.isEmpty()   ||
			lobFileName == null  || lobFileName.isEmpty()   ||
			//lobFilePath == null  || lobFilePath.isEmpty()   ||
			column == null   || column.isEmpty()    ||
			table == null    || table.isEmpty()) {
    		System.out.println("One or more missing parameter: jdbcUrl, user, password, action, column, table, lobFileName");
    		return false;
    	}
    	return true;
    }
    		
	private void showHelp() {
		HelpFormatter help = new HelpFormatter();
		help.setWidth(100);
		help.printHelp("LOB", options);		
	}
	
	// https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringSubstitutor.html
	private String resolveFileName(String _lobFileName) {
	  Map<String, String> valuesMap = new HashMap<>();
	  valuesMap.put("column", column);
	  valuesMap.put("table", table);
	  String whereClean = where.replaceAll("\\/:*?\"\'<>|", "");
	  valuesMap.put("where", whereClean);
	  StringSubstitutor sub = new StringSubstitutor(valuesMap);
	  String str = sub.replace(_lobFileName);
	  
	  sub = StringSubstitutor.createInterpolator();
	  str = sub.replace(str);
	  
	  return str;
	}
}	


