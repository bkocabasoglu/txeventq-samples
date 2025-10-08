--STEP: 1 ------------------------------------------------------
-- Create table for action events to be inserted or updated
--------------------------------------------------------


CREATE TABLE ActionUpdatesTable
(
    Id            RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
    ActionId VARCHAR(100),
    Notes         VARCHAR(500),
    Timestamp     NUMBER(13, 0)
);


--STEP: 2 ------------------------------------------------------
-- Create TxEventQ in 19c
--------------------------------------------------------
BEGIN
    DBMS_AQADM.CREATE_SHARDED_QUEUE(
            queue_name => 'ActionUpdatesTopic',
            multiple_consumers => TRUE
    );

    DBMS_AQADM.set_queue_parameter('ActionUpdatesTopic', 'SHARD_NUM', 50);

    -- Start the queue
    DBMS_AQADM.start_queue(queue_name=>'ActionUpdatesTopic');
END;

--STEP: 3 ------------------------------------------------------
-- Create/Update trigger handler
--------------------------------------------------------
CREATE OR REPLACE PROCEDURE ActionUpdatesTriggerHandler(
    p_Notes IN VARCHAR2,
    p_ActionId IN VARCHAR2,
    p_OperationType IN VARCHAR2,
    p_OldNotes IN VARCHAR2
)
AS
    message            SYS.AQ$_JMS_TEXT_MESSAGE;
    enqueue_options    DBMS_AQ.ENQUEUE_OPTIONS_T;
    message_properties DBMS_AQ.MESSAGE_PROPERTIES_T;
    msg_id             RAW(16);
    epoch_millis       NUMBER(13, 0);
BEGIN
    enqueue_options := DBMS_AQ.ENQUEUE_OPTIONS_T();
    message_properties := DBMS_AQ.MESSAGE_PROPERTIES_T();

    -- Calculate timestamp once for both operations
    SELECT (SYSDATE - TO_DATE('1970-01-01', 'YYYY-MM-DD')) * 86400000 INTO epoch_millis FROM dual;

    IF p_OperationType = 'INSERT' THEN
        DBMS_OUTPUT.PUT_LINE('INSERT operation detected');

        message := SYS.AQ$_JMS_TEXT_MESSAGE.CONSTRUCT();
        message.set_text('{ "ActionId": "' || p_ActionId || '", "Notes": "' || p_Notes ||
                         '","Timestamp": "' || epoch_millis || '", "Action": "' || p_OperationType || '" }');

        DBMS_AQ.enqueue(
                queue_name => 'ActionUpdatesTopic',
                enqueue_options => enqueue_options,
                message_properties => message_properties,
                payload => message,
                msgid => msg_id
        );


    ELSIF p_OperationType = 'UPDATE' THEN
        DBMS_OUTPUT.PUT_LINE('UPDATE operation detected');

        DBMS_OUTPUT.PUT_LINE(p_OldNotes);
        message := SYS.AQ$_JMS_TEXT_MESSAGE.CONSTRUCT();
        message.set_text('{ "ActionId": "' || p_ActionId || '", "Notes": "' || p_Notes ||
                         '","Timestamp": "' || epoch_millis || '", "Action": "' || p_OperationType ||
                         '", "Notes_Old": "' || p_OldNotes || '" }');


        DBMS_AQ.enqueue(
                queue_name => 'ActionUpdatesTopic',
                enqueue_options => enqueue_options,
                message_properties => message_properties,
                payload => message,
                msgid => msg_id
        );

    ELSE
        RAISE_APPLICATION_ERROR(-20001, 'Invalid operation type');
    END IF;
END;

--STEP: 4 ------------------------------------------------------
-- Create/Update trigger to call enqueue handler procedure
--------------------------------------------------------
CREATE OR REPLACE TRIGGER ActionUpdatesTableTrigger
    BEFORE INSERT OR UPDATE
    ON ActionUpdatesTable
    FOR EACH ROW
BEGIN
    IF INSERTING THEN
        ActionUpdatesTriggerHandler(:NEW.Notes, :NEW.ActionId, 'INSERT', NULL);
    ELSIF UPDATING THEN
        ActionUpdatesTriggerHandler(:NEW.Notes, :NEW.ActionId, 'UPDATE', :OLD.Notes);
    END IF;
END;


--STEP: 5 ------------------------------------------------------
-- Create subscriber
--------------------------------------------------------
BEGIN
    DBMS_AQADM.ADD_SUBSCRIBER(
            queue_name => 'ActionUpdatesTopic',
            subscriber => SYS.AQ$_AGENT('ActionUpdatesSubscriber1', NULL, NULL)
    );
END;

--STEP: 6 ------------------------------------------------------
-- Insert messages PL/SQL for testing
--------------------------------------------------------
-- declare
--     i               NUMBER        := 0;
--     j               NUMBER        := 0;
--     action_number NUMBER        := 0;
--     message         VARCHAR2(100) := '';
-- begin
--
--
--     for i in 1..10000
--         loop
--             j := j + 1;
--
--             action_number := 127;
--             select ('Create action entry: ' || j || ' for action: ' || action_number) into message from dual;
-- --dbms_output.put_line('Value to be inserted: ' || message);
--
--             INSERT INTO ActionUpdatesTable(Notes, ActionId)
--             VALUES ('Create action entry: ' || j || ' for action: ' || action_number,
--                     action_number);
--
--         end loop;
--     commit;
-- end;

-- select *
-- from ActionUpdatesTopic;
--
-- select shard, count(*)
-- from ActionUpdatesTopic
-- group by shard;
--
-- select *
-- from ActionUpdatesTable;