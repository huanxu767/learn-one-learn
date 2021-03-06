import org.apache.kudu.ColumnSchema;
import org.apache.kudu.Schema;
import org.apache.kudu.Type;
import org.apache.kudu.client.*;
import org.apache.kudu.shaded.com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This example creates a 'movies' table for movie information.
 */
public class AddSingleRecord {
  private static final Logger LOG = LoggerFactory.getLogger(AddSingleRecord.class);

//	private static final String KUDU_MASTER = System.getProperty("kuduMasters", "dev-dw1:7051,dev-dw2:7051,dev-dw3:7051");
	public static final String KUDU_MASTER = "192.168.80.10:7051";


	public static void main(String[] args) throws KuduException {
		String tableName = "movie";

//		String tableName = "impala::default.my_first_table";
		KuduClient client = new KuduClient.KuduClientBuilder(KUDU_MASTER).defaultOperationTimeoutMs(60000)
				.defaultSocketReadTimeoutMs(60000)
				.defaultAdminOperationTimeoutMs(60000)
				.build();
		dropTable(client,tableName);
//
		try {
			if (!client.tableExists(tableName)) {
				create(client,tableName);
			}
			populateSingleRow(client);
//			queryData(client);
		} finally {
			client.shutdown();
		}
  }
  
  private static void create(KuduClient client,String tableName) throws KuduException {

	  	LOG.info("in create");
	    // Create columns for the table.
	    ColumnSchema movieId = new ColumnSchema.ColumnSchemaBuilder("movie_id", Type.INT32).key(true).build();
	    ColumnSchema movieName = new ColumnSchema.ColumnSchemaBuilder("movie_name", Type.STRING).build();
	    ColumnSchema movieYear = new ColumnSchema.ColumnSchemaBuilder("movie_year", Type.STRING).build();

	    // The movie_genre is part of primary key so can do range partition on it.
	    ColumnSchema movieGenre = new ColumnSchema.ColumnSchemaBuilder("movie_genre", Type.STRING).build();

	    List<ColumnSchema> columns = Stream.of(movieId, movieName, movieYear, movieGenre).collect(Collectors.toList());

	    // Create a schema from the list of columns.
	    Schema schema = new Schema(columns);

	    // Specify hash partitioning over the movie_id column with 4 buckets.
	    CreateTableOptions createOptions =
	        new CreateTableOptions().addHashPartitions(ImmutableList.of("movie_id"), 4);

	    // Create the table.
	    client.createTable(tableName, schema, createOptions);

	    LOG.info("Table '{}' created", tableName);
	    
	    //get schema for Table
	    Schema movieSchema = client.openTable("movie").getSchema();
	    LOG.info("Number of columns in table " + movieSchema.getColumnCount());
	   
	    for (ColumnSchema colSchema : movieSchema.getColumns()) {
	    	LOG.info("Columns in table " + colSchema.getName() + "is primary key " + colSchema.isKey());
	    }
  }
  
  private static void populateSingleRow(KuduClient client) throws KuduException {
	  
	  KuduSession session = client.newSession();
	  session.setFlushMode(SessionConfiguration.FlushMode.MANUAL_FLUSH);
	  session.setMutationBufferSpace(20000);

//	  session.setFlushMode(SessionConfiguration.FlushMode.AUTO_FLUSH_SYNC);
//	  session.setFlushMode(SessionConfiguration.FlushMode.AUTO_FLUSH_BACKGROUND);

	  long flushIndex = 0;
	  long t1 = System.currentTimeMillis();
	  for (int i = 1; i < 100000; i++) {
		  KuduTable table = client.openTable("movie");
		  flushIndex++;
//		  System.out.println("--------------------" + i);
		  Insert insert = table.newInsert();
		  PartialRow row = insert.getRow();
		  row.addInt(0, i);
		  row.addString(1, "Star Wars Force Awakens ");
		  row.addString(2, "2016");
		  row.addString(3,  "Sci-Fi");
		  session.apply(insert);
		  if( flushIndex > 15000){
			  System.out.println("1——flush");
			  flushIndex = 0;
			  session.flush();
		  }
	  }
	  System.out.println("end——flush");
	  session.flush();
	  long t2 = System.currentTimeMillis();

	  System.out.println("-------" + (t2-t1)/1000);
	  session.close();
	  LOG.info("added one record" );
	  
  }

	private static void populateSingleRow2(KuduClient client) throws KuduException {

		long flushIndex = 0;
		long t1 = System.currentTimeMillis();
		for (int i = 1; i < 10000; i++) {

			KuduSession session = client.newSession();
//	  session.setFlushMode(SessionConfiguration.FlushMode.MANUAL_FLUSH);
			session.setMutationBufferSpace(800);

//	  session.setFlushMode(SessionConfiguration.FlushMode.AUTO_FLUSH_SYNC);
			session.setFlushMode(SessionConfiguration.FlushMode.AUTO_FLUSH_BACKGROUND);

			KuduTable table = client.openTable("movie");
			flushIndex++;
			System.out.println("--------------------" + i);
			Insert insert = table.newInsert();
			PartialRow row = insert.getRow();
			row.addInt(0, i);
			row.addString(1, "Star Wars Force Awakens ");
			row.addString(2, "2016");
			row.addString(3,  "Sci-Fi");
			session.apply(insert);
//		  if( flushIndex > 1){
//			  System.out.println("flush");
//			  flushIndex = 0;
//			  session.flush();
//		  }

			long t2 = System.currentTimeMillis();

			System.out.println("-------" + (t2-t1)/1000);
			session.close();
			LOG.info("added one record" );
		}
	}


  private static void queryData(KuduClient client) throws KuduException {
	  

	  KuduTable table = client.openTable("movie");

	  KuduScanner kuduScanner = client.newScannerBuilder(table).build();
	  while (kuduScanner.hasMoreRows()) {
		  RowResultIterator rows = kuduScanner.nextRows();
		  while (rows.hasNext()) {
			  RowResult row = rows.next();
			  LOG.info("row value " + row.rowToString());
		  }
			  
	  }
	  
  }

	/**
	 * 删除表
	 * @param client
	 * @param tableName
	 */
	public static void dropTable(KuduClient client, String tableName) {
		try {
			if(client.tableExists(tableName)) {
				client.deleteTable(tableName);
			}
		} catch (KuduException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 列出Kudu下所有的表
	 * @param client
	 */
	public static void tableList(KuduClient client) {
		try {
			ListTablesResponse listTablesResponse = client.getTablesList();
			List<String> tblist = listTablesResponse.getTablesList();
			for(String tableName : tblist) {
				System.out.println(tableName);
			}
		} catch (KuduException e) {
			e.printStackTrace();
		}
	}

  
}
