import CryptoJS from 'crypto-js';

const STORAGE_KEY = 'PKHX8dfuePB2PpT4';
const STORAGE_IV = '7JBzAcDRQc9HCahm';
const TRANSPORT_KEY = 'Cdg9VObOpE3yEQzz';
const TRANSPORT_IV = 'frYwJYzoqXvv5ePy';

const ENC_PREFIX = 'ENC:';

const sKey = CryptoJS.enc.Utf8.parse(STORAGE_KEY);
const sIv = CryptoJS.enc.Utf8.parse(STORAGE_IV);
const tKey = CryptoJS.enc.Utf8.parse(TRANSPORT_KEY);
const tIv = CryptoJS.enc.Utf8.parse(TRANSPORT_IV);

const cbcOptions = (key: CryptoJS.lib.WordArray, iv: CryptoJS.lib.WordArray) => ({
  iv,
  mode: CryptoJS.mode.CBC,
  padding: CryptoJS.pad.Pkcs7,
});

export function encryptTransport(plaintext: string): string {
  if (!plaintext) return plaintext;
  return CryptoJS.AES.encrypt(plaintext, tKey, cbcOptions(tKey, tIv)).toString();
}

export function decryptTransport(ciphertext: string): string {
  if (!ciphertext) return ciphertext;
  try {
    const bytes = CryptoJS.AES.decrypt(ciphertext, tKey, cbcOptions(tKey, tIv));
    return bytes.toString(CryptoJS.enc.Utf8) || ciphertext;
  } catch {
    return ciphertext;
  }
}

export function decryptStorage(ciphertext: string): string {
  if (!ciphertext) return ciphertext;
  if (!ciphertext.startsWith(ENC_PREFIX)) return ciphertext;
  try {
    const base64Part = ciphertext.substring(ENC_PREFIX.length);
    const bytes = CryptoJS.AES.decrypt(base64Part, sKey, cbcOptions(sKey, sIv));
    return bytes.toString(CryptoJS.enc.Utf8) || ciphertext;
  } catch {
    return ciphertext;
  }
}

/**
 * 双层解密：先解传输层，再解存储层
 */
export function decryptMessage(doubleCipher: string): string {
  if (!doubleCipher) return doubleCipher;
  const afterTransport = decryptTransport(doubleCipher);
  return decryptStorage(afterTransport);
}

/**
 * 发送消息时的传输加密（语义别名）
 */
export const encryptForSend = encryptTransport;
