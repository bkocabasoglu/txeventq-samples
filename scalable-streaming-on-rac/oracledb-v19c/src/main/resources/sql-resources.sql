/*
====================================================================
    Oracle 19c TxEventQ Demo: Claim Updates Event Streaming
  
    This script demonstrates how to set up an event-driven architecture
    using Oracle TxEventQ for claim updates.
    It covers table creation, queue setup, triggers, enqueue/dequeue logic,
    and monitoring scripts. Suitable for POC, blog posts, and learning.

    Prerequisites:
        - Oracle 19c or later
        - User with privileges to execute DBMS_AQ, DBMS_AQADM, DBMS_AQIN, DBMS_AQJMS
        - Replace TXEVENTQ_ADMIN with your TxEventQ user if needed

    Steps:
        1. Create table for claim events
        2. Create sharded event queue
        3. Create enqueue handler procedure
        4. Create trigger to call handler
        5. Add default subscriber
        6. Create dequeue procedure
        7. Create sequence for claim IDs
        8. Insert test messages (optional)
        9. Dequeue messages (optional)
        10. Monitoring queries

====================================================================
*/

-- === Permissions Required ===

-- Make sure the following grants are in place for your TxEventQ admin user:
-- GRANT EXECUTE ON dbms_aq TO TXEVENTQ_ADMIN;
-- GRANT EXECUTE ON dbms_aqadm TO TXEVENTQ_ADMIN;
-- GRANT EXECUTE ON dbms_aqin TO TXEVENTQ_ADMIN;
-- GRANT EXECUTE ON dbms_aqjms TO TXEVENTQ_ADMIN;

-- =============================================================
-- STEP 1: Create table for claim events to be inserted or updated
-- =============================================================

CREATE TABLE ClaimUpdatesTable(
    Id RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,  -- Unique event ID
    ClaimId VARCHAR(100),                       -- Claim identifier
    StatusUpdateNotes VARCHAR(500),             -- Notes about status update
    Timestamp NUMBER(13,0)                      -- Epoch time in ms
);

-- =============================================================
-- STEP 2: Create sequence for claim IDs
-- =============================================================

CREATE SEQUENCE  "AQ"."CLAIM_ID_SEQ"  
    MINVALUE 1 
    MAXVALUE 9999999999999999999999999999 
    INCREMENT BY 1 
    START WITH 11111 
    CACHE 20 
    NOORDER  
    NOCYCLE  
    NOKEEP  
    GLOBAL ;

-- =============================================================
-- STEP 3: Create sharded event queue (TxEventQ)
-- =============================================================
BEGIN
    DBMS_AQADM.CREATE_SHARDED_QUEUE(
        queue_name         => 'ClaimUpdatesTopic',
        multiple_consumers => TRUE      -- Needed if an event will be consumed by multiple consumers
    );
    -- Set number of shards for parallelism
    DBMS_AQADM.set_queue_parameter('ClaimUpdatesTopic', 'SHARD_NUM', 6);
    -- Ensure event order per correlation key (e.g., ClaimId)
    DBMS_AQADM.set_queue_parameter('ClaimUpdatesTopic', 'KEY_BASED_ENQUEUE', 1);
    -- Guarantee order at consumer
    DBMS_AQADM.set_queue_parameter('ClaimUpdatesTopic', 'STICKY_DEQUEUE', 1);
    -- Start the queue
    DBMS_AQADM.start_queue(queue_name=>'ClaimUpdatesTopic');
END;


-- =============================================================
-- STEP 4: Create enqueue handler procedure
-- This procedure is called by the trigger to enqueue claim update events
-- =============================================================

CREATE OR REPLACE PROCEDURE ClaimUpdatesTriggerHandler (
    p_StatusUpdateNotes IN VARCHAR2,
    p_ClaimId IN VARCHAR2,
    p_operation_type IN VARCHAR2,
    p_OldStatusUpdateNotes IN VARCHAR2
)
AS
    message SYS.AQ$_JMS_TEXT_MESSAGE;
    enqueue_options DBMS_AQ.ENQUEUE_OPTIONS_T;
    message_properties DBMS_AQ.MESSAGE_PROPERTIES_T;
    msg_id RAW(16);
    epoch_millis NUMBER(13,0);
BEGIN
    enqueue_options := DBMS_AQ.ENQUEUE_OPTIONS_T();
    message_properties := dbms_aq.message_properties_t();
    message_properties.correlation := p_ClaimId; -- Correlate events by ClaimId
    
    IF p_operation_type = 'INSERT' THEN
        DBMS_OUTPUT.PUT_LINE('INSERT operation detected');
        -- Calculate epoch time in milliseconds
        SELECT (SYSDATE - TO_DATE('1970-01-01', 'YYYY-MM-DD')) * 86400000 INTO epoch_millis FROM dual;
       
        message := SYS.AQ$_JMS_TEXT_MESSAGE.CONSTRUCT();
        -- Build JSON message for INSERT
        message.set_text('{ "ClaimId": "' || p_ClaimId || '", "StatusUpdateNotes": "' || p_StatusUpdateNotes || '",Timestamp: "' || epoch_millis || '", "Action": "' || p_operation_type ||'" }');
          
        dbms_aq.enqueue(
            queue_name => 'ClaimUpdatesTopic',
            enqueue_options => enqueue_options,
            message_properties => message_properties,
            payload => message,
            msgid => msg_id
        );
        
    ELSIF p_operation_type = 'UPDATE' THEN
        DBMS_OUTPUT.PUT_LINE('UPDATE operation detected');
        DBMS_OUTPUT.PUT_LINE(p_OldStatusUpdateNotes);
        message := SYS.AQ$_JMS_TEXT_MESSAGE.CONSTRUCT();
        -- Build JSON message for UPDATE, including old notes
        message.set_text('{ "ClaimId": "' || p_ClaimId || '", "StatusUpdateNotes": "' || p_StatusUpdateNotes || '",Timestamp: "' || epoch_millis || '", "Action": "' || p_operation_type ||'", "StatusUpdateNotes_Old": "' || p_OldStatusUpdateNotes || '" }');
        
        dbms_aq.enqueue(
            queue_name => 'ClaimUpdatesTopic',
            enqueue_options => enqueue_options,
            message_properties => message_properties,
            payload => message,
            msgid => msg_id
        );
        
    ELSE
        RAISE_APPLICATION_ERROR(-20001, 'Invalid operation type');
    END IF;
END;


-- =============================================================
-- STEP 5: Create trigger to call enqueue handler procedure
-- This trigger fires before INSERT or UPDATE on ClaimUpdatesTable
-- =============================================================

CREATE OR REPLACE TRIGGER ClaimUpdatesTableTrigger
BEFORE INSERT OR UPDATE ON ClaimUpdatesTable
FOR EACH ROW
BEGIN
    IF INSERTING THEN
        ClaimUpdatesTriggerHandler(:NEW.StatusUpdateNotes, :NEW.ClaimId, 'INSERT', NULL);
    ELSIF UPDATING THEN
        ClaimUpdatesTriggerHandler(:NEW.StatusUpdateNotes, :NEW.ClaimId, 'UPDATE', :OLD.StatusUpdateNotes);
    END IF;
END;



-- =============================================================
-- STEP 6: Add default subscriber to the queue
-- =============================================================

BEGIN
    DBMS_AQADM.ADD_SUBSCRIBER(
        queue_name => 'ClaimUpdatesTopic',
        subscriber => SYS.AQ$_AGENT('ClaimUpdatesSubscriber', NULL, NULL)
    );
END;


-- =============================================================
-- STEP 7: PL/SQL dequeue procedure
-- This procedure dequeues messages for a given subscriber
-- =============================================================

CREATE OR REPLACE PROCEDURE ClaimUpdatesTopicDequeue(qname varchar2, subscriber varchar2)
AS
    dequeue_options    dbms_aq.dequeue_options_t;
    message_properties dbms_aq.message_properties_t;
    message_handle     RAW(16);
    message            SYS.AQ$_JMS_TEXT_MESSAGE;
    outtext            clob;
BEGIN
    dequeue_options.consumer_name := subscriber ;
    dequeue_options.navigation    := DBMS_AQ.FIRST_MESSAGE;
    dequeue_options.wait          := 10;
    dequeue_options.delivery_mode := dbms_aq.PERSISTENT;
  
    DBMS_AQ.Dequeue(queue_name         => qname,
                                    dequeue_options    => dequeue_options,
                                    message_properties => message_properties,
                                    payload            => message,
                                    msgid              => message_handle);
    
     message.get_text(outtext);
     DBMS_OUTPUT.PUT_LINE(outtext);
END;
/  -- Compile procedure
SHOW ERRORS


-- =============================================================
-- STEP 8 (Optional): Insert test messages using PL/SQL
-- Use this block for quick testing if not using a Java app
-- =============================================================

declare
    i NUMBER := 0;
    j NUMBER := 0;
    claim_number NUMBER := 0;
    message VARCHAR2(100) := '';
begin	
    for i in 5..10 loop
        j := j+1;
        
        claim_number := 127;
        select ('Create claim entry: '|| j ||' for claim: '|| claim_number) into message from dual;
        dbms_output.put_line('Value to be inserted: ' || message);
        
        INSERT INTO ClaimUpdatesTable(StatusUpdateNotes, ClaimId)
        VALUES
        (
            'Create claim entry: '|| j ||' for claim: '|| claim_number,
            claim_number
        );
        
        if (j=15) then
            j:= 0;
        end if;
    end loop;
    commit;
end;

-- =============================================================
-- STEP 9 (Optional): Execute dequeue procedure in PL/SQL
-- Use this block to dequeue messages in PL/SQL (for demo/testing)
-- =============================================================

begin 
    while true loop
        ClaimUpdatesTopicDequeue('ClaimUpdatesTopic', 'ClaimUpdatesSubscriber');
    end loop;    
end;

-- =============================================================
-- STEP 10: Monitoring and helpful scripts
-- Use these queries to inspect queue status, subscribers, and lag
-- =============================================================
begin 
    while true loop
        ClaimUpdatesTopicDequeue('ClaimUpdatesTopic', 'ClaimUpdatesSubscriber');
    end loop;    
end;


-- =============================================================
-- STEP 10: Monitoring and helpful scripts
-- Use these queries to inspect queue status, subscribers, and lag
-- =============================================================
-- List all queues
select * from user_queues;

-- List all queue subscribers
select * from user_queue_subscribers;

-- List all queue tables
select * from user_queue_tables;

-- List queue shards (replace queue_id with your actual queue ID)
select * from user_queue_shards where queue_id = 75476;

-- Remote dequeue affinity
select * from gv$aq_remote_dequeue_affinity;

-- Count messages per shard
select shard, count(*) from ClaimUpdatesTopic group by shard;

-- See the lag at queue level
column QUEUE_NAME format a12
column consumer_name format a16
set line 200
set pagesize 100
SELECT dbqs.QUEUE_NAME, dbqs.consumer_name,gvsss.enqueued_msgs, gvsss.dequeued_msgs,
       (gvsss.enqueued_msgs - gvsss.dequeued_msgs) "Backlog"
  from (select QUEUE_NAME, CONSUMER_NAME, SUBSCRIBER_ID
      from user_queue_subscribers) dbqs,
       (select NAME, QID from user_queues) dbq,
       (select QUEUE_ID, SUBSCRIBER_ID, sum(ENQUEUED_MSGS) enqueued_msgs,
           sum(DEQUEUED_MSGS) dequeued_msgs
      from gv$aq_sharded_subscriber_stat
      group by QUEUE_ID, SUBSCRIBER_ID) gvsss
  where
    dbqs.QUEUE_NAME = dbq.NAME and dbq.QID = gvsss.QUEUE_ID and
    dbqs.SUBSCRIBER_ID = gvsss.SUBSCRIBER_ID
  ORDER BY
  (gvsss.enqueued_msgs - gvsss.dequeued_msgs) desc;

-- See the lag at the shard level
select INST_ID, QUEUE_ID, SUBSCRIBER_ID, SHARD_ID, PRIORITY, ENQUEUED_MSGS, DEQUEUED_MSGS from gv$aq_sharded_subscriber_stat;

