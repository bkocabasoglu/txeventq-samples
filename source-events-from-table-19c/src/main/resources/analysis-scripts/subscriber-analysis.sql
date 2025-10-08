SELECT dbqs.QUEUE_NAME,
       dbqs.consumer_name,
       gvsss.enqueued_msgs,
       gvsss.dequeued_msgs,
       (gvsss.enqueued_msgs - gvsss.dequeued_msgs) "Backlog"
from (select QUEUE_NAME, CONSUMER_NAME, SUBSCRIBER_ID
      from user_queue_subscribers) dbqs,
     (select NAME, QID from user_queues) dbq,
     (select QUEUE_ID,
             SUBSCRIBER_ID,
             sum(ENQUEUED_MSGS) enqueued_msgs,
             sum(DEQUEUED_MSGS) dequeued_msgs
      from gv$aq_sharded_subscriber_stat
      group by QUEUE_ID, SUBSCRIBER_ID) gvsss
where dbqs.QUEUE_NAME = dbq.NAME
  and dbq.QID = gvsss.QUEUE_ID
  and dbqs.SUBSCRIBER_ID = gvsss.SUBSCRIBER_ID
ORDER BY (gvsss.enqueued_msgs - gvsss.dequeued_msgs) desc;