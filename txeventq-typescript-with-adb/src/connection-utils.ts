export function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

export function getErrorCode(error: unknown): string | undefined {
  if (!error || typeof error !== 'object') {
    return undefined;
  }

  const code = (error as { code?: unknown }).code;
  return typeof code === 'string' ? code : undefined;
}

export function getErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }
  if (typeof error === 'string') {
    return error;
  }
  if (!error) {
    return 'Unknown error';
  }
  return String(error);
}

export function isQueueEmptyError(error: unknown): boolean {
  if (!error || typeof error !== 'object') {
    return false;
  }
  return (error as { errorNum?: unknown }).errorNum === 25228;
}

export function isRecoverableConnectionError(error: unknown): boolean {
  const code = getErrorCode(error);
  const message = getErrorMessage(error).toLowerCase();

  const recoverableCodes = new Set([
    'DPI-1010', // not connected
    'NJS-500',  // connection closed
    'NJS-503',  // connection to host failed / lost
    'NJS-511'   // connection was closed
  ]);

  if (code && recoverableCodes.has(code)) {
    return true;
  }

  if (!error || typeof error !== 'object') {
    return false;
  }

  const errorNum = (error as { errorNum?: unknown }).errorNum;
  const recoverableOracleErrorNumbers = new Set([
    3113,  // ORA-03113: end-of-file on communication channel
    3114,  // ORA-03114: not connected to ORACLE
    3135,  // ORA-03135: connection lost contact
    12170, // ORA-12170: connect timeout
    12514, // ORA-12514: listener does not currently know service
    12537, // ORA-12537: connection closed
    12541, // ORA-12541: no listener
    12547  // ORA-12547: TNS lost contact
  ]);

  if (typeof errorNum === 'number' && recoverableOracleErrorNumbers.has(errorNum)) {
    return true;
  }

  return (
    message.includes('not connected') ||
    message.includes('connection was closed') ||
    message.includes('connection is closed') ||
    message.includes('connection lost') ||
    message.includes('socket') ||
    message.includes('econnreset') ||
    message.includes('econnrefused')
  );
}
