# 꼬박꼬박 배포 설정

`main` 브랜치가 Vercel과 Render에 연결되면 이후 GitHub 푸시마다 프론트엔드와 백엔드가 자동 배포됩니다.

## 1. Supabase

1. Supabase 프로젝트를 만듭니다.
2. SQL Editor에서 `supabase/storage.sql`을 실행합니다.
3. Database의 Transaction pooler 연결 정보를 확인합니다.
4. Project URL과 backend-only service role key를 보관합니다.

애플리케이션 테이블은 첫 Render 배포 때 Flyway가 자동 생성합니다.

## 2. Render

GitHub 저장소의 `render.yaml`을 Blueprint로 가져온 뒤 다음 비밀 값을 입력합니다.

```text
SUPABASE_DB_URL
SUPABASE_DB_USERNAME
SUPABASE_DB_PASSWORD
SUPABASE_URL
SUPABASE_SERVICE_ROLE_KEY
APP_CORS_ALLOWED_ORIGINS
APP_FRONTEND_URL
APP_PUSH_VAPID_PUBLIC_KEY
APP_PUSH_VAPID_PRIVATE_KEY
APP_PUSH_VAPID_SUBJECT
```

`APP_NOTIFICATION_DISPATCH_SECRET`은 Blueprint가 자동 생성하며 수동 알림 테스트 API를 보호합니다.

첫 배포 전에 관리자 계정의 비밀번호를 반드시 Render 환경 변수로 설정합니다. 서버가 다시 시작되면 같은 관리자 아이디의 비밀번호가 환경 변수 값으로 갱신됩니다.

```text
APP_ADMIN_USERNAME=admin
APP_ADMIN_PASSWORD=<충분히 긴 새 비밀번호>
```

## 3. Vercel

GitHub 저장소를 Import합니다. 루트 디렉터리는 저장소 루트로 두고, 다음 환경 변수를 추가합니다.

```text
VITE_API_BASE_URL=https://<render-service>.onrender.com
```

생성된 Vercel 주소를 Render의 다음 값에 반영합니다.

```text
APP_CORS_ALLOWED_ORIGINS=https://<vercel-domain>.vercel.app
APP_FRONTEND_URL=https://<vercel-domain>.vercel.app
```

## 4. 매일 알림

`Daily cycle reminders` 워크플로가 매일 한국시간 오전 9시에 Render API를 깨우고 알림을 발송합니다. 기본 운영 주소 `https://kkobak-api.onrender.com`은 워크플로에 설정되어 있으므로 별도 Secret이 필요하지 않습니다. 백엔드 주소를 바꿀 때만 GitHub 저장소 Settings → Secrets and variables → Actions → Variables에 `KKOBAK_BACKEND_URL`을 등록하세요.

예약 발송 API는 한국시간 오전 9시에만 동작하며, 같은 구독에는 하루 한 번만 발송합니다. Web Push는 HTTPS 환경과 사용자 브라우저의 알림 권한이 필요합니다.

## 5. 계정 승인과 기기 간 동기화

1. 일반 사용자는 로그인 화면의 **계정 만들기**에서 가입을 요청합니다.
2. 관리자가 `admin` 계정으로 로그인하면 관리자 페이지가 열립니다.
3. **승인 대기** 사용자의 **승인** 버튼을 누릅니다.
4. 승인된 사용자는 같은 아이디로 스마트폰과 랩탑에 로그인합니다.

각 브라우저에 예전 방식으로 저장되어 있던 주기는 해당 기기에서 처음 로그인할 때 계정으로 자동 귀속됩니다. 따라서 기존 데이터가 있는 스마트폰과 랩탑에서 각각 한 번씩 같은 일반 사용자 계정으로 로그인하면 두 기기의 기존 주기가 하나의 계정에 합쳐집니다.
