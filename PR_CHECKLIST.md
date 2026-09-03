# Travel Service PR Checklist

## 검증 결과

- [x] Gradle `clean test bootJar` 성공
- [x] GitHub Packages `travel-common` 다운로드 확인
- [x] Dockerfile 표준 적용 (Temurin 21 Alpine, UID/GID 10001)
- [x] `.dockerignore`에 소스·환경파일 제외 설정
- [x] Docker 이미지 빌드 및 컨테이너 실행 확인
- [x] PostgreSQL·Redis 연결 확인
- [x] `/actuator/health` 및 `/actuator/health/readiness` HTTP 200 확인
- [x] Bearer 인증 기능 API 호출 확인
- [x] Request ID 전달 및 의존성 장애 503 처리 테스트 확인
- [x] Kubernetes Pod `1/1 Running` 확인
- [x] Kubernetes Service DNS 통신 확인
- [x] Kyverno Admission 복구 및 동작 확인
- [x] GitHub Actions Secret `COMMON_PACKAGES_USER` 등록 확인
- [x] GitHub Actions Secret `COMMON_PACKAGES_TOKEN` 등록 확인

## 변경 범위

- Docker 실행 표준화
- 개발 환경 로그 경로 표준화
- 컨테이너 검증 및 배포 설정 보완

## 확인 사항

- [x] 민감한 값이 커밋에 포함되지 않음
- [x] 운영 클러스터에는 배포하지 않음
- [ ] 리뷰어 승인
