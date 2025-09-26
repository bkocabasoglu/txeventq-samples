-- =====================================================
-- Oracle TxEventQ POC - User Creation Script
-- =====================================================

-- Connect to the PDB
ALTER SESSION SET CONTAINER = FREEPDB1;

-- Create TxEventQ User
CREATE USER txeventq_user IDENTIFIED BY pass123;

-- Basic login permission
GRANT CREATE SESSION TO txeventq_user;

-- Required roles for TxEventQ (Advanced Queuing)
GRANT AQ_ADMINISTRATOR_ROLE TO txeventq_user;
GRANT CONNECT, RESOURCE TO txeventq_user;

-- Enable usage of Oracle Streams AQ (used by TxEventQ)
GRANT EXECUTE ON DBMS_AQ TO txeventq_user;
GRANT EXECUTE ON DBMS_AQADM TO txeventq_user;
GRANT ENQUEUE ANY QUEUE TO txeventq_user;
GRANT DEQUEUE ANY QUEUE TO txeventq_user;

-- Access to performance views (optional, for diagnostics)
GRANT SELECT_CATALOG_ROLE TO txeventq_user;

-- Give quota on tablespace
ALTER USER txeventq_user QUOTA UNLIMITED ON USERS;

-- Display success message
SELECT 'txeventq_user created successfully!' as status FROM dual;

-- Show created user
SELECT username, account_status, created 
FROM dba_users 
WHERE username = 'TXEVENTQ_USER';
