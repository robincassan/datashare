import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '5s', target: 5 },
    { duration: '10s', target: 10 },
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://172.23.32.1:8080';
const FILE_SIZE = 100 * 1024;
const JSON_HEADERS = { 'Content-Type': 'application/json' };

export default function () {
  const uniqueId = `${__VU}-${__ITER}`;
  const email = `perf-${uniqueId}@test.com`;
  const password = 'Test1234!';

  // 1. Inscription
  const registerRes = http.post(
    `${BASE_URL}/api/auth/register`,
    JSON.stringify({ email, password }),
    { headers: JSON_HEADERS }
  );
  check(registerRes, { 'register 201': (r) => r.status === 201 || r.status === 409 });

  // 2. Connexion
  const loginRes = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email, password }),
    { headers: JSON_HEADERS }
  );
  check(loginRes, { 'login 200': (r) => r.status === 200 });
  let token = '';
  if (loginRes.status === 200) {
    token = loginRes.json('token');
  }

  // 3. Upload fichier
  const fileBody = new ArrayBuffer(FILE_SIZE);
  const uploadRes = http.post(
    `${BASE_URL}/api/files/upload`,
    {
      file: http.file(fileBody, `test-${uniqueId}.bin`, 'application/octet-stream'),
      password: 'filepass',
    },
    token ? { headers: { Authorization: `Bearer ${token}` } } : {}
  );
  check(uploadRes, { 'upload 201': (r) => r.status === 201 });

  sleep(1);
}
