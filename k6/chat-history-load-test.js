import http from "k6/http";
import { check, sleep } from "k6";

// JWT 토큰 여기에 넣기
const JWT_TOKEN =
  "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0LXVzZXItMSIsImlhdCI6MTc4NDA3NzQ0NCwiZXhwIjoxNzg0MDgxMDQ0fQ.SDTFwo7Bkm839oUhqHOCeWOicRjaif0voX6B0EAEvrU";
const ROOM_ID = 3;
const BASE_URL = "http://localhost:8080";

export const options = {
  stages: [
    { duration: "10s", target: 10 }, // 10초 동안 10명까지 증가
    { duration: "30s", target: 50 }, // 30초 동안 50명 유지
    { duration: "10s", target: 100 }, // 10초 동안 100명까지 증가
    { duration: "30s", target: 100 }, // 30초 동안 100명 유지
    { duration: "10s", target: 0 }, // 10초 동안 0명으로 감소
  ],
  thresholds: {
    http_req_duration: ["p(95)<2000"], // 95%가 2초 이내
    http_req_failed: ["rate<0.05"], // 에러율 5% 미만
  },
};

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/rooms/${ROOM_ID}/chat?size=50`, {
    headers: {
      Authorization: `Bearer ${JWT_TOKEN}`,
      "Content-Type": "application/json",
    },
  });

  check(res, {
    "status is 200": (r) => r.status === 200,
    "response time < 2s": (r) => r.timings.duration < 2000,
  });

  sleep(1);
}
