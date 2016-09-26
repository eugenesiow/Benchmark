package org.insight_centre.aceis.io.streams.sparql2stream;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

public class SensorRepositoryDb {
	private static final String tableName = "SensorRepository";
	
	public static void main(String[] args) {
		run("dataset/"+tableName+".n3","./dataset/CityBench");
	}
	
	public static void run(String srcPath, String dbPath) {
		try {
			Class.forName("org.h2.Driver");
			Connection conn = DriverManager.getConnection("jdbc:h2:"+dbPath, "sa", "");
			createTable(conn);
			ResultSet results = retriveDataFromRdf(srcPath);
			insertToTable(conn, results);
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static void insertToTable(Connection conn, ResultSet results) throws SQLException {
		PreparedStatement prep = conn.prepareStatement("INSERT INTO "+tableName+" (propId,type,lat,lon) VALUES (?,?,?,?)");
		
		conn.setAutoCommit(false);
		while(results.hasNext()) {
			QuerySolution soln = results.nextSolution() ;
			prep.setString(1, soln.getResource("prop").getURI().replace("http://www.insight-centre.org/dataset/SampleEventService#", ""));
			prep.setString(2, soln.getResource("type").getURI().replace("http://www.insight-centre.org/citytraffic#", ""));
			prep.setDouble(3, soln.getLiteral("lat").getDouble());
			prep.setDouble(4, soln.getLiteral("lon").getDouble());
			prep.addBatch();
		}	
		prep.executeBatch();
		conn.setAutoCommit(true);
	}

	private static void createTable(Connection conn) throws SQLException {
		conn.createStatement().executeUpdate("DROP TABLE "+tableName);
		Statement stat = conn.createStatement();
		stat.execute("CREATE TABLE "+tableName+"\n" + 
			"(\n" + 
			"  propId VARCHAR,\n" + 
			"  type VARCHAR,\n" +
			"  lat DOUBLE,\n" +
			"  lon DOUBLE\n" + 
			")\n" + 
			";");
	}

	private static ResultSet retriveDataFromRdf(String srcPath) {
		Model model = ModelFactory.createDefaultModel();
		InputStream in = FileManager.get().open(srcPath);
		if (in == null) {
		    throw new IllegalArgumentException(
		                                 "File: " + srcPath + " not found");
		}
		model.read(in, null, "N3");
		String queryString = "PREFIX sao: <http://purl.oclc.org/NET/sao/>\n" + 
				"PREFIX ssn: <http://purl.oclc.org/NET/ssnx/ssn#>\n" + 
				"PREFIX ct: 	<http://www.insight-centre.org/citytraffic#>\n" + 
				"\n" + 
				"SELECT ?prop ?type ?lat ?lon\n" + 
				"WHERE {\n" + 
				"	?prop a ?type;\n" + 
				"		ssn:isPropertyOf ?propNode.\n" + 
				"	?propNode ct:hasStartLatitude ?lat;\n" + 
				"		ct:hasStartLongitude ?lon.\n" + 
				"}" ;
		QueryExecution qexec = QueryExecutionFactory.create(queryString,model);
		return qexec.execSelect();
	}
}
