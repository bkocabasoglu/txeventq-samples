-- =====================================================
-- Oracle TxEventQ POC - Topic Drop Script
-- Reverses actions performed by 02-create-topics.sql
-- =====================================================

-- Stop the queue if running, remove subscribers, and drop the transactional event queue
BEGIN
  -- Attempt to stop the queue (ignore errors if already stopped)
  BEGIN
    DBMS_AQADM.stop_queue(queue_name => 'EVENT_TOPIC');
  EXCEPTION
    WHEN OTHERS THEN
      NULL; -- proceed even if stopping fails or queue already stopped
  END;

  -- Remove the subscriber added in the create script
  BEGIN
    DBMS_AQADM.REMOVE_SUBSCRIBER(
      queue_name => 'EVENT_TOPIC',
      subscriber => SYS.AQ$_AGENT('event_subscriber', NULL, NULL)
    );
  EXCEPTION
    WHEN OTHERS THEN
      NULL; -- ignore if subscriber does not exist
  END;

  -- Drop the transactional event queue
  BEGIN
    DBMS_AQADM.DROP_TRANSACTIONAL_EVENT_QUEUE(
      queue_name => 'EVENT_TOPIC',
      force => TRUE
    );
  EXCEPTION
    WHEN OTHERS THEN
      NULL; -- ignore if queue does not exist
  END;
END;
/

-- Display success message
SELECT 'TxEventQ topic EVENT_TOPIC dropped (if it existed).' as status FROM dual;
