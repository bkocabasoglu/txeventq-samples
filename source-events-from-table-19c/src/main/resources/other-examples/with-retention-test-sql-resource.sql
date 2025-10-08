--STEP: 2 ------------------------------------------------------
-- Create TxEventQ in 19c
--------------------------------------------------------
DECLARE
    queue_props DBMS_AQADM.QUEUE_PROPS_T;
BEGIN
    queue_props.retention_time := 604800; -- 7 days retention
    DBMS_AQADM.CREATE_SHARDED_QUEUE(
            queue_name => 'StatementUpdatesTopic',
            multiple_consumers => TRUE,
            queue_properties => queue_props
    );

    DBMS_AQADM.set_queue_parameter('StatementUpdatesTopic', 'SHARD_NUM', 100);

    -- Start the queue
    DBMS_AQADM.start_queue(queue_name=>'StatementUpdatesTopic');
END;