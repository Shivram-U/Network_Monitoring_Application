import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder;
import com.datastax.oss.driver.api.core.type.DataTypes;

import java.nio.*;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.UUID;

public class CassandraConnection {

public static void main(String[] args) {
    String serverIp = "166.78.10.41";
    String keyspace = "gamma";
    CassandraConnection connection;

    Cluster cluster = Cluster.builder()
            .addContactPoints(serverIp)
            .build();

    Session session = cluster.connect(keyspace);


    String cqlStatement = "SELECT * FROM TestCF";
    for (Row row : session.execute(cqlStatement)) {
        System.out.println(row.toString());
    }

}
}