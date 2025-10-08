DECLARE
    qschm          varchar2(100);
    qname          varchar2(100);
    qid            number;
    qtid           number;
    dqltid         number;
    evtid          number;
    partbytes      number;
    partblocks     number;
    partextents    number;
    totPartBytes   number;
    totPartblocks  number;
    totPartextents number;
    q_cursor       integer ;
    q_cursor1      integer ;
    sqlstmt        varchar2(500);
    sqlstmt1       varchar2(500);
    qdummy         integer ;
    qdummy1        integer ;
    partitionName  varchar2(30);
    partCnt        number;
    partTypes      number;

BEGIN

    sqlstmt1 :=
            'SELECT name FROM user_queues WHERE sharded=''TRUE'' AND name IN (''ACTIONUPDATESTOPIC'',''TRANSACTIONSTATEMENTTOPIC'',''PAYMENTUPDATESTOPIC'')';
    q_cursor1 := dbms_sql.open_cursor;
    dbms_sql.parse(q_cursor1, sqlstmt1, dbms_sql.v7);
    dbms_sql.DEFINE_COLUMN(q_cursor1, 1, qname, 100);
    qdummy1 := dbms_sql.execute(q_cursor1);
    LOOP
        IF (dbms_sql.fetch_rows(q_cursor1) = 0) THEN
            EXIT;
        END IF;
        dbms_sql.column_value(q_cursor1, 1, qname);

        totPartBytes := 0;
        totPartblocks := 0;
        totPartextents := 0;

        qschm := 'TXEVENTQ_ADMIN';


        SELECT QID INTO qid FROM user_queues WHERE name = qname;
        SELECT QTID INTO qtid FROM user_queue_tables WHERE queue_table = qname;
        SELECT object_id
        INTO dqltid
        FROM user_objects
        WHERE object_name = 'AQ$_' || qname || '_L'
          AND object_type = 'TABLE';
        SELECT object_id
        INTO evtid
        FROM user_objects
        WHERE object_name = 'AQ$_' || qname || '_X'
          AND object_type = 'TABLE';

        dbms_output.put_line('*******************************************************************************');
        dbms_output.put_line(qschm || '.' || qname);
        dbms_output.put_line('===============================================================================');
        dbms_output.put_line(rpad('table type', 14) || lpad(' ', 14) || lpad('size (MB)', 14) || lpad('blocks', 14) ||
                             lpad('extents', 14));
        dbms_output.put_line('-------------------------------------------------------------------------------');

        partbytes := 0;
        partblocks := 0;
        partextents := 0;
        SELECT sum(bytes), sum(blocks), sum(extents)
        INTO partbytes, partblocks, partextents
        FROM user_segments
        WHERE segment_type = 'TABLE PARTITION'
          AND segment_name = qname;
        dbms_output.put_line(rpad('QT', 14) || lpad(' ', 14) || lpad((TRUNC(partbytes / 1024 / 1024) || ''), 14) ||
                             lpad((partblocks || ''), 14) || lpad((partextents || ''), 14));
        -- dbms_output.put_line(rpad('QT',14) || lpad((TRUNC(partbytes/1024/1024) || ' MB'), 14) || rpad(' blocks', 8) || lpad((partblocks || ' '), 14) || rpad('extents',10) || lpad((partextents || ' '),10));
        totPartBytes := totPartBytes + partbytes;
        totPartblocks := totPartblocks + partblocks;
        totPartextents := totPartextents + partextents;

        partbytes := 0;
        partblocks := 0;
        partextents := 0;
        SELECT sum(bytes), sum(blocks), sum(extents)
        INTO partbytes, partblocks, partextents
        FROM user_segments
        WHERE segment_type = 'INDEX PARTITION'
          AND segment_name = 'qt' || qtid || '_sqno_idx';
        dbms_output.put_line(rpad('QT Index', 14) || lpad(' ', 14) ||
                             lpad((TRUNC(partbytes / 1024 / 1024) || ''), 14) || lpad((partblocks || ''), 14) ||
                             lpad((partextents || ''), 14));
        --dbms_output.put_line(rpad('QT Index',14) || TRUNC(partbytes/1024/1024) || ' MB' || ', blocks - ' || partblocks || ', extents - ' || partextents);
        totPartBytes := totPartBytes + partbytes;
        totPartblocks := totPartblocks + partblocks;
        totPartextents := totPartextents + partextents;

        partbytes := 0;
        partblocks := 0;
        partextents := 0;
        SELECT sum(bytes), sum(blocks), sum(extents)
        INTO partbytes, partblocks, partextents
        FROM user_segments
        WHERE segment_type = 'LOB PARTITION'
          AND segment_name LIKE '%SYS_LOB%' || qtid || '%';
        dbms_output.put_line(rpad('QT LOB', 14) || lpad(' ', 14) || lpad((TRUNC(partbytes / 1024 / 1024) || ''), 14) ||
                             lpad((partblocks || ''), 14) || lpad((partextents || ''), 14));
        --dbms_output.put_line('QT LOB - ' || TRUNC(partbytes/1024/1024) || ' MB' || ', blocks - ' || partblocks || ', extents - ' || partextents);
        totPartBytes := totPartBytes + partbytes;
        totPartblocks := totPartblocks + partblocks;
        totPartextents := totPartextents + partextents;

        partbytes := 0;
        partblocks := 0;
        partextents := 0;
        SELECT sum(bytes), sum(blocks), sum(extents)
        INTO partbytes, partblocks, partextents
        FROM user_segments
        WHERE segment_type = 'INDEX PARTITION'
          AND segment_name LIKE '%SYS_IL%' || qtid || '%';
        dbms_output.put_line(rpad('QT LOB Index', 14) || lpad(' ', 14) ||
                             lpad((TRUNC(partbytes / 1024 / 1024) || ''), 14) || lpad((partblocks || ''), 14) ||
                             lpad((partextents || ''), 14));
        --dbms_output.put_line('QT LOB Index - ' || TRUNC(partbytes/1024/1024) || ' MB' || ', blocks - ' || partblocks || ', extents - ' || partextents);
        totPartBytes := totPartBytes + partbytes;
        totPartblocks := totPartblocks + partblocks;
        totPartextents := totPartextents + partextents;

        partbytes := 0;
        partblocks := 0;
        partextents := 0;
        SELECT sum(bytes), sum(blocks), sum(extents)
        INTO partbytes, partblocks, partextents
        FROM user_segments
        WHERE segment_type = 'TABLE PARTITION'
          AND segment_name = 'AQ$_' || qname || '_L';
        dbms_output.put_line(rpad('DL (_L)', 14) || lpad(' ', 14) || lpad((TRUNC(partbytes / 1024 / 1024) || ''), 14) ||
                             lpad((partblocks || ''), 14) || lpad((partextents || ''), 14));
--  dbms_output.put_line('DL - ' || TRUNC(partbytes/1024/1024) || ' MB' || ', blocks - ' || partblocks || ', extents - ' || partextents);
        totPartBytes := totPartBytes + partbytes;
        totPartblocks := totPartblocks + partblocks;
        totPartextents := totPartextents + partextents;

        partbytes := 0;
        partblocks := 0;
        partextents := 0;
        SELECT sum(bytes), sum(blocks), sum(extents)
        INTO partbytes, partblocks, partextents
        FROM user_segments
        WHERE segment_type = 'INDEX PARTITION'
          AND segment_name = 'dql' || dqltid || '_sqno_idx';
        dbms_output.put_line(rpad('DL Index (_L)', 14) || lpad(' ', 14) ||
                             lpad((TRUNC(partbytes / 1024 / 1024) || ''), 14) || lpad((partblocks || ''), 14) ||
                             lpad((partextents || ''), 14));
--  dbms_output.put_line('DL Index - ' || TRUNC(partbytes/1024/1024) || ' MB' || ', blocks - ' || partblocks || ', extents - ' || partextents);
        totPartBytes := totPartBytes + partbytes;
        totPartblocks := totPartblocks + partblocks;
        totPartextents := totPartextents + partextents;

        partbytes := 0;
        partblocks := 0;
        partextents := 0;
        SELECT sum(bytes), sum(blocks), sum(extents)
        INTO partbytes, partblocks, partextents
        FROM user_segments
        WHERE segment_type = 'TABLE PARTITION'
          AND segment_name = 'AQ$_' || qname || '_T';
        dbms_output.put_line(rpad('TM (_T)', 14) || lpad(' ', 14) || lpad((TRUNC(partbytes / 1024 / 1024) || ''), 14) ||
                             lpad((partblocks || ''), 14) || lpad((partextents || ''), 14));
--  dbms_output.put_line('TM - ' || TRUNC(partbytes/1024/1024) || ' MB' || ', blocks - ' || partblocks || ', extents - ' || partextents);
        totPartBytes := totPartBytes + partbytes;
        totPartblocks := totPartblocks + partblocks;
        totPartextents := totPartextents + partextents;

        partbytes := 0;
        partblocks := 0;
        partextents := 0;
        SELECT sum(bytes), sum(blocks), sum(extents)
        INTO partbytes, partblocks, partextents
        FROM user_segments
        WHERE segment_type = 'INDEX PARTITION'
          AND segment_name LIKE '%' || qname || '_T%';
        dbms_output.put_line(rpad('TM Index (_T)', 14) || lpad(' ', 14) ||
                             lpad((TRUNC(partbytes / 1024 / 1024) || ''), 14) || lpad((partblocks || ''), 14) ||
                             lpad((partextents || ''), 14));
--  dbms_output.put_line('TM Index - ' || TRUNC(partbytes/1024/1024) || ' MB' || ', blocks - ' || partblocks || ', extents - ' || partextents);
        totPartBytes := totPartBytes + partbytes;
        totPartblocks := totPartblocks + partblocks;
        totPartextents := totPartextents + partextents;


        partbytes := 0;
        partblocks := 0;
        partextents := 0;
        SELECT sum(bytes), sum(blocks), sum(extents)
        INTO partbytes, partblocks, partextents
        FROM user_segments
        WHERE segment_type = 'TABLE PARTITION'
          AND segment_name = 'AQ$_' || qname || '_X';
        dbms_output.put_line(rpad('EV (_X)', 14) || lpad(' ', 14) || lpad((TRUNC(partbytes / 1024 / 1024) || ''), 14) ||
                             lpad((partblocks || ''), 14) || lpad((partextents || ''), 14));
--  dbms_output.put_line('EV - ' || TRUNC(partbytes/1024/1024) || ' MB' || ', blocks - ' || partblocks || ', extents - ' || partextents);
        totPartBytes := totPartBytes + partbytes;
        totPartblocks := totPartblocks + partblocks;
        totPartextents := totPartextents + partextents;

        partbytes := 0;
        partblocks := 0;
        partextents := 0;
        SELECT sum(bytes), sum(blocks), sum(extents)
        INTO partbytes, partblocks, partextents
        FROM user_segments
        WHERE segment_type = 'LOB PARTITION'
          AND segment_name LIKE '%SYS_LOB%' || evtid || '%';
        dbms_output.put_line(rpad('EV LOB (_X)', 14) || lpad(' ', 14) ||
                             lpad((TRUNC(partbytes / 1024 / 1024) || ''), 14) || lpad((partblocks || ''), 14) ||
                             lpad((partextents || ''), 14));
--  dbms_output.put_line('EV LOB - ' || TRUNC(partbytes/1024/1024) || ' MB' || ', blocks - ' || partblocks || ', extents - ' || partextents);
        totPartBytes := totPartBytes + partbytes;
        totPartblocks := totPartblocks + partblocks;
        totPartextents := totPartextents + partextents;

        dbms_output.put_line('-------------------------------------------------------------------------------');
        dbms_output.put_line(rpad('Total size', 14) || lpad(' ', 14) ||
                             lpad((TRUNC(totPartbytes / 1024 / 1024) || ''), 14) || lpad((totPartblocks || ''), 14) ||
                             lpad((totPartextents || ''), 14));
--  dbms_output.put_line('Total size - ' || TRUNC(totPartBytes/1024/1024) || ' MB' || ', blocks - ' || totPartblocks || ', extents - ' || totPartextents);
        dbms_output.put_line('===============================================================================');
        dbms_output.put_line(rpad('part type', 14) || lpad('PartCount', 14) || lpad('size (MB)', 14) ||
                             lpad('blocks', 14) || lpad('extents', 14));
        dbms_output.put_line('-------------------------------------------------------------------------------');


        partTypes := 6;

        FOR partType IN 1 .. partTypes
            LOOP
                totPartbytes := 0;
                totPartblocks := 0;
                totPartextents := 0;
                partCnt := 0;

                IF partType = 1 THEN
                    sqlstmt := 'SELECT partname FROM user_queue_partition_map WHERE queue_table_id = ' || qtid ||
                               ' AND subshard <> -1';
                ELSIF partType = 2 THEN
                    sqlstmt := 'SELECT partname FROM user_queue_partition_map WHERE queue_table_id = ' || qtid ||
                               ' AND subshard = -1';
                ELSIF partType = 3 THEN
                    sqlstmt := 'SELECT PARTITION_NAME FROM USER_TAB_PARTITIONS WHERE TABLE_NAME = ''' || qname ||
                               ''' AND PARTITION_NAME NOT IN (SELECT partname FROM user_queue_partition_map WHERE queue_table_id = ' ||
                               qtid || ')';
                ELSIF partType = 4 THEN
                    sqlstmt := 'SELECT partname FROM user_dequeue_log_partition_map WHERE queue_table_id = ' || qtid ||
                               ' AND QUEUE_PART# <> -1';
                ELSIF partType = 5 THEN
                    sqlstmt := 'SELECT partname FROM user_dequeue_log_partition_map WHERE queue_table_id = ' || qtid ||
                               ' AND QUEUE_PART# = -1';
                ELSIF partType = 6 THEN
                    sqlstmt := 'SELECT PARTITION_NAME FROM USER_TAB_PARTITIONS WHERE TABLE_NAME = ''AQ$_' || qname ||
                               '_L'' AND PARTITION_NAME NOT IN (SELECT partname FROM user_dequeue_log_partition_map WHERE queue_table_id = ' ||
                               qtid || ')';
                ELSE
                    EXIT;
                END IF;

--    dbms_output.put_line(sqlstmt);

                q_cursor := dbms_sql.open_cursor;
                dbms_sql.parse(q_cursor, sqlstmt, dbms_sql.v7);
                dbms_sql.DEFINE_COLUMN(q_cursor, 1, partitionName, 30);
                qdummy := dbms_sql.execute(q_cursor);
                LOOP
                    IF (dbms_sql.fetch_rows(q_cursor) = 0) THEN
                        EXIT;
                    END IF;
                    dbms_sql.column_value(q_cursor, 1, partitionName);

                    partbytes := 0;
                    partblocks := 0;
                    partextents := 0;

                    IF partType >= 1 AND partType <= 3 THEN
                        SELECT SUM(bytes), SUM(blocks), SUM(extents)
                        INTO partbytes, partblocks, partextents
                        FROM ((SELECT bytes, blocks, extents
                               FROM user_segments
                               WHERE segment_type = 'TABLE PARTITION'
                                 AND segment_name = qname
                                 AND partition_name = partitionName)
                              UNION ALL
                              (SELECT s.bytes bytes, s.blocks blocks, s.extents extents
                               FROM user_segments s,
                                    user_lob_partitions lp
                               WHERE lp.table_name = qname
                                 AND s.segment_type = 'LOB PARTITION'
                                 AND s.segment_name LIKE '%SYS_LOB%' || qtid || '%'
                                 AND lp.partition_name = partitionName
                                 AND lp.LOB_PARTITION_NAME = s.PARTITION_NAME)
                              UNION ALL
                              (SELECT s.bytes bytes, s.blocks blocks, s.extents extents
                               FROM user_segments s,
                                    user_lob_partitions lp
                               WHERE lp.table_name = qname
                                 AND s.segment_type = 'INDEX PARTITION'
                                 AND s.segment_name LIKE '%SYS_IL%' || qtid || '%'
                                 AND lp.partition_name = partitionName
                                 AND lp.LOB_INDPART_NAME = s.PARTITION_NAME)
                              UNION ALL
                              (SELECT bytes, blocks, extents
                               FROM user_segments
                               WHERE segment_type = 'INDEX PARTITION'
                                 AND segment_name = 'qt' || qtid || '_sqno_idx'
                                 AND partition_name = partitionName));

                    ELSIF partType >= 4 AND partType <= 6 THEN
                        SELECT SUM(bytes), SUM(blocks), SUM(extents)
                        INTO partbytes, partblocks, partextents
                        FROM ((SELECT bytes, blocks, extents
                               FROM user_segments
                               WHERE segment_type = 'TABLE PARTITION'
                                 AND segment_name = 'AQ$_' || qname || '_L'
                                 AND partition_name = partitionName)
                              UNION ALL
                              (SELECT bytes, blocks, extents
                               FROM user_segments
                               WHERE segment_type = 'INDEX PARTITION'
                                 AND segment_name = 'dql' || dqltid || '_sqno_idx'
                                 AND partition_name = partitionName));
                    END IF;

                    totPartBytes := totPartBytes + partbytes;
                    totPartblocks := totPartblocks + partblocks;
                    totPartextents := totPartextents + partextents;

                    partCnt := partCnt + 1;
                END LOOP;
                dbms_sql.close_cursor(q_cursor);

                IF partType = 1 THEN
                    dbms_output.put_line(rpad('Mapped QT', 14) || lpad(partCnt || '', 14) ||
                                         lpad(TRUNC(totPartBytes / 1024 / 1024) || '', 14) ||
                                         lpad(totPartblocks || '', 14) || lpad(totPartextents || '', 14));
                ELSIF partType = 2 THEN
                    dbms_output.put_line(rpad('Unmapped QT', 14) || lpad(partCnt || '', 14) ||
                                         lpad(TRUNC(totPartBytes / 1024 / 1024) || '', 14) ||
                                         lpad(totPartblocks || '', 14) || lpad(totPartextents || '', 14));
                ELSIF partType = 3 THEN
                    dbms_output.put_line(rpad('Missing QT', 14) || lpad(partCnt || '', 14) ||
                                         lpad(TRUNC(totPartBytes / 1024 / 1024) || '', 14) ||
                                         lpad(totPartblocks || '', 14) || lpad(totPartextents || '', 14));
                ELSIF partType = 4 THEN
                    dbms_output.put_line(rpad('Mapped DL', 14) || lpad(partCnt || '', 14) ||
                                         lpad(TRUNC(totPartBytes / 1024 / 1024) || '', 14) ||
                                         lpad(totPartblocks || '', 14) || lpad(totPartextents || '', 14));
                ELSIF partType = 5 THEN
                    dbms_output.put_line(rpad('Unmapped DL', 14) || lpad(partCnt || '', 14) ||
                                         lpad(TRUNC(totPartBytes / 1024 / 1024) || '', 14) ||
                                         lpad(totPartblocks || '', 14) || lpad(totPartextents || '', 14));
                ELSIF partType = 6 THEN
                    dbms_output.put_line(rpad('Missing DL', 14) || lpad(partCnt || '', 14) ||
                                         lpad(TRUNC(totPartBytes / 1024 / 1024) || '', 14) ||
                                         lpad(totPartblocks || '', 14) || lpad(totPartextents || '', 14));
                ELSE
                    EXIT;
                END IF;

            END LOOP;
        dbms_output.put_line('===============================================================================');
    END LOOP;
    dbms_sql.close_cursor(q_cursor1);

    DBMS_OUTPUT.PUT_LINE('================================================================');
    DBMS_OUTPUT.PUT_LINE('QT              = Queue Table - main message storage');
    DBMS_OUTPUT.PUT_LINE('QT Index        = Queue Table Index');
    DBMS_OUTPUT.PUT_LINE('QT LOB          = Queue Table LOB - large payloads');
    DBMS_OUTPUT.PUT_LINE('QT LOB Index    = Queue Table LOB Index');
    DBMS_OUTPUT.PUT_LINE('DL (_L)         = Dequeue Log - tracks consumed messages');
    DBMS_OUTPUT.PUT_LINE('DL Index (_L)   = Dequeue Log Index');
    DBMS_OUTPUT.PUT_LINE('TM (_T)         = Time Manager - scheduling and expiration');
    DBMS_OUTPUT.PUT_LINE('TM Index (_T)   = Time Manager Index');
    DBMS_OUTPUT.PUT_LINE('EV (_X)         = Eviction - memory-evicted records');
    DBMS_OUTPUT.PUT_LINE('EV LOB (_X)     = Eviction LOB');
    DBMS_OUTPUT.PUT_LINE('================================================================');
END;

