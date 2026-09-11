<img width="1733" height="908" alt="kkobak-social-preview" src="https://github.com/user-attachments/assets/e65d2fe8-32c7-4947-8893-381a6020545b" />

# 꼬박꼬박 · KKOBAK

정수기 필터 교체, 칫솔 교체, 청소와 세탁처럼 잊기 쉬운 생활 주기를 기록하고 알림 받는 웹앱입니다.

## 기술 구성

- Frontend: React 19 + Vite 8 + TypeScript
- Backend: Spring Boot 4 + Gradle + Java 21
- Database: Supabase PostgreSQL + Flyway
- Media: Supabase Storage
- Push: Web Push + VAPID
- Deploy: Vercel + Render, GitHub 연동 자동 배포

## 프로젝트 구조

```text
frontend/              React + Vite 웹앱과 PWA 서비스 워커
backend/               Spring Boot API, 데이터 저장, 이미지 업로드, 푸시 발송
supabase/storage.sql   공개 이미지 버킷 설정
.github/workflows/     프론트·백엔드 검증과 매일 알림 호출
render.yaml            Render Blueprint
vercel.json            Vercel 모노레포 빌드 설정
```

## 로컬 실행

백엔드:

```powershell
cd backend
$env:APP_ADMIN_PASSWORD="local-admin-password"
.\gradlew.bat bootRun
```

프론트엔드:

```powershell
cd frontend
Copy-Item .env.example .env.local
npm install
npm run dev
```

백엔드는 별도 설정이 없으면 H2와 로컬 이미지 저장소를 사용합니다. 일반 계정은 가입 후 관리자 승인이 필요하며, 승인된 동일 계정으로 로그인하면 여러 기기에서 같은 주기를 사용합니다. 프론트엔드는 최근 서버 데이터를 계정별 기기 캐시에 보관하지만 생성·수정·삭제는 서버 연결 상태에서만 확정합니다.

전체 배포 절차와 필요한 환경 변수는 [DEPLOYMENT.md](./DEPLOYMENT.md)에 정리되어 있습니다.

## 공개 포트폴리오

공개용 소스는 [kkobak-portfolio](https://github.com/JUNHONGKIM95/kkobak-portfolio)에 별도 이력으로 동기화됩니다. 실제 서비스의 비밀 환경 변수와 비공개 저장소의 과거 이력은 공개 저장소로 전달되지 않습니다.
