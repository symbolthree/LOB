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
import java.util.Map;

import com.google.gson.Gson;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class LOB {
	
	private static String defaultConfig = "lob-config.json";
	//private String sqlStmt;
	private String lobFile="";
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
	
	private void readConfigFile(String _config) throws IOException {
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
	    lobType  = (String)map.get("lobType");
	    action   = (String)map.get("action");
	    column   = (String)map.get("column");
	    table    = (String)map.get("table");
	    where    = (String)map.get("where");
	    lobFile  = (String)map.get("lobFile");	    
	    
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

        int sizeWritten = 0;
        int precentageShown = 0;
        
		if (rs.next()) {
			int filesize =  rs.getInt(2);
			System.out.println("\tFile size = " + String.format("%,d bytes", filesize));
		
			if (colTypeName.equals("CLOB")) {
				Clob clob = rs.getClob(1);
				if (clob != null) {
		          File file = new File(lobFile);
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
		          File file = new File(lobFile);
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
		String sqlStmt = "UPDATE " + table + "SET " + column + "=? WHERE " + where;
		
		PreparedStatement ps = conn.prepareStatement(sqlStmt);
		System.out.println("\tlobType is " + lobType);
		System.out.println("\tlobFile is " + lobFile);
		System.out.println("\tlobFile length is " + String.format("%,d bytes", new File(lobFile).length()));
		
		if (lobType.equals("CLOB")) {
			BufferedReader reader = new BufferedReader(new FileReader(lobFile));
			ps.setClob(1, reader);
			ps.executeUpdate();
			reader.close();
			ps.close();
			conn.close();
		}
		
		if (lobType.equals("BLOB")) {
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
				.desc("CLOB or BLOB. It is used for UPDATE statement only").build());
		options.addOption(Option.builder("column").hasArg().required(false)
				.desc("LOB column name").build());
		options.addOption(Option.builder("table").hasArg().required(false)
				.desc("target table or view name").build());
		options.addOption(Option.builder("where").hasArg().required(false)
				.desc("where clause to identify this LOB value").build());
		options.addOption(Option.builder("lobFile").hasArg().required(false)
				.desc("full file path of the output file for SELECT stmt; input file for UPDATE stmt").build());
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
		
		if (cmd.hasOption("jdbcUrl")) jdbcUrl   = cmd.getOptionValue("jdbcUrl").trim();
		if (cmd.hasOption("user")) user         = cmd.getOptionValue("user").trim();
		if (cmd.hasOption("password")) password = cmd.getOptionValue("password").trim();
		if (cmd.hasOption("action")) action     = cmd.getOptionValue("action").trim();		
		if (cmd.hasOption("lobType")) lobType   = cmd.getOptionValue("lobType").trim();
		if (cmd.hasOption("lobFile")) lobFile   = cmd.getOptionValue("lobFile").trim();
		if (cmd.hasOption("column")) lobFile    = cmd.getOptionValue("column").trim();
		if (cmd.hasOption("table")) lobFile     = cmd.getOptionValue("table").trim();
		if (cmd.hasOption("where")) lobFile     = cmd.getOptionValue("where").trim();
		
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
			lobFile == null  || lobFile.isEmpty()   ||
			column == null   || column.isEmpty()    ||
			table == null    || table.isEmpty()) {
    		System.out.println("One or more missing parameter: jdbcUrl, user, password, action, column, table, lobFile");
    		return false;
    	}
    	return true;
    }
    		
	
	private void showHelp() {
		HelpFormatter help = new HelpFormatter();
		help.setWidth(100);
		help.printHelp("LOB", options);		
	}
}	
