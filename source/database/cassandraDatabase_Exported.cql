
CREATE KEYSPACE nmtarchive WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '3'}  AND durable_writes = true;

CREATE TABLE nmtarchive.networkdevicedata (
    archive_id uuid PRIMARY KEY,
    archive_timestamp timestamp,
    compression_algorithm text,
    data blob,
    metadata text,
    serialization text
) WITH bloom_filter_fp_chance = 0.01
    AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
    AND comment = ''
    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}
    AND compression = {'chunk_length_in_kb': '64', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
    AND crc_check_chance = 1.0
    AND dclocal_read_repair_chance = 0.1
    AND default_time_to_live = 0
    AND gc_grace_seconds = 864000
    AND max_index_interval = 2048
    AND memtable_flush_period_in_ms = 0
    AND min_index_interval = 128
    AND read_repair_chance = 0.0
    AND speculative_retry = '99PERCENTILE';

CREATE TABLE nmtarchive.networkinterfacemetricsarchive (
    deviceid int,
    recordtime timestamp,
    interfaceindex int,
    avgdiscards_percent double,
    avgerrors_percent double,
    avgintraffic_bps double,
    avgouttraffic_bps double,
    count bigint,
    maxdiscards_percent double,
    maxerrors_percent double,
    maxintraffic_bps double,
    maxouttraffic_bps double,
    mindiscards_percent double,
    minerrors_percent double,
    minintraffic_bps double,
    minouttraffic_bps double,
    sumdiscards_percent double,
    sumerrors_percent double,
    sumintraffic_bps double,
    sumouttraffic_bps double,
    PRIMARY KEY (deviceid, recordtime, interfaceindex)
) WITH CLUSTERING ORDER BY (recordtime ASC, interfaceindex ASC)
    AND bloom_filter_fp_chance = 0.01
    AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
    AND comment = ''
    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}
    AND compression = {'chunk_length_in_kb': '64', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
    AND crc_check_chance = 1.0
    AND dclocal_read_repair_chance = 0.1
    AND default_time_to_live = 0
    AND gc_grace_seconds = 864000
    AND max_index_interval = 2048
    AND memtable_flush_period_in_ms = 0
    AND min_index_interval = 128
    AND read_repair_chance = 0.0
    AND speculative_retry = '99PERCENTILE';

CREATE TABLE nmtarchive.networkinterfacemetricsarchive_ (
    deviceid int,
    interfaceindex int,
    recordtime timestamp,
    avgdiscards_percent double,
    avgerrors_percent double,
    avgintraffic_bps double,
    avgouttraffic_bps double,
    count bigint,
    maxdiscards_percent double,
    maxerrors_percent double,
    maxintraffic_bps double,
    maxouttraffic_bps double,
    mindiscards_percent double,
    minerrors_percent double,
    minintraffic_bps double,
    minouttraffic_bps double,
    sumdiscards_percent double,
    sumerrors_percent double,
    sumintraffic_bps double,
    sumouttraffic_bps double,
    PRIMARY KEY (deviceid, interfaceindex, recordtime)
) WITH CLUSTERING ORDER BY (interfaceindex ASC, recordtime ASC)
    AND bloom_filter_fp_chance = 0.01
    AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
    AND comment = ''
    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}
    AND compression = {'chunk_length_in_kb': '64', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
    AND crc_check_chance = 1.0
    AND dclocal_read_repair_chance = 0.1
    AND default_time_to_live = 0
    AND gc_grace_seconds = 864000
    AND max_index_interval = 2048
    AND memtable_flush_period_in_ms = 0
    AND min_index_interval = 128
    AND read_repair_chance = 0.0
    AND speculative_retry = '99PERCENTILE';

CREATE TABLE nmtarchive.networkinterfacemetricsarchive_rt (
    deviceid int,
    recordtime timestamp,
    interfaceindex int,
    avgdiscards_percent double,
    avgerrors_percent double,
    avgintraffic_bps double,
    avgouttraffic_bps double,
    count bigint,
    maxdiscards_percent double,
    maxerrors_percent double,
    maxintraffic_bps double,
    maxouttraffic_bps double,
    mindiscards_percent double,
    minerrors_percent double,
    minintraffic_bps double,
    minouttraffic_bps double,
    sumdiscards_percent double,
    sumerrors_percent double,
    sumintraffic_bps double,
    sumouttraffic_bps double,
    PRIMARY KEY (deviceid, recordtime, interfaceindex)
) WITH CLUSTERING ORDER BY (recordtime ASC, interfaceindex ASC)
    AND bloom_filter_fp_chance = 0.01
    AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
    AND comment = ''
    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}
    AND compression = {'chunk_length_in_kb': '64', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
    AND crc_check_chance = 1.0
    AND dclocal_read_repair_chance = 0.1
    AND default_time_to_live = 0
    AND gc_grace_seconds = 864000
    AND max_index_interval = 2048
    AND memtable_flush_period_in_ms = 0
    AND min_index_interval = 128
    AND read_repair_chance = 0.0
    AND speculative_retry = '99PERCENTILE';

CREATE TABLE nmtarchive.networkinterfacemetricsarchive_ifin (
    deviceid int,
    interfaceindex int,
    recordtime timestamp,
    avgdiscards_percent double,
    avgerrors_percent double,
    avgintraffic_bps double,
    avgouttraffic_bps double,
    count bigint,
    maxdiscards_percent double,
    maxerrors_percent double,
    maxintraffic_bps double,
    maxouttraffic_bps double,
    mindiscards_percent double,
    minerrors_percent double,
    minintraffic_bps double,
    minouttraffic_bps double,
    sumdiscards_percent double,
    sumerrors_percent double,
    sumintraffic_bps double,
    sumouttraffic_bps double,
    PRIMARY KEY (deviceid, interfaceindex, recordtime)
) WITH CLUSTERING ORDER BY (interfaceindex ASC, recordtime ASC)
    AND bloom_filter_fp_chance = 0.01
    AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
    AND comment = ''
    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}
    AND compression = {'chunk_length_in_kb': '64', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
    AND crc_check_chance = 1.0
    AND dclocal_read_repair_chance = 0.1
    AND default_time_to_live = 0
    AND gc_grace_seconds = 864000
    AND max_index_interval = 2048
    AND memtable_flush_period_in_ms = 0
    AND min_index_interval = 128
    AND read_repair_chance = 0.0
    AND speculative_retry = '99PERCENTILE';

